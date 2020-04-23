package me.ykalemi.jenkinstoslack;

import net.gpedro.integrations.slack.SlackApi;
import net.gpedro.integrations.slack.SlackMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author ykalemi
 * @since 08.02.19
 */
@Service
public class SlackServiceImpl implements SlackService {

    private final SlackApi api;

    public SlackServiceImpl(@Value("${slack.uri}") String slackUri) {
        this.api = new SlackApi(slackUri);
    }

    @Override
    public void send(String message) {
        api.call(new SlackMessage(message));
    }
}
