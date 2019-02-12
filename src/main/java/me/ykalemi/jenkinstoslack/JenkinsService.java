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

/**
 * @author ykalemi
 * @since 08.02.19
 */
@Service
public class JenkinsService {

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

    FolderJob getRootFolder(String jobName) throws IOException {
        JobWithDetails opmsJob = jenkins.getJob(jobName);
        return jenkins.getFolderJob(opmsJob).get();
    }

    List<String> getUnsuccessfulJobs(FolderJob parentFolder, String folderName) throws IOException {

        Job mv = parentFolder.getJob(folderName);
        FolderJob folder = jenkins.getFolderJob(mv).get();

        Collection<Job> jobs = folder.getJobs().values();

        return getUnsuccessfulJobs(folder, jobs);
    }

    List<String> getUnsuccessfulJobs(FolderJob folder, Collection<Job> jobs) throws IOException {
        List<String> unsuccessfulJobs = new ArrayList<>();
        for (Job job : jobs) {
            JobWithDetails details = job.details();
            Build lastBuild = details.getLastCompletedBuild();
            if (Build.BUILD_HAS_NEVER_RUN.equals(lastBuild)) {
                continue;
            }

            BuildResult result = lastBuild.details().getResult();
            if (result != BuildResult.SUCCESS) {
                String msg = String.format("%s/%s build %d: %s (<%s|link>)",
                        folder.getDisplayName(), job.getName(), lastBuild.getNumber(), result, lastBuild.getUrl());
                unsuccessfulJobs.add(msg);
            }
        }

        return unsuccessfulJobs;
    }
}
