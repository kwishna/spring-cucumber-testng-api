package org.spring.bdd.single_page_frameworks.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * ApiClientV2 - Enhanced API client for robust API testing
 *
 * Features:
 *  - Spring Environment-aware base URL & timeouts
 *  - Thread-safe TokenManager for OAuth2 Client Credentials flow
 *  - Interceptor hooks for request/response handling
 *  - Smart retry with exponential backoff + condition-based retry
 *  - Metrics collection for request durations + status codes
 *  - Schema validation utilities
 *  - File upload helper
 *  - Request templates for reusable settings
 *  - Soft assertions and predicate-based validations
 */
@Component
public class ApiClientV2 {

    private static final Logger log = LogManager.getLogger(ApiClientV2.class);

    @Autowired
    private Environment env;

    // Defaults (can be overridden via application.properties or environment)
    private final String defaultBaseUrl;
    private final int defaultTimeoutSeconds;
    private final boolean defaultLogRequests;
    private final boolean defaultLogResponses;
    private final int defaultRetries;
    private final long defaultRetryDelayMillis;

    private final ObjectMapper objectMapper;
    private final RestAssuredConfig restAssuredConfig;

    // Interceptors and metrics
    private final List<ApiInterceptor> interceptors = new CopyOnWriteArrayList<>();
    private final MetricsRegistry metricsRegistry = new MetricsRegistry();

    // Token manager shared instance
    private final TokenManager tokenManager = new TokenManager();

    public ApiClientV2(
            @Autowired Environment env,
            @Autowired(required = false) ObjectMapper injectedMapper
    ) {
        this.env = env;
        this.defaultBaseUrl = env.getProperty("api.base.url", "");
        this.defaultTimeoutSeconds = Integer.parseInt(env.getProperty("api.timeout", "30"));
        this.defaultLogRequests = Boolean.parseBoolean(env.getProperty("api.log.requests", "true"));
        this.defaultLogResponses = Boolean.parseBoolean(env.getProperty("api.log.responses", "true"));
        this.defaultRetries = Integer.parseInt(env.getProperty("api.retries", "2"));
        this.defaultRetryDelayMillis = Long.parseLong(env.getProperty("api.retry.delay", "1000"));

        this.objectMapper = injectedMapper != null ? injectedMapper : createObjectMapper();
        restAssuredConfig = createRestAssuredConfig();
        RestAssured.config = restAssuredConfig;
    }

    // -------------------------
    // Public factory methods
    // -------------------------
    public ApiRequestBuilder newRequest() {
        return new ApiRequestBuilder(this);
    }

    public ApiRequestBuilder newRequest(String baseUri) {
        return new ApiRequestBuilder(this).baseUri(baseUri);
    }

    public void registerInterceptor(ApiInterceptor interceptor) {
        this.interceptors.add(interceptor);
    }

    public MetricsRegistry getMetricsRegistry() {
        return metricsRegistry;
    }

    public TokenManager getTokenManager() {
        return tokenManager;
    }

    // -------------------------
    // ObjectMapper helpers
    // -------------------------
    private ObjectMapper createObjectMapper() {
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        m.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        m.configure(SerializationFeature.INDENT_OUTPUT, true);
        return m;
    }

    public String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            log.error("Serialization failed for: {}", o == null ? "null" : o.getClass().getName(), e);
            throw new ApiSerializationException("Serialization failed", e);
        }
    }

    public <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Deserialization failed for: {}", clazz.getName(), e);
            throw new ApiSerializationException("Deserialization failed", e);
        }
    }

    /**
     * Creates RestAssured configuration
     */
    private RestAssuredConfig createRestAssuredConfig() {
        return RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory((cls, charset) -> objectMapper));
    }

    // -------------------------
    // Inner: ApiRequestBuilder
    // -------------------------
    public static class ApiRequestBuilder {
        private final ApiClientV2 client;
        private String baseUri;
        private String basePath = "";
        private String path = "";
        private final Map<String, Object> headers = new LinkedHashMap<>();
        private final Map<String, Object> queryParams = new LinkedHashMap<>();
        private final Map<String, Object> pathParams = new LinkedHashMap<>();
        private Object body;
        private ContentType contentType = ContentType.JSON;
        private AuthConfig authDescriptor;
        private int timeoutSeconds;
        private boolean enableLogging = true;
        private int retryCount;
        private long retryDelayMillis;
        private Predicate<Response> retryCondition = ApiRequestBuilder::defaultRetryCondition;

        public ApiRequestBuilder(ApiClientV2 client) {
            this.client = client;
            this.baseUri = client.defaultBaseUrl;
            this.timeoutSeconds = client.defaultTimeoutSeconds;
            this.retryCount = client.defaultRetries;
            this.retryDelayMillis = client.defaultRetryDelayMillis;
        }

        // Fluent setters
        public ApiRequestBuilder baseUri(String baseUri) { this.baseUri = baseUri; return this; }
        public ApiRequestBuilder basePath(String basePath) { this.basePath = basePath; return this; }
        public ApiRequestBuilder path(String path) { this.path = path; return this; }
        public ApiRequestBuilder header(String name, Object value) { this.headers.put(name, value); return this; }
        public ApiRequestBuilder headers(Map<String, Object> map) { this.headers.putAll(map); return this; }
        public ApiRequestBuilder queryParam(String name, Object value) { this.queryParams.put(name, value); return this; }
        public ApiRequestBuilder queryParams(Map<String, Object> map) { this.queryParams.putAll(map); return this; }
        public ApiRequestBuilder pathParam(String name, Object value) { this.pathParams.put(name, value); return this; }
        public ApiRequestBuilder body(Object body) { this.body = body; return this; }
        public ApiRequestBuilder jsonBody(String json) { this.body = json; return this; }
        public ApiRequestBuilder contentType(ContentType ct) { this.contentType = ct; return this; }
        public ApiRequestBuilder timeout(int seconds) { this.timeoutSeconds = seconds; return this; }
        public ApiRequestBuilder disableLogging() { this.enableLogging = false; return this; }
        public ApiRequestBuilder retries(int count) { this.retryCount = count; return this; }
        public ApiRequestBuilder retryDelayMillis(long delay) { this.retryDelayMillis = delay; return this; }
        public ApiRequestBuilder retryIf(Predicate<Response> condition) { this.retryCondition = condition; return this; }

        // Authentication convenience methods
        public ApiRequestBuilder basicAuth(String user, String pass) {
            this.authDescriptor = new AuthConfig(AuthType.BASIC, user, pass);
            return this;
        }

        public ApiRequestBuilder bearerToken(String token) {
            this.authDescriptor = new AuthConfig(AuthType.BEARER, token);
            return this;
        }

        /**
         * OAuth2 using client credentials - will use TokenManager
         * @param clientId client id
         * @param clientSecret client secret
         * @param tokenUrl token endpoint URL
         */
        public ApiRequestBuilder oauth2ClientCredentials(String clientId, String clientSecret, String tokenUrl) {
            this.authDescriptor = new AuthConfig(AuthType.OAUTH2_CLIENT_CREDENTIALS, clientId, clientSecret, tokenUrl);
            return this;
        }

        // Execution methods
        public ApiResponse get() { return execute("GET"); }
        public ApiResponse post() { return execute("POST"); }
        public ApiResponse put() { return execute("PUT"); }
        public ApiResponse patch() { return execute("PATCH"); }
        public ApiResponse delete() { return execute("DELETE"); }
        public ApiResponse head() { return execute("HEAD"); }
        public ApiResponse options() { return execute("OPTIONS"); }

        private ApiResponse execute(String method) {
            RequestSpecification spec = buildRequestSpec();
            Response lastResponse = null;
            Exception lastException = null;

            // Pre-request interceptors
            client.interceptors.forEach(i -> i.beforeRequest(spec));

            long startTime = System.nanoTime();
            for (int attempt = 0; attempt <= retryCount; attempt++) {
                try {
                    if (attempt > 0) {
                        long backoff = calculateBackoffMillis(attempt);
                        log.warn("Retry attempt {} for {} {} — sleeping {}ms", attempt, method, getFullPath(), backoff);
                        Thread.sleep(backoff);
                    }

                    // Send request
                    Response response = sendWithSpec(spec, method);
                    lastResponse = response;

                    long durationMs = Duration.ofNanos(System.nanoTime() - startTime).toMillis();
                    client.metricsRegistry.record(method, getFullPath(), response.getStatusCode(), durationMs);

                    // Post-response interceptors
                    client.interceptors.forEach(i -> i.afterResponse(response));

                    // Logging
                    if (enableLogging && client.defaultLogResponses) {
                        log.info("Response [{}] {} -> {} ({} ms)",
                                method, getFullPath(), response.getStatusLine(), durationMs);
                    }

                    // Retry decision
                    if (retryCondition != null && retryCondition.test(response)) {
                        // transient error - continue loop to retry
                        log.warn("Retry condition matched (status {}) for {} {}", response.getStatusCode(), method, getFullPath());
                        continue;
                    }

                    return new ApiResponse(response, client.objectMapper, client.metricsRegistry);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    lastException = ie;
                    break;
                } catch (Exception e) {
                    lastException = e;
                    log.warn("Exception during request attempt {} for {} {}: {}", attempt, method, getFullPath(), e.toString());
                    // if last attempt, log error
                    if (attempt == retryCount) {
                        log.error("Request failed after {} attempts for {} {}",
                                attempt + 1, method, getFullPath(), e);
                    }
                }
            }

            // final failure
            throw new ApiRequestException("Request failed for " + method + " " + getFullPath(), lastException);
        }

        private Response sendWithSpec(RequestSpecification spec, String method) {
            String fullPath = getFullPath();
            return switch (method.toUpperCase()) {
                case "GET" -> spec.get(fullPath);
                case "POST" -> spec.post(fullPath);
                case "PUT" -> spec.put(fullPath);
                case "PATCH" -> spec.patch(fullPath);
                case "DELETE" -> spec.delete(fullPath);
                case "HEAD" -> spec.head(fullPath);
                case "OPTIONS" -> spec.options(fullPath);
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            };
        }

        private long calculateBackoffMillis(int attempt) {
            // Exponential backoff with jitter
            double exponential = Math.pow(2.0, attempt);
            long base = Math.max(1, retryDelayMillis);
            long raw = (long) (base * exponential);
            long jitter = ThreadLocalRandom.current().nextLong(base);
            return Math.min(raw + jitter, 30_000); // cap at 30s
        }

        private RequestSpecification buildRequestSpec() {
            RequestSpecBuilder builder = new RequestSpecBuilder();

            if (baseUri != null && !baseUri.isEmpty()) builder.setBaseUri(baseUri);
            if (basePath != null && !basePath.isEmpty()) builder.setBasePath(basePath);
            builder.setContentType(contentType);

            headers.forEach((k, v) -> builder.addHeader(k, String.valueOf(v)));
            queryParams.forEach(builder::addQueryParam);
            pathParams.forEach(builder::addPathParam);

            if (body != null) {
                if (body instanceof String) builder.setBody(body);
                else builder.setBody(body); // ObjectMapper will serialize
            }

            // Timeout config
            builder.setConfig(RestAssured.config()
                    .httpClient(io.restassured.config.HttpClientConfig.httpClientConfig()
                            .setParam("http.connection.timeout", timeoutSeconds * 1000)
                            .setParam("http.socket.timeout", timeoutSeconds * 1000)));

            // Logging filters
            if (enableLogging && client.defaultLogRequests) {
                builder.addFilter(new RequestLoggingFilter(LogDetail.ALL));
            }
            if (enableLogging && client.defaultLogResponses) {
                builder.addFilter(new ResponseLoggingFilter(LogDetail.ALL));
            }

            // Authentication handling
            if (authDescriptor != null) {
                authDescriptor.applyTo(builder, client);
            }

            return builder.build();
        }

        private String getFullPath() {
            // Prefer path as provided; if path is relative then RestAssured will combine baseUri/basePath automatically
            return path;
        }

        private static boolean defaultRetryCondition(Response response) {
            if (response == null) return true;
            int status = response.getStatusCode();
            // Retry on server errors, timeouts, rate limits
            return status == 408 || status == 429 || (status >= 500 && status < 600);
        }
    }

    // -------------------------
    // ApiResponse - enrichments
    // -------------------------
    public static class ApiResponse {
        private final Response raw;
        private final ObjectMapper mapper;
        private final MetricsRegistry metricsRegistry;

        public ApiResponse(Response raw, ObjectMapper mapper, MetricsRegistry metricsRegistry) {
            this.raw = raw;
            this.mapper = mapper;
            this.metricsRegistry = metricsRegistry;
        }

        public int getStatusCode() { return raw.getStatusCode(); }
        public String getStatusLine() { return raw.getStatusLine(); }
        public String getBody() { return raw.getBody().asString(); }
        public JsonPath jsonPath() { return raw.jsonPath(); }
        public <T> T as(Class<T> clazz) { return raw.as(clazz); }
        public Response getRawResponse() { return raw; }
        public long getResponseTimeMillis() { return raw.getTimeIn(TimeUnit.MILLISECONDS); }
        public boolean isSuccess() { return getStatusCode() >= 200 && getStatusCode() < 300; }

        // Assertions
        public ApiResponse assertStatus(int expected) {
            if (getStatusCode() != expected) {
                throw new ApiAssertionException(String.format("Expected %d but got %d. Body: %s", expected, getStatusCode(), getBody()));
            }
            return this;
        }

        public ApiResponse assertSuccess() {
            if (!isSuccess()) {
                throw new ApiAssertionException(String.format("Expected success but got %d. Body: %s", getStatusCode(), getBody()));
            }
            return this;
        }

        public ApiResponse assertJsonPathEquals(String path, Object expected) {
            Object actual = jsonPath().get(path);
            if (!Objects.equals(expected, actual)) {
                throw new ApiAssertionException(String.format("JsonPath %s expected %s but got %s", path, expected, actual));
            }
            return this;
        }

        public ApiResponse softAssert(Consumer<ApiResponse> validator) {
            try {
                validator.accept(this);
            } catch (AssertionError e) {
                log.warn("Soft assertion failed: {}", e.getMessage());
            }
            return this;
        }

        public ApiResponse assertPredicate(String path, Predicate<Object> predicate, String messageIfFail) {
            Object actual = jsonPath().get(path);
            if (!predicate.test(actual)) {
                throw new ApiAssertionException(String.format("Predicate failed for %s: %s (actual=%s)", path, messageIfFail, actual));
            }
            return this;
        }

        public ApiResponse logResponse() {
            log.info("Status: {}, Time: {} ms", getStatusCode(), getResponseTimeMillis());
            log.info("Body: {}", getBody());
            return this;
        }

        // Schema validation helper
        public ApiResponse validateJsonSchema(File schemaFile) {
            raw.then().assertThat().body(io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema(schemaFile));
            return this;
        }

        public ApiResponse validateJsonSchema(String schemaString) {
            raw.then().assertThat().body(io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema(schemaString));
            return this;
        }
    }

    // -------------------------
    // TokenManager (simple client-credentials flow)
    // -------------------------
    public static class TokenManager {
        private final ConcurrentMap<String, CachedToken> tokens = new ConcurrentHashMap<>();
        private final ScheduledExecutorService sweeper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TokenManager-Sweeper");
            t.setDaemon(true);
            return t;
        });

        public TokenManager() {
            // periodic cleanup
            sweeper.scheduleAtFixedRate(this::cleanup, 5, 5, TimeUnit.MINUTES);
        }

        private static class CachedToken {
            final String token;
            final long expiryMillis; // epoch millis

            CachedToken(String token, long expiryMillis) { this.token = token; this.expiryMillis = expiryMillis; }
        }

        public String getToken(String clientId, String clientSecret, String tokenUrl) {
            String key = clientId + "|" + tokenUrl;
            CachedToken ct = tokens.get(key);
            if (ct != null && System.currentTimeMillis() < ct.expiryMillis - 10_000) {
                return ct.token;
            }
            synchronized (key.intern()) {
                // double-check
                ct = tokens.get(key);
                if (ct != null && System.currentTimeMillis() < ct.expiryMillis - 10_000) {
                    return ct.token;
                }
                // request new token
                try {
                    Response r = RestAssured.given()
                            .contentType(ContentType.URLENC)
                            .formParam("grant_type", "client_credentials")
                            .formParam("client_id", clientId)
                            .formParam("client_secret", clientSecret)
                            .post(tokenUrl);

                    if (r.getStatusCode() >= 200 && r.getStatusCode() < 300) {
                        String accessToken = r.jsonPath().getString("access_token");
                        Integer expiresIn = r.jsonPath().getInt("expires_in");
                        long expiry = System.currentTimeMillis() + (expiresIn != null && expiresIn > 0 ? expiresIn * 1000L : 3600_000L);
                        tokens.put(key, new CachedToken(accessToken, expiry));
                        return accessToken;
                    } else {
                        throw new ApiRequestException("Failed to obtain token: " + r.getStatusLine() + " - " + r.getBody().asString());
                    }
                } catch (Exception e) {
                    throw new ApiRequestException("Error fetching token from " + tokenUrl, e);
                }
            }
        }

        private void cleanup() {
            long now = System.currentTimeMillis();
            List<String> expired = tokens.entrySet().stream()
                    .filter(e -> e.getValue().expiryMillis < now)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            expired.forEach(tokens::remove);
        }
    }

    // -------------------------
    // AuthDescriptor / AuthKind
    // -------------------------
    private enum AuthType { NONE, BASIC, BEARER, OAUTH2_CLIENT_CREDENTIALS }

    private static class AuthConfig {
        final AuthType authType;
        final String[] credentials;

        AuthConfig(AuthType authType, String... credentials) {
            this.authType = authType;
            this.credentials = credentials;
        }

        void applyTo(RequestSpecBuilder builder, ApiClientV2 client) {
            switch (authType) {
                case BASIC -> builder.setAuth(RestAssured.basic(credentials[0], credentials[1]));
                case BEARER -> builder.addHeader("Authorization", "Bearer " + credentials[0]);
                case OAUTH2_CLIENT_CREDENTIALS -> {
                    String clientId = credentials[0], clientSecret = credentials[1], tokenUrl = credentials[2];
                    String token = client.getTokenManager().getToken(clientId, clientSecret, tokenUrl);
                    builder.addHeader("Authorization", "Bearer " + token);
                }
                default -> { /* no-op */ }
            }
        }
    }

    // -------------------------
    // File upload helper
    // -------------------------
    public FileUploadBuilder fileUpload() {
        return new FileUploadBuilder(this);
    }

    public static class FileUploadBuilder {
        private final ApiClientV2 client;
        private final Map<String, File> files = new LinkedHashMap<>();
        private final Map<String, String> form = new LinkedHashMap<>();
        private String url;
        private Map<String, Object> headers = new LinkedHashMap<>();

        public FileUploadBuilder(ApiClientV2 client) { this.client = client; }

        public FileUploadBuilder to(String url) { this.url = url; return this; }
        public FileUploadBuilder addFile(String param, File f) { files.put(param, f); return this; }
        public FileUploadBuilder addFile(String param, String path) { files.put(param, new File(path)); return this; }
        public FileUploadBuilder formField(String k, String v) { form.put(k, v); return this; }
        public FileUploadBuilder headers(Map<String, Object> h) { this.headers.putAll(h); return this; }

        public ApiResponse upload() {
            RequestSpecBuilder b = new RequestSpecBuilder();
            headers.forEach((k, v) -> b.addHeader(k, String.valueOf(v)));
            RequestSpecification spec = b.build();
            files.forEach((param, file) -> spec.multiPart(param, file));
            form.forEach(spec::formParam);
            Response r = spec.post(url);
            return new ApiResponse(r, client.objectMapper, client.metricsRegistry);
        }
    }

    // -------------------------
    // Interceptor interface
    // -------------------------
    public interface ApiInterceptor {
        void beforeRequest(RequestSpecification req);
        void afterResponse(Response resp);
    }

    // -------------------------
    // MetricsRegistry (simple)
    // -------------------------
    public static class MetricsRegistry {
        private final ConcurrentMap<String, List<MetricEntry>> metrics = new ConcurrentHashMap<>();

        public void record(String method, String endpoint, int status, long durationMs) {
            String key = method + " " + endpoint;
            metrics.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(new MetricEntry(status, durationMs, System.currentTimeMillis()));
        }

        public Map<String, List<MetricEntry>> snapshot() {
            return new HashMap<>(metrics);
        }

        public static class MetricEntry {
            public final int status;
            public final long durationMs;
            public final long timestamp;

            public MetricEntry(int status, long durationMs, long timestamp) {
                this.status = status;
                this.durationMs = durationMs;
                this.timestamp = timestamp;
            }
        }
    }

    // -------------------------
    // Request template builder
    // -------------------------
    public static class RequestTemplate {
        private String baseUri;
        private String basePath;
        private final Map<String, Object> headers = new LinkedHashMap<>();
        private final Map<String, Object> queryParams = new LinkedHashMap<>();
        private ContentType contentType = ContentType.JSON;
        private AuthConfig authDescriptor;

        public RequestTemplate baseUri(String b) { this.baseUri = b; return this; }
        public RequestTemplate basePath(String p) { this.basePath = p; return this; }
        public RequestTemplate header(String k, Object v) { headers.put(k, v); return this; }
        public RequestTemplate queryParam(String k, Object v) { queryParams.put(k, v); return this; }
        public RequestTemplate contentType(ContentType ct) { this.contentType = ct; return this; }
        public RequestTemplate bearerToken(String token) { this.authDescriptor = new AuthConfig(AuthType.BEARER, token); return this; }
        public RequestTemplate oauth2Client(String clientId, String clientSecret, String tokenUrl) { this.authDescriptor = new AuthConfig(AuthType.OAUTH2_CLIENT_CREDENTIALS, clientId, clientSecret, tokenUrl); return this; }

        public ApiRequestBuilder apply(ApiRequestBuilder builder) {
            if (baseUri != null) builder.baseUri(baseUri);
            if (basePath != null) builder.basePath(basePath);
            builder.contentType(contentType);
            if (!headers.isEmpty()) builder.headers(headers);
            if (!queryParams.isEmpty()) builder.queryParams(queryParams);
            if (authDescriptor != null) {
                // apply auth descriptor state to builder: we'll copy the args
                switch (authDescriptor.authType) {
                    case BEARER -> builder.bearerToken(authDescriptor.credentials[0]);
                    case OAUTH2_CLIENT_CREDENTIALS ->
                            builder.oauth2ClientCredentials(authDescriptor.credentials[0], authDescriptor.credentials[1], authDescriptor.credentials[2]);
                    case BASIC -> builder.basicAuth(authDescriptor.credentials[0], authDescriptor.credentials[1]);
                    default -> {}
                }
            }
            return builder;
        }
    }

    // -------------------------
    // Schema validator helpers
    // -------------------------
    public static class SchemaValidator {
        public static void validateFromFile(ApiResponse resp, File schemaFile) {
            resp.getRawResponse().then().assertThat().body(io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema(schemaFile));
        }
        public static void validateFromString(ApiResponse resp, String schema) {
            resp.getRawResponse().then().assertThat().body(io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema(schema));
        }
    }

    // -------------------------
    // Exception classes
    // -------------------------
    public static class ApiRequestException extends RuntimeException {
        public ApiRequestException(String message) { super(message); }
        public ApiRequestException(String message, Throwable cause) { super(message, cause); }
    }

    public static class ApiSerializationException extends RuntimeException {
        public ApiSerializationException(String message, Throwable cause) { super(message, cause); }
    }

    public static class ApiAssertionException extends AssertionError {
        public ApiAssertionException(String message) { super(message); }
    }

}

