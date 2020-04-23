package me.ykalemi.jenkinstoslack;

import com.offbytwo.jenkins.model.FolderJob;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
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

    private static class FolderSetting {

        /**
         * Possible values:
         * - OPMS[OPMS,OPMS-migration_test,MV,MV-tests]
         * - Chat
         */
        static FolderSetting parse(String rootFoldersSettings) {
            String rootFolder;
            String[] subFolders = new String[0];
            if (rootFoldersSettings.contains("[")) {
                String[] split = StringUtils.split(rootFoldersSettings, "[");
                rootFolder = split[0];
                if (split.length > 1 && split[1].endsWith("]")) {
                    String subFoldersString = split[1].substring(0, split[1].length() - 1);
                    subFolders = StringUtils.split(subFoldersString, ",");
                }
            } else {
                rootFolder = rootFoldersSettings;
            }

            return new FolderSetting(rootFolder, subFolders);
        }

        private final String rootFolder;
        private final String[] subFolders;

        private FolderSetting(String rootFolder, String[] subFolders) {
            this.rootFolder = rootFolder;
            this.subFolders = subFolders;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(PmsJobsService.class);

    private final JenkinsService jenkinsService;
    private final List<FolderSetting> rootFoldersSettings;

    @Autowired
    public PmsJobsService(JenkinsService jenkinsService, @Value("${jenkins.pms.folders}") String rootFoldersSettings) {
        this.jenkinsService = jenkinsService;
        this.rootFoldersSettings = Arrays.stream(rootFoldersSettings.split(";"))
            .map(FolderSetting::parse)
            .collect(Collectors.toList());
    }

    public List<String> getUnsuccessfulJobs() throws IOException {
        return rootFoldersSettings.stream()
            .map(this::getUnsuccessfulJobsSafe)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    private List<String> getUnsuccessfulJobsSafe(FolderSetting folderSetting) {
        try {
            return getUnsuccessfulJobs(folderSetting);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private List<String> getUnsuccessfulJobs(FolderSetting folderSetting) throws IOException {
        Optional<FolderJob> rootJobOpt = jenkinsService.getRootFolder(folderSetting.rootFolder);

        if (rootJobOpt.isEmpty()) {
            String msg = String.format("Root folder '%s' doesn't exist. Fix the config", folderSetting.rootFolder);
            LOG.error(msg);
            return Collections.singletonList(msg);
        }

        FolderJob rootJob = rootJobOpt.get();

        if (folderSetting.subFolders.length > 0) {
            List<String> unsuccessfulJobs = new ArrayList<>();
            for (String subFolder : folderSetting.subFolders) {
                Optional<FolderJob> folderOpt = jenkinsService.getFolderJob(rootJob, subFolder);
                List<String> result = folderOpt.map(this::getUnsuccessfulPmsJobs).orElse(Collections.singletonList(String.format("Folder '%s' doesn't exist. Fix the config", subFolder)));
                unsuccessfulJobs.addAll(result);
            }
            return unsuccessfulJobs;
        } else {
            return getUnsuccessfulPmsJobs(rootJob);
        }
    }

    private List<String> getUnsuccessfulPmsJobs(FolderJob folderJob) {
        try {
            return jenkinsService.getUnsuccessfulJobs(folderJob, folderJob.getJobs().values());
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
