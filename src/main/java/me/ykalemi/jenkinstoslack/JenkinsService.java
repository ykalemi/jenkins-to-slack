package me.ykalemi.jenkinstoslack;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.FolderJob;
import com.offbytwo.jenkins.model.Job;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author ykalemi
 * @since 08.02.19
 */
@Service
public class JenkinsService {

    private static final UnsuccessfulBuildReport EMPTY_BUILD_REPORT = new UnsuccessfulBuildReport();
    private final String jenkinsUri;
    private final String username;
    private final String password;

    private JenkinsServer jenkins;

    @Autowired
    public JenkinsService(@Value("${jenkins.uri}") String jenkinsUri, @Value("${jenkins.username}") String username,
                          @Value("${jenkins.password}") String password) {
        this.jenkinsUri = jenkinsUri;
        this.username = username;
        this.password = password;
    }

    @PostConstruct
    public void init() throws URISyntaxException {
        jenkins = new JenkinsServer(new URI(jenkinsUri), username, password);
    }

    Optional<FolderJob> getRootFolder(String jobName) throws IOException {
        JobWithDetails opmsJob = jenkins.getJob(jobName);
        if (opmsJob == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(jenkins.getFolderJob(opmsJob).orNull());
    }

    Optional<FolderJob> getFolderJob(FolderJob parentFolder, String folderName) throws IOException {
        Job mv = parentFolder.getJob(folderName);
        if (mv == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(jenkins.getFolderJob(mv).orNull());
    }

    List<String> getUnsuccessfulJobs(FolderJob folder, Collection<Job> jobs) throws IOException {
        List<String> unsuccessfulJobs = new ArrayList<>();
        for (Job job : jobs) {
            JobWithDetails details = job.details();

            UnsuccessfulBuildReport report = createReport(details);

            if (report.unsuccessful) {
                String msg = String.format("%s/%s build %d: %s (<%s|link>)",
                        folder.getDisplayName(), job.getName(), report.lastBuildNumber, report.lastBuildResult, report.lastBuildUrl);
                if (report.lastSuccessBuildNumber > 0) {
                    msg += String.format(". Последняя успешная сборка: %d", report.lastSuccessBuildNumber);
                }
                unsuccessfulJobs.add(msg);
            }
        }

        return unsuccessfulJobs;
    }

    private UnsuccessfulBuildReport createReport(JobWithDetails details) throws IOException {

        Build lastBuild = details.getLastCompletedBuild();

        BuildResult result = lastBuild.details().getResult();

        if (Build.BUILD_HAS_NEVER_RUN.equals(lastBuild)) {
            return emptyReport();
        }

        if (result == BuildResult.SUCCESS) {
            return emptyReport();
        }

        Build lastSuccessful = details.getLastStableBuild();
        if (lastSuccessful == null) {
            return new UnsuccessfulBuildReport(true, lastBuild.getNumber(), result, lastBuild.getUrl(), 0);
        }

        if (lastBuild.getNumber() - lastSuccessful.getNumber() == 1) {
            return emptyReport();
        }

        return new UnsuccessfulBuildReport(true, lastBuild.getNumber(), result, lastBuild.getUrl(), lastSuccessful.getNumber());
    }

    private UnsuccessfulBuildReport emptyReport() {
        return EMPTY_BUILD_REPORT;
    }

    private static class UnsuccessfulBuildReport {

        boolean unsuccessful;
        int lastBuildNumber;
        BuildResult lastBuildResult;
        String lastBuildUrl;
        int lastSuccessBuildNumber;

        private UnsuccessfulBuildReport() {}

        private UnsuccessfulBuildReport(boolean unsuccessful, int lastBuildNumber, BuildResult lastBuildResult, String lastBuildUrl, int lastSuccessBuildNumber) {
            this.unsuccessful = unsuccessful;
            this.lastBuildNumber = lastBuildNumber;
            this.lastBuildResult = lastBuildResult;
            this.lastBuildUrl = lastBuildUrl;
            this.lastSuccessBuildNumber = lastSuccessBuildNumber;
        }
    }
}
