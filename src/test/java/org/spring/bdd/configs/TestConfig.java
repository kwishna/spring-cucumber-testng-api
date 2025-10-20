package org.spring.bdd.configs;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = {
        "org.spring.bdd"
})
//@Import({WebDriverConfig.class})
public class TestConfig {
}


