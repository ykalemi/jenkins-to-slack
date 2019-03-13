package me.ykalemi.jenkinstoslack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

@SpringBootApplication
public class JenkinsToSlackApplication {

    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext context = SpringApplication.run(JenkinsToSlackApplication.class, args);
        var messagingService = context.getBean(BuildsMessagingService.class);
        messagingService.sendBuildsStatusMessages();
    }
}

