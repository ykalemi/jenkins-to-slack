package me.ykalemi.jenkinstoslack;

import com.offbytwo.jenkins.model.FolderJob;
import com.offbytwo.jenkins.model.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ykalemi
 * @since 08.02.19
 */
@Service
public class PmsJobsService {

    private final String pmsFolder;
    private final JenkinsService jenkinsService;
    private final String[] pmsJobs;
    private final String[] mvJobs;

    @Autowired
    public PmsJobsService(JenkinsService jenkinsService, @Value("${jenkins.pms.folder}") String pmsFolder,
                          @Value("${jenkins.pms.jobs}") String pmsJobs, @Value("${jenkins.pms.jobs}") String mvJobs) {
        this.jenkinsService = jenkinsService;
        this.pmsFolder = pmsFolder;
        this.pmsJobs = pmsJobs.split(",");
        this.mvJobs = mvJobs.split(",");
    }

    public List<String> getUnsuccessfulJobs() throws IOException {
        FolderJob opmsFolder = jenkinsService.getRootFolder(pmsFolder);

        List<String> unsuccessfulJobs = new ArrayList<>();

        for (String mvJobFolder : mvJobs) {
            unsuccessfulJobs.addAll(jenkinsService.getUnsuccessfulJobs(opmsFolder, mvJobFolder));
        }
        unsuccessfulJobs.addAll(getUnsuccessfulPmsJobs(opmsFolder));

        return unsuccessfulJobs;
    }

    private List<String> getUnsuccessfulPmsJobs(FolderJob opmsFolder) throws IOException {
        List<Job> jobs = Arrays.stream(pmsJobs).map(opmsFolder::getJob).collect(Collectors.toList());
        return jenkinsService.getUnsuccessfulJobs(opmsFolder, jobs);
    }
}
