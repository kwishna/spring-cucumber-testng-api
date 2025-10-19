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

    // 1st Approach. DOES NOT WORK IN PARALLEL
//    @Bean(destroyMethod = "quit") // 2. Declares that this method produces a bean to be managed by Spring. The 'destroyMethod' attribute is an explicit way to tell Spring what to call.
//    @Scope("singleton") // 3. The magic! Ensures only ONE instance is created per TestContext.
//    public WebDriver webDriver() {
//        if ("chrome".equalsIgnoreCase(browser)) {
//             WebDriverManager.chromedriver().setup();
//            return new ChromeDriver();
//        } else {
//            throw new IllegalArgumentException("Unsupported browser type: " + browser);
//        }
//    }

    // 2nd Approach
//    @Bean
//    public static BeanFactoryPostProcessor beanFactoryPostProcessor() {
//        return beanFactory -> beanFactory.registerScope("thread", new ThreadScope());
//    }
//
//    @Bean
//    @Scope("thread") // Use our custom thread scope!
//    public WebDriver webDriver() {
//        return WebDriverManager.getDriver();
//    }

    // 3rd Approach
    @Bean(destroyMethod = "quit") // Spring will call driver.quit() at the end of the scope.
    @Scope("cucumber-glue")      // 1. The magic scope provided by cucumber-spring!
    public WebDriver webDriver(WebDriverManager manager) {
        return manager.getDriver();
    }

    @Value("${default.timeout:10}") // Get timeout from properties file, default to 10
    private long defaultTimeout;

    @Bean
    @Scope("cucumber-glue")
    public WebDriverWait webDriverWait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
    }
}

//class ThreadScope extends SimpleThreadScope {
//    // You can extend it if needed, but the base class often works perfectly.
//}

