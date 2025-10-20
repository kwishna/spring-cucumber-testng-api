package org.spring.bdd.configs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.spring.bdd.envs.DriverFactory;
import org.spring.bdd.envs.WebDriverManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.Duration;

@Configuration // 1. Marks this as a source of beans for tests only.
public class WebDriverConfig {

    private static final Logger log = LogManager.getLogger(WebDriverConfig.class);

    @Value("${default.timeout:10}")
    private long defaultTimeout;

    @Value("${browser:chrome}")
    private String browser;

    @Bean(destroyMethod = "quit") // Spring will call driver.quit() at the end of the scope.
    @Scope("cucumber-glue")      // 1. The magic scope provided by cucumber-spring!
    public WebDriver webDriver(DriverFactory factory) {
        log.info("Creating WebDriver bean for browser: {}", browser);
        WebDriver driver = factory.createWebDriver(browser);
        log.info("WebDriver bean created for browser: {}", browser);
        return driver;
    }

    @Bean
    @Scope("cucumber-glue")
    public WebDriverWait webDriverWait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
    }
}
