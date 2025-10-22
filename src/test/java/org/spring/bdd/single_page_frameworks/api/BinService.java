package org.spring.bdd.single_page_frameworks.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Simple service to interact with httpbin (or other simple endpoints) using ApiClient
 */
@Component
public class BinService {
    private final ApiClient apiClient;

    @Autowired
    public BinService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Send a GET request to a full path (path should include leading slash) on provided baseUrl
     */
    public ApiClient.ApiResponse get(String baseUrl, String path) {
        return apiClient.newRequest(baseUrl)
                .path(path)
                .get();
    }
}
