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
        MotivationQuoteService quoteService = context.getBean(MotivationQuoteService.class);

        List<String> unsuccessfulJobs = pmsJobs.getUnsuccessfulJobs();
        if (!unsuccessfulJobs.isEmpty()) {
            slackService.send("Не было более одной успешной сборки подряд:\n" + StringUtils.join(unsuccessfulJobs, "\n"));
        } else {

            String quote = null;
            try {
                quote = quoteService.getMotivatingQuote();
            } catch (IOException e) {
                // skip
            }
            String message = "Вы молодцы, ваши сборки стабильны";
            if (quote != null) {
                message += String.format(". Вот вам мотивирующая цитата на грядущий день:\n _%s_" , quote);
            }
            slackService.send(message);
        }
    }
}

