package org.spring.bdd.configs;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.spring.bdd.envs.WebDriverManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.Duration;

@Configuration // 1. Marks this as a source of beans for tests only.
public class WebDriverConfig {

    @Bean(destroyMethod = "quit") // Spring will call driver.quit() at the end of the scope.
    @Scope("cucumber-glue")      // 1. The magic scope provided by cucumber-spring!
    public WebDriver webDriver(WebDriverManager manager) {
        return manager.getDriver();
    }

    @Value("${default.timeout:10}")
    private long defaultTimeout;

    @Bean
    @Scope("cucumber-glue")
    public WebDriverWait webDriverWait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
    }
}
