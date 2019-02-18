package me.ykalemi.jenkinstoslack;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author ykalemi
 * @since 18.02.19
 */
@Component
public class MotivationQuoteService {

    private static final String QUOTE_PROPERTY = "quoteText";
    private final String quoteApi;

    @Autowired
    public MotivationQuoteService(@Value("${motivationQuote.uri}") String quoteApi) {
        this.quoteApi = quoteApi;
    }

    public String getMotivatingQuote() throws IOException {
        URL url = new URL(quoteApi);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(1000);
        con.setReadTimeout(1000);

        StringBuilder content = new StringBuilder();

        try(BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
        }

        JsonFactory factory = new JsonFactory();
        try (JsonParser parser = factory.createParser(content.toString())) {
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (QUOTE_PROPERTY.equals(parser.getCurrentName())) {
                    parser.nextToken();
                    return parser.getText();
                }
            }
        }

        return null;
    }
}
