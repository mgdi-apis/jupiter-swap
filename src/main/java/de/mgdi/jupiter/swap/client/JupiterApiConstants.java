package de.mgdi.jupiter.swap.client;

/**
 * Constants for the Jupiter v2 REST API — URLs, headers, and query parameter names.
 *
 * @author mgdi consulting
 * @since 2026-03-20
 */
public final class JupiterApiConstants {

    private JupiterApiConstants() {
    }

    public static final String ORDER_URL = "/swap/v2/order?inputMint=%s&outputMint=%s&amount=%s&taker=%s";
    public static final String EXECUTE_URL = "/swap/v2/execute";

    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_X_API_KEY = "x-api-key";

    public static final String PARAM_SLIPPAGE_BPS = "slippageBps";
    public static final String PARAM_PRIORITY_FEE = "priorityFee";
    public static final String PARAM_COMPUTE_UNIT_PRICE = "computeUnitPrice";
    public static final String PARAM_ONLY_DIRECT_ROUTES = "onlyDirectRoutes";
    public static final String PARAM_MAX_ACCOUNTS = "maxAccounts";
    public static final String PARAM_EXCLUDE_DEXES = "excludeDexes";
    public static final String PARAM_EXCLUDE_ROUTERS = "excludeRouters";
}
