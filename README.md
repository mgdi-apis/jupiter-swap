# jupiter-swap

Java library for executing token swaps on the [Jupiter Aggregator](https://jup.ag) on Solana.

Handles the full swap lifecycle using the **Jupiter v2 API**: ordering a transaction, signing it locally, and submitting it via Jupiter's execute endpoint — Jupiter handles transaction landing internally.

---

## Requirements

- Java 21+
- Maven
- A Solana wallet (private key as Base58)
- A Jupiter API key ([api.jup.ag](https://api.jup.ag))

---

## Build from source

```bash
git clone https://github.com/mgdi/jupiter-swap.git
cd jupiter-swap
mvn install
```

Then add it as a local dependency:

```xml
<dependency>
    <groupId>de.mgdi</groupId>
    <artifactId>jupiter-swap</artifactId>
    <version>1.0.0</version>
</dependency>
```

SLF4J is used for logging (`slf4j-api` is a transitive dependency). Add a logging backend of your choice to your application (e.g. Logback, Log4j2).

---

## Security warning — private key handling

> **This library signs transactions locally using your private key. The key never leaves your machine and is never sent to Jupiter or any external service.**
>
> However, you are fully responsible for how you store and pass the private key:
>
> - **Never hardcode a private key in source code.** Anyone with access to your repository will have access to your funds.
> - **Never commit a `.env` or `application.properties` file containing a real private key.**
> - Load the key at runtime from a secret manager, environment variable, or a vault — never from a file checked into version control.
> - Use a dedicated wallet with only the funds needed for the swap. Do not use a wallet that holds significant assets.
> - If you believe a private key has been exposed, move your funds immediately to a new wallet.
>
> Losing control of your private key means losing all assets associated with that address — this cannot be undone.

---

## How it works

1. **Order** — calls `GET /swap/v2/order` with the token pair, amount, taker wallet address, and optional config parameters; Jupiter returns an unsigned versioned transaction and a `requestId`
2. **Sign** — the transaction is signed locally with the wallet's private key
3. **Execute** — calls `POST /swap/v2/execute` with the signed transaction and `requestId`; Jupiter routes, lands the transaction, and returns the **final status and signature directly**

No RPC client, no polling — Jupiter v2 blocks until the transaction is confirmed and returns the result.

---

## Usage

### Plain Java

#### Fire and forget — returns transaction signature

```java
JupiterSwap jupiterSwap = JupiterSwap.builder()
        .account(new Account(Base58.decode(privateKey)))
        .jupiterClient(
                JupiterHttpClient.builder()
                        .jupiterBaseUrl("https://api.jup.ag")
                        .jupiterApiKey("your-api-key")
                        .build()
        )
        .build();

String signature = jupiterSwap.swap(SOL_MINT, USDC_MINT, 1_000_000_000L);
```

#### Check if the swap succeeded — including transaction hash

```java
SwapResult result = jupiterSwap.swapAndAwait(SOL_MINT, USDC_MINT, 1_000_000_000L);
result.isSuccess();    // true if status == "Success"
result.getSignature(); // transaction hash, e.g. "5KtP9x..."
```

#### Custom swap configuration

```java
JupiterSwap jupiterSwap = JupiterSwap.builder()
        .account(new Account(Base58.decode(privateKey)))
        .jupiterClient(
                JupiterHttpClient.builder()
                        .jupiterBaseUrl("https://api.jup.ag")
                        .jupiterApiKey("your-api-key")
                        .connectTimeout(Duration.ofSeconds(5))
                        .requestTimeout(Duration.ofSeconds(15))
                        .build()
        )
        .config(
                JupiterSwapConfig.builder()
                        .slippageBps(100)
                        .priorityFee(10_000)
                        .onlyDirectRoutes(false)
                        .build()
        )
        .build();
```

---

## Configuration — JupiterSwapConfig

All parameters in `JupiterSwapConfig` are optional and passed directly to the Jupiter `/order` endpoint. When `null`, Jupiter uses its own defaults.

Use `JupiterSwapConfig.defaultConfig()` for a sensible starting point (`slippageBps=50`), or build your own with the builder.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `slippageBps` | `Integer` | `50` | Maximum accepted slippage in basis points (1 bps = 0.01%). Example: `50` = 0.5%. If `null`, Jupiter determines slippage dynamically. |
| `priorityFee` | `Integer` | `null` | Priority fee in lamports added to the transaction to increase landing speed. Higher values compete better during network congestion. |
| `computeUnitPrice` | `Integer` | `null` | Price per compute unit in micro-lamports. Alternative to `priorityFee` for fine-grained fee control. |
| `onlyDirectRoutes` | `boolean` | `false` | If `true`, only direct token pair routes are used — no multi-hop routing through intermediate tokens. Simpler but may give worse prices. |
| `maxAccounts` | `Integer` | `null` | Maximum number of accounts in the swap route (range: 1–64, Jupiter default: 64). Lower values free up space for custom instructions in the same transaction. |
| `excludeDexes` | `List<String>` | `null` | List of DEX names to exclude from routing, e.g. `["Raydium", "Orca"]`. |
| `excludeRouters` | `List<String>` | `null` | List of Jupiter routers to exclude. Possible values: `"Metis"`, `"JupiterZ"`, `"Dflow"`, `"OKX"`. Excluding routers reduces liquidity competition and may result in worse prices. |

> **Note on routing restrictions:** Parameters like `excludeRouters`, `excludeDexes`, and `onlyDirectRoutes` restrict which paths Jupiter can use. This may improve predictability but reduces the chance of getting the best price.

---

### Spring Boot

Define the `JupiterSwap` bean in a `@Configuration` class and inject it wherever needed.

```java
@Configuration
public class JupiterSwapConfig {

    @Value("${jupiter.base-url}")
    private String baseUrl;

    @Value("${jupiter.api-key}")
    private String apiKey;

    @Value("${solana.private-key}")
    private String privateKey;

    @Bean
    public JupiterSwap jupiterSwap() {
        return JupiterSwap.builder()
                .account(new Account(Base58.decode(privateKey)))
                .jupiterClient(
                        JupiterHttpClient.builder()
                                .jupiterBaseUrl(baseUrl)
                                .jupiterApiKey(apiKey)
                                .build()
                )
                .config(de.mgdi.jupiter.swap.config.JupiterSwapConfig.defaultConfig())
                .build();
    }
}
```

`application.properties`:
```properties
jupiter.base-url=https://api.jup.ag
jupiter.api-key=your-api-key
solana.private-key=your-base58-private-key
```

Inject and use:
```java
@Service
public class TradingService {

    private final JupiterSwap jupiterSwap;

    public TradingService(JupiterSwap jupiterSwap) {
        this.jupiterSwap = jupiterSwap;
    }

    public SwapResult executeSolToUsdc(long lamports) throws Exception {
        return jupiterSwap.swapAndAwait(SOL_MINT, USDC_MINT, lamports);
    }
}
```

Add Logback to get logs out of the library:
```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
</dependency>
```

---

### Quarkus

Register the `JupiterSwap` instance as an `@ApplicationScoped` CDI bean.

```java
@ApplicationScoped
public class JupiterSwapProducer {

    @ConfigProperty(name = "jupiter.base-url")
    String baseUrl;

    @ConfigProperty(name = "jupiter.api-key")
    String apiKey;

    @ConfigProperty(name = "solana.private-key")
    String privateKey;

    @Produces
    @ApplicationScoped
    public JupiterSwap jupiterSwap() {
        return JupiterSwap.builder()
                .account(new Account(Base58.decode(privateKey)))
                .jupiterClient(
                        JupiterHttpClient.builder()
                                .jupiterBaseUrl(baseUrl)
                                .jupiterApiKey(apiKey)
                                .build()
                )
                .config(JupiterSwapConfig.defaultConfig())
                .build();
    }
}
```

`application.properties`:
```properties
jupiter.base-url=https://api.jup.ag
jupiter.api-key=your-api-key
solana.private-key=your-base58-private-key
```

Inject and use:
```java
@ApplicationScoped
public class TradingService {

    @Inject
    JupiterSwap jupiterSwap;

    public SwapResult executeSolToUsdc(long lamports) throws Exception {
        return jupiterSwap.swapAndAwait(SOL_MINT, USDC_MINT, lamports);
    }
}
```

Quarkus includes JBoss Logging bridged to SLF4J by default — no additional logging dependency needed.

---

## Common token mint addresses

| Token | Mint address |
|---|---|
| SOL  | `So11111111111111111111111111111111111111112` |
| USDC | `EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v` |
| USDT | `Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB` |

---

## License

MIT
