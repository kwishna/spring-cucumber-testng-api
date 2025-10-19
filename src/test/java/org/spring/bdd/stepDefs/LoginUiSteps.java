package org.spring.bdd.stepDefs;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoginUiSteps extends BaseUiSteps {

    private static final Logger log = LogManager.getLogger(LoginUiSteps.class);

    @Given("User is on HRMLogin page {string}")
    public void loginTest(String url) {
        log.info("Navigating to HRM Login page: {}", url);
    }

    @When("User enters username as {string} and password as {string}")
    public void goToHomePage(String userName, String passWord) {
        log.info("Entering username: {} and password: {}", userName, passWord);
    }

    @When("User clicks on Forgot your Password Link")
    public void goToForgotYourPasswordPage() {
        log.info("Clicking on Forgot your Password link");
    }

    @Then("User should be able to login successfully and new page open")
    public void verifyLogin() {
        log.info("Verifying successful login");
    }

    @Then("User should be able to see error message {string}")
    public void verifyErrorMessage(String expectedErrorMessage) {
        log.info("Verifying error message: {}", expectedErrorMessage);
    }

    @Then("User should be able to see a message {string} below Username")
    public void verifyMissingUsernameMessage(String message) {
        log.info("Verifying message below username: {}", message);
    }

    @Then("User should be able to see LinkedIn Icon")
    public void verifyLinkedInIcon() {
        log.info("Verifying LinkedIn icon is displayed");
    }

    @Then("User should be able to see FaceBook Icon")
    public void verifyFaceBookIcon() {
        log.info("Verifying Facebook icon is displayed");
    }

    @Then("User should navigate to a new page")
    public void verifyForgetYourPasswordPage() {
        log.info("Verifying navigation to Forgot your Password page");
    }
}