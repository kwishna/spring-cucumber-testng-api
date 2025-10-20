package org.spring.bdd.envs;

import io.github.cdimascio.dotenv.Dotenv;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EnvManager {
    static Logger log = LogManager.getLogger();

    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().systemProperties().load();
    private static final String DEFAULT_ENV = "dev";

    @Autowired
    private Environment env;

    public static String getEnv(String name) {
        String val = dotenv.get(name, "");
        if (val.isEmpty()) {
            log.error(String.format("Missing property: {}", name));
            return "chrome";
        }
        return val;
    }

    public String getBaseUrl() {
        return env.getProperty("baseUrl");
    }

    public String getApiBaseUrl() {
        return Optional.ofNullable(System.getenv("API_BASE_URL"))
                .orElseGet(() -> env.getProperty("api.base.url", "https://api.example.com"));
    }
}
