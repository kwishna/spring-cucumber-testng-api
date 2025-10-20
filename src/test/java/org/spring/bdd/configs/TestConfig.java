package org.spring.bdd.configs;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

// Defines what to Scan
@Configuration
@ComponentScan(basePackages = { // Tells what to Scan. StepDefs, Hooks etc
        "org.spring.bdd"
})
//@Import({WebDriverConfig.class})
public class TestConfig {
    // This class contains only Spring related configurations.
}


