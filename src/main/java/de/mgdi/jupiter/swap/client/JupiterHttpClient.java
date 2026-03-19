package de.mgdi.jupiter.swap.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mgdi.jupiter.swap.model.JupiterQuoteRequest;
import de.mgdi.jupiter.swap.model.JupiterQuoteResponse;
import de.mgdi.jupiter.swap.model.JupiterSwapRequest;
import de.mgdi.jupiter.swap.model.JupiterSwapResponse;
import lombok.Builder;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * HTTP implementation of {@link JupiterClient} using the Java built-in {@link java.net.http.HttpClient}.
 * Communicates with the Jupiter v1 REST API for quote and swap requests.
 *
 * @author mgdi consulting
 * @since 2026-03-06
 */
public class JupiterHttpClient implements JupiterClient {

    private final String QUOTE_URL = "/swap/v1/quote?inputMint=%s&outputMint=%s&amount=%s&slippageBps=%s";
    private final String SWAP_URL  = "/swap/v1/swap";

    private final String HEADER_ACCEPT = "Accept";
    private final String HEADER_CONTENT_TYPE = "Content-Type";
    private final String HEADER_X_API_KEY = "x-api-key";

    private final String jupiterBaseUrl;
    private final String jupiterApiKey;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Builder
    private JupiterHttpClient(final String jupiterBaseUrl, final String jupiterApiKey,
                              final String userPublicKey,
                              final Duration connectTimeout, final Duration requestTimeout) {
        this.jupiterBaseUrl = jupiterBaseUrl;
        this.jupiterApiKey = jupiterApiKey;
        this.connectTimeout = connectTimeout != null ? connectTimeout : Duration.ofSeconds(10);
        this.requestTimeout = requestTimeout != null ? requestTimeout : Duration.ofSeconds(30);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.connectTimeout)
                .build();
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    @Override
    public JupiterQuoteResponse quote(final JupiterQuoteRequest request) {
        try {
            final String uri =
                    String.format(QUOTE_URL,
                            URLEncoder.encode(request.getInputMint(), StandardCharsets.UTF_8),
                            URLEncoder.encode(request.getOutputMint(), StandardCharsets.UTF_8),
                            request.getAmount(),
                            request.getSlippageBps());

            final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(jupiterBaseUrl + uri))
                    .timeout(requestTimeout)
                    .header(HEADER_ACCEPT, "application/json")
                    .header(HEADER_X_API_KEY, jupiterApiKey)
                    .GET();

            final HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new RuntimeException("Jupiter quote API error " + response.statusCode() + ": " + response.body());
            }

            return objectMapper.readValue(response.body(), JupiterQuoteResponse.class);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Jupiter quote", e);
        }
    }

    @Override
    public JupiterSwapResponse swap(final JupiterSwapRequest request) {
        try {
            final String bodyJson = objectMapper.writeValueAsString(request);

            final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(jupiterBaseUrl + SWAP_URL))
                    .timeout(requestTimeout)
                    .header(HEADER_CONTENT_TYPE, "application/json")
                    .header(HEADER_ACCEPT, "application/json")
                    .header(HEADER_X_API_KEY, jupiterApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson));

            final HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new RuntimeException("Jupiter swap API error " + response.statusCode() + ": " + response.body());
            }

            return objectMapper.readValue(response.body(), JupiterSwapResponse.class);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute Jupiter swap", e);
        }
    }
}
