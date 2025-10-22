package org.spring.bdd.stepDefs;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.spring.bdd.single_page_frameworks.api.BinService;
import org.spring.bdd.single_page_frameworks.api.ApiClient;

import static org.testng.Assert.assertEquals;

@Component
@Scope('cucumber-glue')
public class UserApiSteps extends BaseApiSteps {

    private static final Logger log = LogManager.getLogger(UserApiSteps.class);
    
    @Autowired
    private BinService binService;

    private String currentBaseUrl;

    @Given("the API base URL is {string}")
    public void iSetBaseURL(String baseUrl) {
        log.info("--- Setting base URL to: {} ---", baseUrl);
        this.currentBaseUrl = baseUrl;
    }

    @When("I send a GET request to {string}")
    public void iSendGetRequest(String endpoint) {
        log.info("--- Sending GET request to: {}{} ---", baseUrl, endpoint);
        response = binService.get(currentBaseUrl, endpoint);
    }

    @Then("the response status code should be {int}")
    public void iVerifyStatusCode(Integer expectedStatus) {
        log.info("--- Verifying status code. Expected: {}, Actual: {} ---", expectedStatus, response.getStatusCode());
        assertEquals(response.getStatusCode(), expectedStatus.intValue(), "Unexpected status code");
    }
}