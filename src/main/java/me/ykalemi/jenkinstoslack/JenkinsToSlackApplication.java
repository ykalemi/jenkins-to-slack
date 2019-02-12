package me.ykalemi.jenkinstoslack;

import org.apache.commons.lang.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.List;

@SpringBootApplication
public class JenkinsToSlackApplication {

    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext context = SpringApplication.run(JenkinsToSlackApplication.class, args);

        PmsJobsService pmsJobs = context.getBean(PmsJobsService.class);
        SlackService slackService = context.getBean(SlackService.class);

        List<String> unsuccessfulJobs = pmsJobs.getUnsuccessfulJobs();
        if (!unsuccessfulJobs.isEmpty()) {
            slackService.send(StringUtils.join(unsuccessfulJobs, "\n"));
        }
    }
}

