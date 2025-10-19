package org.spring.bdd.pages;

import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component // 1. Make this Page Object a generic Spring component.
@Scope("prototype") // 2. IMPORTANT! We want a new instance of this page every time we ask for it.
public class LoginPage {

    private final WebDriver driver;

    @Autowired // 3. Spring automatically injects the singleton WebDriver bean we defined earlier.
    public LoginPage(WebDriver driver) {
        this.driver = driver;
    }

    public void navigate() {
        driver.get("https://example.com/login" );
    }

    // ... other methods for login actions and assertions
}
