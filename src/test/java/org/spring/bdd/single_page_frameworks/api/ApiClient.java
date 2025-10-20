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
import io.restassured.http.Headers;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Production-grade REST Assured API Testing Framework * * Features: * - Fluent API for building requests * - Automatic serialization/deserialization * - Request/Response logging * - Authentication support (Basic, Bearer, OAuth) * - Header management * - Query and path parameter handling * - File upload/download * - Response validation and extraction * - Retry mechanism for flaky tests * - Custom configurations * * Usage: * <pre> * ApiResponse response = apiClient.newRequest() * .baseUri("https://api.example.com") * .path("/users/{id}") * .pathParam("id", "123") * .header("Authorization", "Bearer token") * .queryParam("include", "details") * .get(); * * User user = response.as(User.class); * </pre>
 */
@Component
public class ApiClient {
    private static final Logger log = LogManager.getLogger(ApiClient.class);
    @Value("${api.base.url:}")
    private String defaultBaseUrl;
    @Value("${api.timeout:30}")
    private int defaultTimeout;
    @Value("${api.log.requests:true}")
    private boolean logRequests;
    @Value("${api.log.responses:true}")
    private boolean logResponses;
    private final ObjectMapper objectMapper;
    private final RestAssuredConfig config;

    public ApiClient() {
        this.objectMapper = createObjectMapper();
        this.config = createRestAssuredConfig();
        RestAssured.config = config;
    }

    /**
     * Creates a new API request builder
     */
    public ApiRequestBuilder newRequest() {
        return new ApiRequestBuilder(this);
    }

    /**
     * Creates a new API request builder with base URI
     */
    public ApiRequestBuilder newRequest(String baseUri) {
        return new ApiRequestBuilder(this).baseUri(baseUri);
    }

    /**
     * Serializes object to JSON string
     */
    public String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Serialization failed for object: {}", object.getClass().getName(), e);
            throw new ApiSerializationException("Failed to serialize object", e);
        }
    }

    /**
     * Deserializes JSON string to object
     */
    public <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Deserialization failed for class: {}", clazz.getName(), e);
            throw new ApiSerializationException("Failed to deserialize JSON", e);
        }
    }

    /**
     * Creates configured ObjectMapper
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }

    /**
     * Creates RestAssured configuration
     */
    private RestAssuredConfig createRestAssuredConfig() {
        return RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory((cls, charset) -> objectMapper));
    }

    // ═══════════════════════════════════════════════════════════════════════════ // Inner Classes // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Fluent API Request Builder
     */
    public static class ApiRequestBuilder {
        private final ApiClient client;
        private String baseUri;
        private String basePath = "";
        private String path = "";
        private final Map<String, Object> headers = new HashMap<>();
        private final Map<String, Object> queryParams = new HashMap<>();
        private final Map<String, Object> pathParams = new HashMap<>();
        private final Map<String, Object> formParams = new HashMap<>();
        private Object body;
        private ContentType contentType = ContentType.JSON;
        private AuthConfig authConfig;
        private int timeout;
        private boolean enableLogging = true;
        private int retryCount = 0;
        private long retryDelay = 1000;

        private ApiRequestBuilder(ApiClient client) {
            this.client = client;
            this.baseUri = client.defaultBaseUrl;
            this.timeout = client.defaultTimeout;
        }

        public ApiRequestBuilder baseUri(String baseUri) {
            this.baseUri = baseUri;
            return this;
        }

        public ApiRequestBuilder basePath(String basePath) {
            this.basePath = basePath;
            return this;
        }

        public ApiRequestBuilder path(String path) {
            this.path = path;
            return this;
        }

        public ApiRequestBuilder header(String name, Object value) {
            this.headers.put(name, value);
            return this;
        }

        public ApiRequestBuilder headers(Map<String, Object> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public ApiRequestBuilder contentType(ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        public ApiRequestBuilder accept(ContentType contentType) {
            return header("Accept", contentType.toString());
        }

        public ApiRequestBuilder queryParam(String name, Object value) {
            this.queryParams.put(name, value);
            return this;
        }

        public ApiRequestBuilder queryParams(Map<String, Object> params) {
            this.queryParams.putAll(params);
            return this;
        }

        public ApiRequestBuilder pathParam(String name, Object value) {
            this.pathParams.put(name, value);
            return this;
        }

        public ApiRequestBuilder pathParams(Map<String, Object> params) {
            this.pathParams.putAll(params);
            return this;
        }

        public ApiRequestBuilder formParam(String name, Object value) {
            this.formParams.put(name, value);
            return this;
        }

        public ApiRequestBuilder body(Object body) {
            this.body = body;
            return this;
        }

        public ApiRequestBuilder jsonBody(String json) {
            this.body = json;
            return this;
        }

        public ApiRequestBuilder basicAuth(String username, String password) {
            this.authConfig = new AuthConfig(AuthType.BASIC, username, password);
            return this;
        }

        public ApiRequestBuilder bearerToken(String token) {
            this.authConfig = new AuthConfig(AuthType.BEARER, token);
            return this;
        }

        public ApiRequestBuilder oauth2(String accessToken) {
            this.authConfig = new AuthConfig(AuthType.OAUTH2, accessToken);
            return this;
        }

        public ApiRequestBuilder apiKey(String keyName, String keyValue) {
            this.authConfig = new AuthConfig(AuthType.API_KEY, keyName, keyValue);
            return this;
        }

        public ApiRequestBuilder timeout(int seconds) {
            this.timeout = seconds;
            return this;
        }

        public ApiRequestBuilder disableLogging() {
            this.enableLogging = false;
            return this;
        }

        public ApiRequestBuilder retry(int count, long delayMillis) {
            this.retryCount = count;
            this.retryDelay = delayMillis;
            return this;
        }

        public ApiResponse get() {
            return execute("GET");
        }

        public ApiResponse post() {
            return execute("POST");
        }

        public ApiResponse put() {
            return execute("PUT");
        }

        public ApiResponse patch() {
            return execute("PATCH");
        }

        public ApiResponse delete() {
            return execute("DELETE");
        }

        public ApiResponse head() {
            return execute("HEAD");
        }

        public ApiResponse options() {
            return execute("OPTIONS");
        }

        private ApiResponse execute(String method) {
            RequestSpecification spec = buildRequestSpec();
            Response response = null;
            Exception lastException = null;
            for (int attempt = 0; attempt <= retryCount; attempt++) {
                try {
                    if (attempt > 0) {
                        log.warn("Retry attempt {} for {} {}", attempt, method, getFullPath());
                        Thread.sleep(retryDelay);
                    }
                    response = switch (method.toUpperCase()) {
                        case "GET" -> spec.get(getFullPath());
                        case "POST" -> spec.post(getFullPath());
                        case "PUT" -> spec.put(getFullPath());
                        case "PATCH" -> spec.patch(getFullPath());
                        case "DELETE" -> spec.delete(getFullPath());
                        case "HEAD" -> spec.head(getFullPath());
                        case "OPTIONS" -> spec.options(getFullPath());
                        default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
                    };
                    return new ApiResponse(response, client.objectMapper);
                } catch (Exception e) {
                    lastException = e;
                    if (attempt == retryCount) {
                        log.error("Request failed after {} attempts: {} {}", attempt + 1, method, getFullPath(), e);
                    }
                }
            }
            throw new ApiRequestException("Request failed after " + (retryCount + 1) + " attempts", lastException);
        }

        private RequestSpecification buildRequestSpec() {
            RequestSpecBuilder builder = new RequestSpecBuilder();
            // Base URI
            if (baseUri != null && !baseUri.isEmpty()) {
                builder.setBaseUri(baseUri);
            }
            if (!basePath.isEmpty()) {
                builder.setBasePath(basePath);
            }

            // Content Type
            builder.setContentType(contentType);

            // Headers
            headers.forEach((key, value) -> builder.addHeader(key, String.valueOf(value)));

            // Query Params
            queryParams.forEach(builder::addQueryParam);

            // Path Params
            pathParams.forEach(builder::addPathParam);

            // Form Params
            if (!formParams.isEmpty()) {
                formParams.forEach(builder::addFormParam);
            }

            // Body
            if (body != null) {
                builder.setBody(body);
            }

            // Authentication
            if (authConfig != null) {
                authConfig.apply(builder);
            }

            // Timeout
            builder.setConfig(RestAssuredConfig.config().httpClient(
                    io.restassured.config.HttpClientConfig.httpClientConfig().setParam("http.connection.timeout", timeout * 1000).setParam("http.socket.timeout", timeout * 1000)));

            // Logging
            if (enableLogging && client.logRequests) {
                builder.addFilter(new RequestLoggingFilter(LogDetail.ALL));
            }

            if (enableLogging && client.logResponses) {
                builder.addFilter(new ResponseLoggingFilter(LogDetail.ALL));
            }

            return builder.build();
        }

        private String getFullPath() {
            return path;
        }
    }

    /**
     * API Response Wrapper
     */
    public static class ApiResponse {
        private final Response response;
        private final ObjectMapper objectMapper;

        private ApiResponse(Response response, ObjectMapper objectMapper) {
            this.response = response;
            this.objectMapper = objectMapper;
        }

        public int getStatusCode() {
            return response.getStatusCode();
        }

        public String getStatusLine() {
            return response.getStatusLine();
        }

        public Headers getHeaders() {
            return response.getHeaders();
        }

        public String getHeader(String name) {
            return response.getHeader(name);
        }

        public String getBody() {
            return response.getBody().asString();
        }

        public JsonPath jsonPath() {
            return response.jsonPath();
        }

        public <T> T as(Class<T> clazz) {
            return response.as(clazz);
        }

        public <T> T extract(String path, Class<T> clazz) {
            return jsonPath().getObject(path, clazz);
        }

        public <T> List<T> extractList(String path, Class<T> clazz) {
            return jsonPath().getList(path, clazz);
        }

        public String extractString(String path) {
            return jsonPath().getString(path);
        }

        public Integer extractInt(String path) {
            return jsonPath().getInt(path);
        }

        public Long extractLong(String path) {
            return jsonPath().getLong(path);
        }

        public Boolean extractBoolean(String path) {
            return jsonPath().getBoolean(path);
        }

        public long getResponseTime() {
            return response.getTime();
        }

        public long getResponseTimeInMillis() {
            return response.getTimeIn(TimeUnit.MILLISECONDS);
        }

        public boolean isSuccess() {
            return getStatusCode() >= 200 && getStatusCode() < 300;
        }

        public ApiResponse assertStatusCode(int expectedCode) {
            if (getStatusCode() != expectedCode) {
                throw new ApiAssertionException(String.format("Expected status code %d but got %d. Response: %s", expectedCode, getStatusCode(), getBody()));
            }
            return this;
        }

        public ApiResponse assertSuccess() {
            if (!isSuccess()) {
                throw new ApiAssertionException(String.format("Expected success status (2xx) but got %d. Response: %s", getStatusCode(), getBody()));
            }
            return this;
        }

        public ApiResponse assertHeader(String headerName, String expectedValue) {
            String actualValue = getHeader(headerName);
            if (!expectedValue.equals(actualValue)) {
                throw new ApiAssertionException(String.format("Header '%s' expected '%s' but got '%s'", headerName, expectedValue, actualValue));
            }
            return this;
        }

        public ApiResponse assertJsonPath(String path, Object expectedValue) {
            Object actualValue = jsonPath().get(path);
            if (!Objects.equals(expectedValue, actualValue)) {
                throw new ApiAssertionException(String.format("JsonPath '%s' expected '%s' but got '%s'", path, expectedValue, actualValue));
            }
            return this;
        }

        public ApiResponse assertResponseTime(long maxMillis) {
            long actualTime = getResponseTimeInMillis();
            if (actualTime > maxMillis) {
                throw new ApiAssertionException(String.format("Response time %dms exceeded maximum %dms", actualTime, maxMillis));
            }
            return this;
        }

        public ApiResponse logResponse() {
            log.info("Response Status: {}", getStatusCode());
            log.info("Response Time: {}ms", getResponseTime());
            log.info("Response Body: {}", getBody());
            return this;
        }

        public Response getRawResponse() {
            return response;
        }
    }

    /**
     * Authentication Configuration
     */
    private static class AuthConfig {
        private final AuthType type;
        private final String[] credentials;

        public AuthConfig(AuthType type, String... credentials) {
            this.type = type;
            this.credentials = credentials;
        }

        public void apply(RequestSpecBuilder builder) {
            switch (type) {
                case BASIC -> builder.setAuth(RestAssured.basic(credentials[0], credentials[1]));
                case BEARER -> builder.addHeader("Authorization", "Bearer " + credentials[0]);
                case OAUTH2 -> builder.setAuth(RestAssured.oauth2(credentials[0]));
                case API_KEY -> builder.addHeader(credentials[0], credentials[1]);
            }
        }
    }

    /**
     * Authentication Types
     */
    private enum AuthType {BASIC, BEARER, OAUTH2, API_KEY}

    /**
     * File Upload Builder
     */
    public static class FileUploadBuilder {
        private final ApiRequestBuilder requestBuilder;
        private final Map<String, File> files = new HashMap<>();
        private final Map<String, String> formData = new HashMap<>();

        public FileUploadBuilder(ApiRequestBuilder requestBuilder) {
            this.requestBuilder = requestBuilder;
        }

        public FileUploadBuilder addFile(String paramName, File file) {
            files.put(paramName, file);
            return this;
        }

        public FileUploadBuilder addFile(String paramName, String filePath) {
            files.put(paramName, new File(filePath));
            return this;
        }

        public FileUploadBuilder addFormField(String name, String value) {
            formData.put(name, value);
            return this;
        }

        public ApiResponse upload() {
            RequestSpecification spec = new RequestSpecBuilder().build();
            files.forEach((name, file) -> spec.multiPart(name, file));
            formData.forEach(spec::formParam);
            Response response = spec.post(requestBuilder.getFullPath());
            return new ApiResponse(response, requestBuilder.client.objectMapper);
        }
    }

    /**
     * Request/Response Interceptor
     */
    public interface ApiInterceptor {
        void beforeRequest(RequestSpecification request);

        void afterResponse(Response response);
    }

    /**
     * Custom Exception Classes
     */
    public static class ApiRequestException extends RuntimeException {
        public ApiRequestException(String message) {
            super(message);
        }

        public ApiRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ApiSerializationException extends RuntimeException {
        public ApiSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ApiAssertionException extends AssertionError {
        public ApiAssertionException(String message) {
            super(message);
        }
    }

    /**
     * API Response Schema Validator
     */
    public static class SchemaValidator {
        public static void validateJson(ApiResponse response, String schemaPath) {
            try {
                String schema = Files.readString(new File(schemaPath).toPath());
                response.getRawResponse().then().assertThat().body(io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema(schema));
            } catch (IOException e) {
                throw new ApiRequestException("Failed to load schema from: " + schemaPath, e);
            }
        }

        public static void validateJson(ApiResponse response, File schemaFile) {
            response.getRawResponse().then().assertThat().body(io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema(schemaFile));
        }
    }

    /**
     * Request Template Builder for reusable configurations
     */
    public static class RequestTemplate {
        private final Map<String, Object> headers = new HashMap<>();
        private final Map<String, Object> queryParams = new HashMap<>();
        private String baseUri;
        private String basePath;
        private ContentType contentType = ContentType.JSON;
        private AuthConfig authConfig;

        public RequestTemplate baseUri(String baseUri) {
            this.baseUri = baseUri;
            return this;
        }

        public RequestTemplate basePath(String basePath) {
            this.basePath = basePath;
            return this;
        }

        public RequestTemplate header(String name, Object value) {
            headers.put(name, value);
            return this;
        }

        public RequestTemplate queryParam(String name, Object value) {
            queryParams.put(name, value);
            return this;
        }

        public RequestTemplate contentType(ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        public RequestTemplate bearerToken(String token) {
            this.authConfig = new AuthConfig(AuthType.BEARER, token);
            return this;
        }

        public ApiRequestBuilder apply(ApiRequestBuilder builder) {
            if (baseUri != null) builder.baseUri(baseUri);
            if (basePath != null) builder.basePath(basePath);
            builder.contentType(contentType);
            builder.headers(headers);
            builder.queryParams(queryParams);
            if (authConfig != null) { // Apply auth based on type
            }
            return builder;
        }
    }
}