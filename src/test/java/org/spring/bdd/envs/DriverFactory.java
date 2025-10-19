package org.spring.bdd.envs;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class DriverFactory {

    public WebDriver createWebDriver(String browser) {
        WebDriver driver = null;

        if (browser.equals("chrome")) {
            String driverPath = "./lib/chromedriver";

            if (EnvManager.getEnv("browser").equals("chrome")) {
                driverPath = "./lib/chromedriver";
            }

            System.setProperty("webdriver.chrome.driver", driverPath);
            ChromeOptions ops = new ChromeOptions();
            ops.addArguments("--headless");
            ops.addArguments("window-size=1920,1080");
            ops.addArguments("--remote-allow-origin=*");
            ops.addArguments("--window-position=-1,-1");

            driver = new ChromeDriver(ops);
        }
        return driver;
    }
}
