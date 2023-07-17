package net.lessenger.practicalgpt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Data
public class GPT4Service {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String GPT_URL = "https://api.openai.com/v1/chat/completions";

    private final HttpClient client = HttpClient.newHttpClient();
    private String apiKey;

    public String complete(final String prompt) throws IOException, InterruptedException {
        final List<ChatMessage> segments = new LinkedList<>();
        segments.add(new ChatMessage("user",prompt));

        final ChatRequest chatRequest = new ChatRequest();
        chatRequest.messages.addAll(segments);
        chatRequest.max_tokens = 4000; // maximum is 8000
        chatRequest.model = "gpt-4";

        final String strChatRequest = mapper.writeValueAsString(chatRequest);

        log.debug("ChatGPT request: " + strChatRequest);

        final HttpRequest request = HttpRequest.newBuilder().uri(URI.create(GPT_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(strChatRequest)).build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("ChatGPT returned status code " + response.statusCode() + " with body " + response.body());
            throw new RuntimeException("ChatGPT returned status code " + response.statusCode() + " with body " + response.body());
        }

        log.debug(response.body());
        final ChatResponse chatResponse = mapper.readValue(response.body(), ChatResponse.class);

        if (chatResponse.choices.size() == 0) {
            throw new RuntimeException("ChatGPT returned no choices");
        }

        return chatResponse.choices.get(0).message.content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static class ChatRequest {

        public String model = "gpt-4";

        public double temperature = 0.7;

        public double presence_penalty = 0.0;

        public double frequency_penalty = 0.0;

        public int max_tokens = 4000;

        public List<ChatMessage> messages = new LinkedList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static class ChatMessage {
        public ChatMessage() {

        }

        public ChatMessage(final String role, final String content) {
            this.role = role;
            this.content = content;
        }

        public String role;

        public String content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static class ChatResponse {
        public String id;
        public List<ChatChoice> choices = new LinkedList<>();
        public ErrorResponse error;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static class ChatChoice {
        public Integer index;
        public ChatMessage message;
        public String finish_reason;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static class Usage {

        public Integer prompt_tokens;

        public Integer completion_tokens;

        public Integer total_tokens;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static class ErrorResponse {
        public String message;
        public String type;
        public String param;
        public String code;
    }
}
