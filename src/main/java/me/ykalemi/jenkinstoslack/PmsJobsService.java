package me.ykalemi.jenkinstoslack;

import com.offbytwo.jenkins.model.FolderJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author ykalemi
 * @since 08.02.19
 */
@Service
public class PmsJobsService {

    private static final Logger LOG = LoggerFactory.getLogger(PmsJobsService.class);

    private final JenkinsService jenkinsService;
    private final String rootFolder;
    private final String[] subFolders;

    @Autowired
    public PmsJobsService(JenkinsService jenkinsService, @Value("${jenkins.pms.folder}") String rootFolder,
                          @Value("${jenkins.pms.jobs}") String subFolders) {
        this.jenkinsService = jenkinsService;
        this.rootFolder = rootFolder;
        this.subFolders = subFolders.split(",");
    }

    public List<String> getUnsuccessfulJobs() throws IOException {
        List<String> unsuccessfulJobs = new ArrayList<>();
        Optional<FolderJob> rootJobOpt = jenkinsService.getRootFolder(rootFolder);

        if (rootJobOpt.isEmpty()) {
            String msg = String.format("Root folder '%s' doesn't exist. Fix the config", rootFolder);
            LOG.error(msg);
            return Collections.singletonList(msg);
        }

        FolderJob rootJob = rootJobOpt.get();

        for (String subFolder : subFolders) {
            Optional<FolderJob> folderOpt = jenkinsService.getFolderJob(rootJob, subFolder);
            if (folderOpt.isEmpty()) {
                String msg = String.format("Folder '%s' doesn't exist. Fix the config", subFolder);
                LOG.error(msg);
                unsuccessfulJobs.add(msg);
            } else {
                unsuccessfulJobs.addAll(getUnsuccessfulPmsJobs(folderOpt.get()));
            }
        }

        return unsuccessfulJobs;
    }

    private List<String> getUnsuccessfulPmsJobs(FolderJob folderJob) throws IOException {
        return jenkinsService.getUnsuccessfulJobs(folderJob, folderJob.getJobs().values());
    }
}
