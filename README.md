# jupiter-swap

Java library for executing token swaps on the [Jupiter Aggregator](https://jup.ag) on Solana.

Handles the full swap lifecycle: fetching a quote, signing the transaction, submitting it to the RPC node with retry, and optionally polling for confirmation.

---

## Requirements

- Java 21+
- Maven
- A Solana wallet (private key as Base58)
- A Jupiter API key ([lite-api.jup.ag](https://lite-api.jup.ag) or your own endpoint)
- A Solana RPC endpoint

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
    <version>0.9.0</version>
</dependency>
```

SLF4J is used for logging (`slf4j-api` is a transitive dependency). Add a logging backend of your choice to your application (e.g. Logback, Log4j2).

---

## How it works

1. **Quote** — fetches the best route from Jupiter for the given token pair and amount
2. **Swap** — submits the quote to Jupiter which returns a pre-built, partially signed versioned transaction
3. **Sign** — the transaction is signed locally with the wallet's private key
4. **Send** — the signed transaction is submitted to the Solana RPC node, with configurable retry on failure
5. **Await** *(optional)* — polls the RPC node until the transaction is confirmed or the timeout is reached

---

## Usage

### Plain Java

#### Fire and forget — returns transaction hash immediately

```java
JupiterSwap jupiterSwap = JupiterSwap.builder()
        .account(new Account(Base58.decode(privateKey)))
        .rpcClient(new RpcClient(Cluster.MAINNET))
        .jupiterClient(
                JupiterHttpClient.builder()
                        .jupiterBaseUrl("https://lite-api.jup.ag")
                        .jupiterApiKey("your-api-key")
                        .build()
        )
        .build();

String txHash = jupiterSwap.swap(SOL_MINT, USDC_MINT, 1_000_000_000L);
```

#### Wait for confirmation

```java
boolean confirmed = jupiterSwap.swapAndAwait(SOL_MINT, USDC_MINT, 1_000_000_000L);
```

#### Poll an already submitted transaction

```java
boolean confirmed = jupiterSwap.awaitTransaction(txHash);
```

#### Custom configuration

```java
JupiterSwap jupiterSwap = JupiterSwap.builder()
        .account(new Account(Base58.decode(privateKey)))
        .rpcClient(new RpcClient(Cluster.MAINNET))
        .jupiterClient(
                JupiterHttpClient.builder()
                        .jupiterBaseUrl("https://lite-api.jup.ag")
                        .jupiterApiKey("your-api-key")
                        .connectTimeout(Duration.ofSeconds(5))
                        .requestTimeout(Duration.ofSeconds(15))
                        .build()
        )
        .config(
                JupiterSwapConfig.builder()
                        .sendRetryCount(3)
                        .sendRetryTimeoutMs(30_000)   // 3 attempts over 30s → 10s between retries
                        .statusCheckCount(10)
                        .statusCheckTimeoutMs(60_000) // 10 polls over 60s → every 6s
                        .slippageBps(50)              // 0.5% max slippage
                        .build()
        )
        .build();
```

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
                .rpcClient(new RpcClient(Cluster.MAINNET))
                .jupiterClient(
                        JupiterHttpClient.builder()
                                .jupiterBaseUrl(baseUrl)
                                .jupiterApiKey(apiKey)
                                .build()
                )
                .build();
    }
}
```

`application.properties`:
```properties
jupiter.base-url=https://lite-api.jup.ag
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

    public boolean executeSolToUsdc(long lamports) throws Exception {
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
                .rpcClient(new RpcClient(Cluster.MAINNET))
                .jupiterClient(
                        JupiterHttpClient.builder()
                                .jupiterBaseUrl(baseUrl)
                                .jupiterApiKey(apiKey)
                                .build()
                )
                .build();
    }
}
```

`application.properties`:
```properties
jupiter.base-url=https://lite-api.jup.ag
jupiter.api-key=your-api-key
solana.private-key=your-base58-private-key
```

Inject and use:
```java
@ApplicationScoped
public class TradingService {

    @Inject
    JupiterSwap jupiterSwap;

    public boolean executeSolToUsdc(long lamports) throws Exception {
        return jupiterSwap.swapAndAwait(SOL_MINT, USDC_MINT, lamports);
    }
}
```

Quarkus includes JBoss Logging bridged to SLF4J by default — no additional logging dependency needed.

---

## Common token mint addresses

| Token | Mint address |
|-------|-------------|
| SOL   | `So11111111111111111111111111111111111111112` |
| USDC  | `EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v` |
| USDT  | `Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB` |

---

## License

MIT
