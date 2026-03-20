package de.mgdi.jupiter.swap.client;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import de.mgdi.jupiter.swap.model.JupiterExecuteRequest;
import de.mgdi.jupiter.swap.model.JupiterExecuteResponse;
import de.mgdi.jupiter.swap.model.JupiterOrderRequest;
import de.mgdi.jupiter.swap.model.JupiterOrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link JupiterHttpClient} using WireMock to simulate the Jupiter v2 REST API.
 *
 * @author mgdi consulting
 * @since 2026-03-20
 */
@WireMockTest
class JupiterHttpClientTest {

    private static final String SOL_MINT   = "So11111111111111111111111111111111111111112";
    private static final String USDC_MINT  = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v";
    private static final String TAKER      = "UserPubKey1111111111111111111111111111111111";
    private static final String API_KEY    = "test-api-key";
    private static final String REQUEST_ID = "abc123-request-id";

    private JupiterHttpClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wm) {
        client = JupiterHttpClient.builder()
                .jupiterBaseUrl("http://localhost:" + wm.getHttpPort())
                .jupiterApiKey(API_KEY)
                .requestTimeout(Duration.ofSeconds(5))
                .build();
    }

    // -------------------------------------------------------------------------
    // order
    // -------------------------------------------------------------------------

    @Test
    void testOrderSuccessReturnsDeserializedResponse() throws Exception {
        stubFor(get(urlPathEqualTo("/swap/v2/order"))
                .willReturn(okJson(readResource("order-response.json"))));

        JupiterOrderResponse response = client.order(orderRequest(1_000_000_000L));

        assertEquals("AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAQAHCw==",
                response.getTransaction());
        assertEquals(REQUEST_ID, response.getRequestId());
        assertEquals("172450000", response.getOutAmount());
        assertEquals("iris", response.getRouter());
    }

    @Test
    void testOrderSendsApiKeyHeader() throws Exception {
        stubFor(get(urlPathEqualTo("/swap/v2/order"))
                .willReturn(okJson(readResource("order-response.json"))));

        client.order(orderRequest(1_000_000_000L));

        verify(getRequestedFor(urlPathEqualTo("/swap/v2/order"))
                .withHeader("x-api-key", equalTo(API_KEY))
                .withHeader("Accept", equalTo("application/json")));
    }

    @Test
    void testOrderSendsCorrectQueryParameters() throws Exception {
        stubFor(get(urlPathEqualTo("/swap/v2/order"))
                .willReturn(okJson(readResource("order-response.json"))));

        client.order(orderRequest(1_000_000_000L));

        verify(getRequestedFor(urlPathEqualTo("/swap/v2/order"))
                .withQueryParam("inputMint",  equalTo(SOL_MINT))
                .withQueryParam("outputMint", equalTo(USDC_MINT))
                .withQueryParam("amount",     equalTo("1000000000"))
                .withQueryParam("taker",      equalTo(TAKER)));
    }

    @Test
    void testOrderHttpError400ThrowsRuntimeException() {
        stubFor(get(urlPathEqualTo("/swap/v2/order"))
                .willReturn(badRequest().withBody("invalid params")));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> client.order(orderRequest(1_000_000_000L)));

        assertTrue(ex.getMessage().contains("400"));
    }

    @Test
    void testOrderHttpError500ThrowsRuntimeException() {
        stubFor(get(urlPathEqualTo("/swap/v2/order"))
                .willReturn(serverError().withBody("internal error")));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> client.order(orderRequest(1_000_000_000L)));

        assertTrue(ex.getMessage().contains("500"));
    }

    @Test
    void testOrderTimeoutThrowsRuntimeException() {
        stubFor(get(urlPathEqualTo("/swap/v2/order"))
                .willReturn(okJson("{}").withFixedDelay(10_000)));

        assertThrows(RuntimeException.class, () -> client.order(orderRequest(1_000_000_000L)));
    }

    // -------------------------------------------------------------------------
    // execute
    // -------------------------------------------------------------------------

    @Test
    void testExecuteSuccessReturnsDeserializedResponse() throws Exception {
        stubFor(post(urlEqualTo("/swap/v2/execute"))
                .willReturn(okJson(readResource("execute-response.json"))));

        JupiterExecuteResponse response = client.execute(executeRequest());

        assertEquals("Success", response.getStatus());
        assertEquals("5KtPn3DXXzHkb7VAVHZGwXJQg8HvFqPR5R6EFgFHMXaT2sLmKZJoNRzJvFwMdBf9dHkQB1yJXjVNnPpQfsTBYhz",
                response.getSignature());
        assertEquals(0, response.getCode());
        assertEquals("1000000000", response.getInputAmountResult());
        assertEquals("172450000", response.getOutputAmountResult());
    }

    @Test
    void testExecuteSendsCorrectHeaders() throws Exception {
        stubFor(post(urlEqualTo("/swap/v2/execute"))
                .willReturn(okJson(readResource("execute-response.json"))));

        client.execute(executeRequest());

        verify(postRequestedFor(urlEqualTo("/swap/v2/execute"))
                .withHeader("x-api-key",    equalTo(API_KEY))
                .withHeader("Content-Type", containing("application/json"))
                .withHeader("Accept",       equalTo("application/json")));
    }

    @Test
    void testExecuteSendsSignedTransactionAndRequestIdInBody() throws Exception {
        stubFor(post(urlEqualTo("/swap/v2/execute"))
                .willReturn(okJson(readResource("execute-response.json"))));

        client.execute(executeRequest());

        verify(postRequestedFor(urlEqualTo("/swap/v2/execute"))
                .withRequestBody(matchingJsonPath("$.signedTransaction"))
                .withRequestBody(matchingJsonPath("$.requestId", equalTo(REQUEST_ID))));
    }

    @Test
    void testExecuteHttpError400ThrowsRuntimeException() {
        stubFor(post(urlEqualTo("/swap/v2/execute"))
                .willReturn(badRequest().withBody("bad request")));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> client.execute(executeRequest()));

        assertTrue(ex.getMessage().contains("400"));
    }

    @Test
    void testExecuteHttpError500ThrowsRuntimeException() {
        stubFor(post(urlEqualTo("/swap/v2/execute"))
                .willReturn(serverError().withBody("server error")));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> client.execute(executeRequest()));

        assertTrue(ex.getMessage().contains("500"));
    }

    @Test
    void testExecuteTimeoutThrowsRuntimeException() {
        stubFor(post(urlEqualTo("/swap/v2/execute"))
                .willReturn(okJson("{}").withFixedDelay(10_000)));

        assertThrows(RuntimeException.class, () -> client.execute(executeRequest()));
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private JupiterOrderRequest orderRequest(long amount) {
        return JupiterOrderRequest.builder()
                .inputMint(SOL_MINT)
                .outputMint(USDC_MINT)
                .amount(amount)
                .taker(TAKER)
                .build();
    }

    private JupiterExecuteRequest executeRequest() {
        return JupiterExecuteRequest.builder()
                .signedTransaction("AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAQAHCw==")
                .requestId(REQUEST_ID)
                .build();
    }

    private String readResource(String filename) throws IOException, URISyntaxException {
        Path path = Path.of(getClass().getClassLoader().getResource(filename).toURI());
        return Files.readString(path);
    }
}
