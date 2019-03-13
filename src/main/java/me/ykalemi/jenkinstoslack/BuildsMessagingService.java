package me.ykalemi.jenkinstoslack;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * @author ykalemi
 * @since 13.03.19
 */
@Service
public class BuildsMessagingService {

    private final PmsJobsService pmsJobs;
    private final SlackService slackService;
    private final MotivationQuoteService quoteService;
    private final boolean sendOkMessage;

    public BuildsMessagingService(PmsJobsService pmsJobs, SlackService slackService, MotivationQuoteService quoteService,
                                  @Value("${j2s.sendOkMessage:false}") boolean sendOkMessage) {
        this.pmsJobs = pmsJobs;
        this.slackService = slackService;
        this.quoteService = quoteService;
        this.sendOkMessage = sendOkMessage;
    }

    public void sendBuildsStatusMessages() throws IOException {
        List<String> unsuccessfulJobs = pmsJobs.getUnsuccessfulJobs();
        if (!unsuccessfulJobs.isEmpty()) {
            slackService.send("Не было более одной успешной сборки подряд:\n" + StringUtils.join(unsuccessfulJobs, "\n"));
        } else if (sendOkMessage) {
            sendOkMessage(slackService, quoteService);
        }
    }

    private static void sendOkMessage(SlackService slackService, MotivationQuoteService quoteService) {
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
