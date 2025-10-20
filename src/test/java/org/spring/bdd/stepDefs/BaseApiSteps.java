package org.spring.bdd.stepDefs;

import org.spring.bdd.single_page_frameworks.api.ApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("cucumber-glue")
public abstract class BaseApiSteps extends BaseSteps{
    @Autowired
    protected ApiClient apiClient;

    @Value("${API_BASE_URL}")
    protected String baseUrl;

    protected ApiClient.ApiResponse response;

    protected ApiClient.ApiRequestBuilder request() {
        return apiClient.newRequest();
    }

    protected ApiClient.ApiRequestBuilder request(String baseUrl) {
        return apiClient.newRequest(baseUrl);
    }
}