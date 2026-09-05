package com.chargegrid.notification_service.email;

import java.io.IOException;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("'${notification.email.resend.api-key:}' != ''")
public class ResendEmailProvider implements EmailProvider {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();
    private final String apiKey;
    private final String endpoint;

    public ResendEmailProvider(
            @Value("${notification.email.resend.api-key}") String apiKey,
            @Value("${notification.email.resend.endpoint}") String endpoint) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
    }

    @Override
    public String send(EmailMessage message) {
        String json =
                "{\"from\":\""
                        + escape(message.from())
                        + "\",\"to\":[\""
                        + escape(message.to())
                        + "\"],\"subject\":\""
                        + escape(message.subject())
                        + "\",\"text\":\""
                        + escape(message.text())
                        + "\"}";
        Request request =
                new Request.Builder()
                        .url(endpoint)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .post(RequestBody.create(json, JSON))
                        .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new IllegalStateException("Resend returned HTTP " + response.code());
            return response.body() == null ? null : response.body().string();
        } catch (IOException ex) {
            throw new IllegalStateException("Resend request failed", ex);
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
