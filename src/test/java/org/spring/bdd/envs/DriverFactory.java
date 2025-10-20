package org.spring.bdd.envs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Refactored DriverFactory with:
 * - ThreadLocal WebDriver for parallel execution
 * - Safer headless handling
 * - Configurable download directory
 * - Custom DriverInitializationException
 * - Remote capabilities merge (JSON)
 * - Improved logging of session capabilities
 */
@Component
public class DriverFactory {

    private static final Logger log = LogManager.getLogger(DriverFactory.class);

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();
    private static final Set<String> SUPPORTED_BROWSERS = Set.of("chrome", "firefox", "edge", "safari");

    @Autowired
    private Environment env;

    @Value("${browser:chrome}")
    private String defaultBrowser;

    @Value("${headless:false}")
    private boolean headless;

    @Value("${remote.execution:false}")
    private boolean remoteExecution;

    @Value("${remote.url:http://localhost:4444/wd/hub}")
    private String remoteUrl;

    @Value("${remote.platform:}")
    private String remotePlatform;

    @Value("${remote.browser.version:}")
    private String remoteBrowserVersion;

    @Value("${grid.node.timeout:300}")
    private int gridNodeTimeout;

    @Value("${grid.session.timeout:300}")
    private int gridSessionTimeout;

    @Value("${window.width:1920}")
    private int windowWidth;

    @Value("${window.height:1080}")
    private int windowHeight;

    @Value("${implicit.wait:10s}")
    private Duration implicitWait;

    @Value("${page.load.timeout:30s}")
    private Duration pageLoadTimeout;

    // Optional JSON to inject extra remote capabilities (useful for BrowserStack / LambdaTest)
    @Value("${remote.capabilities:}")
    private String remoteCapabilitiesJson;

    // Configurable download directory
    @Value("${download.dir:}")
    private String configuredDownloadDir;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Create or return the thread-local WebDriver.
     * If a driver already exists for the current thread this will return it (avoid recreating)
     */
    public WebDriver createWebDriver(String browser) {
        WebDriver existing = DRIVER.get();
        if (existing != null) {
            log.debug("Returning existing WebDriver for thread");
            return existing;
        }

        String browserType = (browser == null || browser.isEmpty()) ? defaultBrowser : browser.toLowerCase();
        if (!SUPPORTED_BROWSERS.contains(browserType)) {
            log.warn("Unsupported browser '{}' - defaulting to chrome", browserType);
            browserType = "chrome";
        }

        log.info("Creating WebDriver for browser='{}' remote='{}' (thread={})", browserType, remoteExecution, Thread.currentThread().getName());

        try {
            WebDriver driver;
            if (remoteExecution) {
                driver = createRemoteDriver(browserType);
            } else {
                driver = createLocalDriver(browserType);
            }

            configureDriver(driver);
            DRIVER.set(driver);

            // Log session capabilities for remote drivers
            if (driver instanceof RemoteWebDriver) {
                Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
                log.info("Remote session capabilities: {}", caps);
            }

            log.info("WebDriver created successfully: {}", driver.getClass().getSimpleName());
            return driver;

        } catch (Exception e) {
            log.error("Failed to create WebDriver for browser={}", browserType, e);
            throw new DriverInitializationException("WebDriver initialization failed for: " + browserType, e);
        }
    }

    /**
     * Returns the WebDriver for the current thread (may be null)
     */
    public static WebDriver getDriver() {
        return DRIVER.get();
    }

    /**
     * Quit and remove the WebDriver for the current thread
     */
    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                // Log and continue
                LogManager.getLogger(DriverFactory.class).warn("Error quitting WebDriver", e);
            } finally {
                DRIVER.remove();
            }
        }
    }

    private WebDriver createLocalDriver(String browser) {
        return switch (browser.toLowerCase()) {
            case "chrome" -> createChromeDriver();
            case "firefox" -> createFirefoxDriver();
            case "edge" -> createEdgeDriver();
            case "safari" -> createSafariDriver();
            default -> createChromeDriver();
        };
    }

    private WebDriver createRemoteDriver(String browser) throws MalformedURLException {
        log.info("Creating RemoteWebDriver - URL: {}, Browser: {}, Platform: {}", remoteUrl, browser, remotePlatform);

        MutableCapabilities caps = switch (browser.toLowerCase()) {
            case "chrome" -> getChromeOptionsForGrid();
            case "firefox" -> getFirefoxOptionsForGrid();
            case "edge" -> getEdgeOptionsForGrid();
            case "safari" -> getSafariOptions();
            default -> getChromeOptionsForGrid();
        };

        // Merge remote capabilities passed as JSON (optional)
        mergeRemoteCapabilities(caps);

        RemoteWebDriver remote = new RemoteWebDriver(new URL(remoteUrl), caps);
        if (remote.getSessionId() != null) {
            log.info("RemoteWebDriver session created. Session ID: {}", remote.getSessionId());
        }
        return remote;
    }

    private WebDriver createChromeDriver() {
        WebDriverManager.chromedriver().setup();
        return new ChromeDriver(getChromeOptions());
    }

    private WebDriver createFirefoxDriver() {
        WebDriverManager.firefoxdriver().setup();
        return new FirefoxDriver(getFirefoxOptions());
    }

    private WebDriver createEdgeDriver() {
        WebDriverManager.edgedriver().setup();
        return new EdgeDriver(getEdgeOptions());
    }

    private WebDriver createSafariDriver() {
        return new SafariDriver(getSafariOptions());
    }

    private ChromeOptions getChromeOptions() {
        ChromeOptions options = new ChromeOptions();

        options.addArguments("--remote-allow-origins=*",
                "--disable-dev-shm-usage",
                "--no-sandbox",
                "--disable-gpu",
                "--disable-extensions",
                "--disable-infobars",
                "--disable-notifications",
                "--disable-popup-blocking",
                String.format("--window-size=%d,%d", windowWidth, windowHeight)
        );

        if (headless || isCI()) {
            // Use new headless mode for modern Chrome
            options.addArguments("--headless=new");
            log.info("Running Chrome in headless mode");
        }

        String downloadDir = determineDownloadDir();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir);
        prefs.put("download.prompt_for_download", false);
        prefs.put("profile.default_content_settings.popups", 0);
        options.setExperimentalOption("prefs", prefs);

        options.setAcceptInsecureCerts(true);
        log.debug("Chrome options configured: headless={}, window={}x{}, downloadDir={}", headless, windowWidth, windowHeight, downloadDir);
        return options;
    }

    private FirefoxOptions getFirefoxOptions() {
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments(String.format("--width=%d", windowWidth), String.format("--height=%d", windowHeight));

        if (headless || isCI()) {
            options.addArguments("--headless");
            log.info("Running Firefox in headless mode");
        }

        options.setAcceptInsecureCerts(true);
        log.debug("Firefox options configured");
        return options;
    }

    private EdgeOptions getEdgeOptions() {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        options.addArguments(String.format("--window-size=%d,%d", windowWidth, windowHeight));

        if (headless || isCI()) {
            options.addArguments("--headless");
            log.info("Running Edge in headless mode");
        }

        options.setAcceptInsecureCerts(true);
        log.debug("Edge options configured");
        return options;
    }

    private SafariOptions getSafariOptions() {
        SafariOptions options = new SafariOptions();
        options.setAutomaticInspection(false);
        log.debug("Safari options configured");
        return options;
    }

    private ChromeOptions getChromeOptionsForGrid() {
        ChromeOptions options = getChromeOptions();
        applyGridSettings(options);
        log.debug("Chrome options configured for Grid execution");
        return options;
    }

    private FirefoxOptions getFirefoxOptionsForGrid() {
        FirefoxOptions options = getFirefoxOptions();
        applyGridSettings(options);
        log.debug("Firefox options configured for Grid execution");
        return options;
    }

    private EdgeOptions getEdgeOptionsForGrid() {
        EdgeOptions options = getEdgeOptions();
        applyGridSettings(options);
        log.debug("Edge options configured for Grid execution");
        return options;
    }

    private void applyGridSettings(MutableCapabilities options) {
        if (remotePlatform != null && !remotePlatform.isBlank()) {
            options.setCapability(CapabilityType.PLATFORM_NAME, remotePlatform);
            log.debug("Platform set to: {}", remotePlatform);
        }
        if (remoteBrowserVersion != null && !remoteBrowserVersion.isBlank()) {
            options.setCapability(CapabilityType.BROWSER_VERSION, remoteBrowserVersion);
            log.debug("Browser version set to: {}", remoteBrowserVersion);
        }

        options.setCapability("se:recordVideo", false);
        options.setCapability("se:timeZone", "UTC");
        options.setCapability("se:screenResolution", String.format("%dx%d", windowWidth, windowHeight));
    }

    private void configureDriver(WebDriver driver) {
        try {
            driver.manage().timeouts().implicitlyWait(implicitWait);
            driver.manage().timeouts().pageLoadTimeout(pageLoadTimeout);

            // Avoid maximize in headless / CI
            if (!(headless || isCI())) {
                driver.manage().window().maximize();
            } else {
                // Some drivers may not support maximize in headless; ensure size set for consistency
                try {
                    driver.manage().window().setSize(new org.openqa.selenium.Dimension(windowWidth, windowHeight));
                } catch (Exception ignored) {
                }
            }

            log.debug("Driver configured with timeouts: implicit={}s, pageLoad={}s", implicitWait.getSeconds(), pageLoadTimeout.getSeconds());
        } catch (Exception e) {
            log.warn("Unable to fully configure driver window/timeouts", e);
        }
    }

    private boolean isCI() {
        return System.getenv("CI") != null
                || System.getenv("JENKINS_HOME") != null
                || System.getenv("GITHUB_ACTIONS") != null
                || System.getenv("GITLAB_CI") != null
                || System.getenv("CIRCLECI") != null;
    }

    private String determineDownloadDir() {
        if (configuredDownloadDir != null && !configuredDownloadDir.isBlank()) {
            return configuredDownloadDir;
        }
        // Fallback to a temp-directory safe for CI environments
        return System.getProperty("download.dir", System.getProperty("java.io.tmpdir"));
    }

    private void mergeRemoteCapabilities(MutableCapabilities caps) {
        if (remoteCapabilitiesJson == null || remoteCapabilitiesJson.isBlank()) {
            return;
        }
        try {
            Map<String, Object> additional = mapper.readValue(remoteCapabilitiesJson, Map.class);
            additional.forEach(caps::setCapability);
            log.debug("Merged remote capabilities: {}", additional.keySet());
        } catch (Exception e) {
            log.warn("Invalid remote.capabilities JSON - ignoring. Value={}", remoteCapabilitiesJson, e);
        }
    }

    public String getBrowserFromEnvironment() {
        String browser = System.getProperty("browser");
        if (browser == null || browser.isEmpty()) {
            browser = env.getProperty("browser", defaultBrowser);
        }
        return browser.toLowerCase();
    }

    /**
     * Custom runtime exception for driver init failures
     */
    public static class DriverInitializationException extends RuntimeException {
        public DriverInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
