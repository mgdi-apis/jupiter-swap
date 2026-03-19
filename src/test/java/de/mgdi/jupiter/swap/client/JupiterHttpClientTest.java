package de.mgdi.jupiter.swap.client;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import de.mgdi.jupiter.swap.model.JupiterQuoteRequest;
import de.mgdi.jupiter.swap.model.JupiterQuoteResponse;
import de.mgdi.jupiter.swap.model.JupiterSwapRequest;
import de.mgdi.jupiter.swap.model.JupiterSwapResponse;
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
 * Integration tests for {@link JupiterHttpClient} using WireMock to simulate the Jupiter REST API.
 *
 * @author mgdi consulting
 * @since 2026-03-06
 */
@WireMockTest
class JupiterHttpClientTest {

    private static final String SOL_MINT  = "So11111111111111111111111111111111111111112";
    private static final String USDC_MINT = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v";
    private static final String API_KEY   = "test-api-key";

    private JupiterHttpClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wm) {
        client = JupiterHttpClient.builder()
                .jupiterBaseUrl("http://localhost:" + wm.getHttpPort())
                .jupiterApiKey(API_KEY)
                .requestTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Test
    void testQuoteSuccessReturnsDeserializedResponse() throws Exception {
        stubFor(get(urlPathEqualTo("/swap/v1/quote"))
                .willReturn(okJson(readResource("quote-response.json"))));

        JupiterQuoteRequest request = quoteRequest(1_000_000_000L, 50);
        JupiterQuoteResponse response = client.quote(request);

        assertEquals(SOL_MINT, response.getInputMint());
        assertEquals(USDC_MINT, response.getOutputMint());
        assertEquals("1000000000", response.getInAmount());
        assertEquals("172450000", response.getOutAmount());
        assertEquals(50, response.getSlippageBps());
        assertEquals(1, response.getRoutePlan().length);
        assertEquals("Raydium", response.getRoutePlan()[0].getSwapInfo().getLabel());
    }

    @Test
    void testQuoteSendsApiKeyHeader() throws Exception {
        stubFor(get(urlPathEqualTo("/swap/v1/quote"))
                .willReturn(okJson(readResource("quote-response.json"))));

        client.quote(quoteRequest(1_000_000_000L, 50));

        verify(getRequestedFor(urlPathEqualTo("/swap/v1/quote"))
                .withHeader("x-api-key", equalTo(API_KEY))
                .withHeader("Accept", equalTo("application/json")));
    }

    @Test
    void testQuoteSendsCorrectQueryParameters() throws Exception {
        stubFor(get(urlPathEqualTo("/swap/v1/quote"))
                .willReturn(okJson(readResource("quote-response.json"))));

        client.quote(quoteRequest(1_000_000_000L, 50));

        verify(getRequestedFor(urlPathEqualTo("/swap/v1/quote"))
                .withQueryParam("inputMint",   equalTo(SOL_MINT))
                .withQueryParam("outputMint",  equalTo(USDC_MINT))
                .withQueryParam("amount",      equalTo("1000000000"))
                .withQueryParam("slippageBps", equalTo("50")));
    }

    @Test
    void testQuoteHttpError400ThrowsRuntimeException() {
        stubFor(get(urlPathEqualTo("/swap/v1/quote"))
                .willReturn(badRequest().withBody("invalid params")));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> client.quote(quoteRequest(1_000_000_000L, 50)));

        assertTrue(ex.getMessage().contains("400"));
    }

    @Test
    void testQuoteHttpError500ThrowsRuntimeException() {
        stubFor(get(urlPathEqualTo("/swap/v1/quote"))
                .willReturn(serverError().withBody("internal error")));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> client.quote(quoteRequest(1_000_000_000L, 50)));

        assertTrue(ex.getMessage().contains("500"));
    }

    @Test
    void testQuoteTimeoutThrowsRuntimeException() {
        stubFor(get(urlPathEqualTo("/swap/v1/quote"))
                .willReturn(okJson("{}").withFixedDelay(10_000)));

        assertThrows(RuntimeException.class,
                () -> client.quote(quoteRequest(1_000_000_000L, 50)));
    }

    @Test
    void testSwapSuccessReturnsDeserializedResponse() throws Exception {
        stubFor(post(urlEqualTo("/swap/v1/swap"))
                .willReturn(okJson(readResource("swap-response.json"))));

        JupiterSwapResponse response = client.swap(swapRequest());

        assertEquals("AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAQAHCw==",
                response.getSwapTransaction());
        assertEquals(287654321L, response.getLastValidBlockHeight().longValue());
        assertEquals(5000L, response.getPrioritizationFeeLamports().longValue());
    }

    @Test
    void testSwapSendsCorrectHeaders() throws Exception {
        stubFor(post(urlEqualTo("/swap/v1/swap"))
                .willReturn(okJson(readResource("swap-response.json"))));

        client.swap(swapRequest());

        verify(postRequestedFor(urlEqualTo("/swap/v1/swap"))
                .withHeader("x-api-key",     equalTo(API_KEY))
                .withHeader("Content-Type",  containing("application/json"))
                .withHeader("Accept",        equalTo("application/json")));
    }

    @Test
    void testSwapSendsQuoteResponseInBody() throws Exception {
        stubFor(post(urlEqualTo("/swap/v1/swap"))
                .willReturn(okJson(readResource("swap-response.json"))));

        client.swap(swapRequest());

        verify(postRequestedFor(urlEqualTo("/swap/v1/swap"))
                .withRequestBody(matchingJsonPath("$.quoteResponse.inputMint",  equalTo(SOL_MINT)))
                .withRequestBody(matchingJsonPath("$.quoteResponse.outputMint", equalTo(USDC_MINT)))
                .withRequestBody(matchingJsonPath("$.userPublicKey"))
                .withRequestBody(matchingJsonPath("$.wrapAndUnwrapSol",         equalTo("true")))
                .withRequestBody(matchingJsonPath("$.dynamicComputeUnitLimit",  equalTo("true")))
                .withRequestBody(matchingJsonPath("$.prioritizationFeeLamports",equalTo("auto"))));
    }

    @Test
    void testSwapHttpError400ThrowsRuntimeException() {
        stubFor(post(urlEqualTo("/swap/v1/swap"))
                .willReturn(badRequest().withBody("bad request")));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> client.swap(swapRequest()));

        assertTrue(ex.getMessage().contains("400"));
    }

    @Test
    void testSwapHttpError500ThrowsRuntimeException() {
        stubFor(post(urlEqualTo("/swap/v1/swap"))
                .willReturn(serverError().withBody("server error")));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> client.swap(swapRequest()));

        assertTrue(ex.getMessage().contains("500"));
    }

    @Test
    void testSwapTimeoutThrowsRuntimeException() {
        stubFor(post(urlEqualTo("/swap/v1/swap"))
                .willReturn(okJson("{}").withFixedDelay(10_000)));

        assertThrows(RuntimeException.class, () -> client.swap(swapRequest()));
    }

    private JupiterQuoteRequest quoteRequest(long amount, int slippageBps) {
        return JupiterQuoteRequest.builder()
                .inputMint(SOL_MINT)
                .outputMint(USDC_MINT)
                .amount(amount)
                .slippageBps(slippageBps)
                .build();
    }

    private JupiterSwapRequest swapRequest() {
        JupiterQuoteResponse quote = new JupiterQuoteResponse();
        quote.setInputMint(SOL_MINT);
        quote.setOutputMint(USDC_MINT);
        quote.setInAmount("1000000000");
        quote.setOutAmount("172450000");

        return JupiterSwapRequest.builder()
                .quoteResponse(quote)
                .userPublicKey("UserPubKey1111111111111111111111111111111111")
                .build();
    }

    private String readResource(String filename) throws IOException, URISyntaxException {
        Path path = Path.of(getClass().getClassLoader().getResource(filename).toURI());
        return Files.readString(path);
    }
}
