package org.spring.bdd.stepDefs;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.assertEquals;

public class UserApiSteps extends BaseApiSteps {

    private static final Logger log = LogManager.getLogger(UserApiSteps.class);

    @Given("the API base URL is {string}")
    public void setBaseURL(String url) {
        log.info("--- Setting base URL to: {} ---", url);
        this.baseURL = url;
    }

    @When("I send a GET request to {string}")
    public void sendGetRequest(String endpoint) {
        log.info("--- Sending GET request to: {}{} ---", baseURL, endpoint);
        response = given().baseUri(baseURL).get(endpoint);
    }

    @Then("the response status code should be {int}")
    public void verifyStatusCode(int expectedStatusCode) {
        log.info("--- Verifying status code. Expected: {}, Actual: {} ---", expectedStatusCode, response.getStatusCode());
        assertEquals(response.getStatusCode(), expectedStatusCode);
    }
}
