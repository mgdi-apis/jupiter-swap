package de.mgdi.jupiter.swap.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mgdi.jupiter.swap.model.JupiterExecuteRequest;
import de.mgdi.jupiter.swap.model.JupiterExecuteResponse;
import de.mgdi.jupiter.swap.model.JupiterOrderRequest;
import de.mgdi.jupiter.swap.model.JupiterOrderResponse;
import lombok.Builder;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * HTTP implementation of {@link JupiterClient} using the Java built-in {@link java.net.http.HttpClient}.
 * Communicates with the Jupiter v2 REST API for order and execute requests.
 *
 * @author mgdi consulting
 * @since 2026-03-20
 */
public class JupiterHttpClient implements JupiterClient {

    private final String jupiterBaseUrl;
    private final String jupiterApiKey;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Builder
    private JupiterHttpClient(final String jupiterBaseUrl, final String jupiterApiKey,
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
    public JupiterOrderResponse order(final JupiterOrderRequest request) {
        try {
            final StringBuilder uri = new StringBuilder(String.format(JupiterApiConstants.ORDER_URL,
                    URLEncoder.encode(request.getInputMint(), StandardCharsets.UTF_8),
                    URLEncoder.encode(request.getOutputMint(), StandardCharsets.UTF_8),
                    request.getAmount(),
                    URLEncoder.encode(request.getTaker(), StandardCharsets.UTF_8)));

            appendParam(uri, JupiterApiConstants.PARAM_SLIPPAGE_BPS, request.getSlippageBps());
            appendParam(uri, JupiterApiConstants.PARAM_PRIORITY_FEE, request.getPriorityFee());
            appendParam(uri, JupiterApiConstants.PARAM_COMPUTE_UNIT_PRICE, request.getComputeUnitPrice());
            appendParam(uri, JupiterApiConstants.PARAM_ONLY_DIRECT_ROUTES, Boolean.TRUE.equals(request.getOnlyDirectRoutes()) ? true : null);
            appendParam(uri, JupiterApiConstants.PARAM_MAX_ACCOUNTS, request.getMaxAccounts());
            appendParam(uri, JupiterApiConstants.PARAM_EXCLUDE_DEXES, request.getExcludeDexes());
            appendParam(uri, JupiterApiConstants.PARAM_EXCLUDE_ROUTERS, request.getExcludeRouters());

            final HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(jupiterBaseUrl + uri))
                    .timeout(requestTimeout)
                    .header(JupiterApiConstants.HEADER_ACCEPT, "application/json")
                    .header(JupiterApiConstants.HEADER_X_API_KEY, jupiterApiKey)
                    .GET()
                    .build();

            final HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Jupiter order API error " + response.statusCode() + ": " + response.body());
            }

            return objectMapper.readValue(response.body(), JupiterOrderResponse.class);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Jupiter order", e);
        }
    }

    @Override
    public JupiterExecuteResponse execute(final JupiterExecuteRequest request) {
        try {
            final String bodyJson = objectMapper.writeValueAsString(request);

            final HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(jupiterBaseUrl + JupiterApiConstants.EXECUTE_URL))
                    .timeout(requestTimeout)
                    .header(JupiterApiConstants.HEADER_CONTENT_TYPE, "application/json")
                    .header(JupiterApiConstants.HEADER_ACCEPT, "application/json")
                    .header(JupiterApiConstants.HEADER_X_API_KEY, jupiterApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            final HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Jupiter execute API error " + response.statusCode() + ": " + response.body());
            }

            return objectMapper.readValue(response.body(), JupiterExecuteResponse.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute Jupiter swap", e);
        }
    }

    private void appendParam(final StringBuilder uri, final String name, final Object value) {
        if (value != null) {
            uri.append('&').append(name).append('=').append(value);
        }
    }

    private void appendParam(final StringBuilder uri, final String name, final List<String> values) {
        if (values != null && !values.isEmpty()) {
            uri.append('&').append(name).append('=').append(String.join(",", values));
        }
    }
}
