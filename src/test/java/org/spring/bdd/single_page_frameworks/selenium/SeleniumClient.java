package org.spring.bdd.single_page_frameworks.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.openqa.selenium.support.ui.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Production-grade Selenium WebDriver Framework
 * 
 * Features:
 * - Multi-browser support (Chrome, Firefox, Edge, Safari)
 * - Thread-safe parallel execution
 * - Fluent API design
 * - Smart wait mechanisms
 * - Element interaction utilities
 * - JavaScript execution
 * - Screenshot capabilities
 * - Cookie management
 * - Window/Tab handling
 * - Alert handling
 * - Frame switching
 * - Advanced actions (drag-drop, hover, etc.)
 * - Select dropdown handling
 * - File upload/download
 * 
 * Usage:
 * <pre>
 * WebDriver driver = seleniumClient.chrome()
 *     .headless(false)
 *     .maximize()
 *     .build();
 * 
 * seleniumClient.element(driver)
 *     .byId("submit")
 *     .click();
 * 
 * seleniumClient.waits(driver)
 *     .forElement(By.id("result"))
 *     .toBeVisible();
 * </pre>
 */
@Component
public class SeleniumClient {

    private static final Logger log = LogManager.getLogger(SeleniumClient.class);
    
    // Thread-local storage for parallel execution
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static final Map<Long, WebDriver> threadDrivers = new ConcurrentHashMap<>();

    @Value("${selenium.implicit.wait:10}")
    private int defaultImplicitWait;

    @Value("${selenium.explicit.wait:20}")
    private int defaultExplicitWait;

    @Value("${selenium.page.load.timeout:30}")
    private int defaultPageLoadTimeout;

    @Value("${selenium.screenshot.dir:./screenshots}")
    private String screenshotDir;

    @Value("${selenium.download.dir:./downloads}")
    private String downloadDir;

    /**
     * Creates Chrome driver builder
     */
    public ChromeDriverBuilder chrome() {
        return new ChromeDriverBuilder(this);
    }

    /**
     * Creates Firefox driver builder
     */
    public FirefoxDriverBuilder firefox() {
        return new FirefoxDriverBuilder(this);
    }

    /**
     * Creates Edge driver builder
     */
    public EdgeDriverBuilder edge() {
        return new EdgeDriverBuilder(this);
    }

    /**
     * Creates Safari driver builder
     */
    public SafariDriverBuilder safari() {
        return new SafariDriverBuilder(this);
    }

    /**
     * Gets current thread's driver
     */
    public WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    /**
     * Sets driver for current thread
     */
    private void setDriver(WebDriver driver) {
        driverThreadLocal.set(driver);
        threadDrivers.put(Thread.currentThread().getId(), driver);
    }

    /**
     * Creates element finder
     */
    public ElementFinder element(WebDriver driver) {
        return new ElementFinder(driver, defaultExplicitWait);
    }

    /**
     * Creates wait handler
     */
    public WaitHandler waits(WebDriver driver) {
        return new WaitHandler(driver, defaultExplicitWait);
    }

    /**
     * Creates navigation handler
     */
    public NavigationHandler navigate(WebDriver driver) {
        return new NavigationHandler(driver);
    }

    /**
     * Creates JavaScript executor
     */
    public JavaScriptHandler javascript(WebDriver driver) {
        return new JavaScriptHandler(driver);
    }

    /**
     * Creates actions handler
     */
    public ActionsHandler actions(WebDriver driver) {
        return new ActionsHandler(driver);
    }

    /**
     * Creates window handler
     */
    public WindowHandler window(WebDriver driver) {
        return new WindowHandler(driver);
    }

    /**
     * Creates alert handler
     */
    public AlertHandler alert(WebDriver driver) {
        return new AlertHandler(driver);
    }

    /**
     * Creates frame handler
     */
    public FrameHandler frame(WebDriver driver) {
        return new FrameHandler(driver);
    }

    /**
     * Creates cookie handler
     */
    public CookieHandler cookies(WebDriver driver) {
        return new CookieHandler(driver);
    }

    /**
     * Creates screenshot handler
     */
    public ScreenshotHandler screenshot(WebDriver driver) {
        return new ScreenshotHandler(driver, screenshotDir);
    }

    /**
     * Cleanup driver for current thread
     */
    public void cleanup() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
                log.info("Driver closed for thread: {}", Thread.currentThread().getName());
            } catch (Exception e) {
                log.warn("Error closing driver", e);
            } finally {
                driverThreadLocal.remove();
                threadDrivers.remove(Thread.currentThread().getId());
            }
        }
    }

    /**
     * Cleanup all drivers
     */
    public static void cleanupAll() {
        threadDrivers.values().forEach(driver -> {
            try {
                driver.quit();
            } catch (Exception e) {
                log.warn("Error closing driver", e);
            }
        });
        threadDrivers.clear();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Chrome Driver Builder
    // ═══════════════════════════════════════════════════════════════════════════

    public class ChromeDriverBuilder {
        private final SeleniumClient client;
        private final ChromeOptions options = new ChromeOptions();
        private boolean maximize = false;
        private Dimension dimension;

        private ChromeDriverBuilder(SeleniumClient client) {
            this.client = client;
            options.addArguments("--remote-allow-origins=*");
        }

        public ChromeDriverBuilder headless() {
            return headless(true);
        }

        public ChromeDriverBuilder headless(boolean headless) {
            if (headless) {
                options.addArguments("--headless=new");
            }
            return this;
        }

        public ChromeDriverBuilder maximize() {
            this.maximize = true;
            return this;
        }

        public ChromeDriverBuilder windowSize(int width, int height) {
            this.dimension = new Dimension(width, height);
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            return this;
        }

        public ChromeDriverBuilder incognito() {
            options.addArguments("--incognito");
            return this;
        }

        public ChromeDriverBuilder disableNotifications() {
            options.addArguments("--disable-notifications");
            return this;
        }

        public ChromeDriverBuilder disablePopups() {
            options.addArguments("--disable-popup-blocking");
            return this;
        }

        public ChromeDriverBuilder addArgument(String argument) {
            options.addArguments(argument);
            return this;
        }

        public ChromeDriverBuilder addExtension(File extension) {
            options.addExtensions(extension);
            return this;
        }

        public ChromeDriverBuilder setDownloadPath(String path) {
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", path);
            prefs.put("download.prompt_for_download", false);
            options.setExperimentalOption("prefs", prefs);
            return this;
        }

        public ChromeDriverBuilder acceptInsecureCerts() {
            options.setAcceptInsecureCerts(true);
            return this;
        }

        public ChromeDriverBuilder setUserAgent(String userAgent) {
            options.addArguments("user-agent=" + userAgent);
            return this;
        }

        public WebDriver build() {
            WebDriverManager.chromedriver().setup();
            WebDriver driver = new ChromeDriver(options);
            configureDriver(driver);
            client.setDriver(driver);
            log.info("Chrome driver created for thread: {}", Thread.currentThread().getName());
            return driver;
        }

        private void configureDriver(WebDriver driver) {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(client.defaultImplicitWait));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(client.defaultPageLoadTimeout));
            
            if (maximize) {
                driver.manage().window().maximize();
            } else if (dimension != null) {
                driver.manage().window().setSize(dimension);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Firefox Driver Builder
    // ═══════════════════════════════════════════════════════════════════════════

    public class FirefoxDriverBuilder {
        private final SeleniumClient client;
        private final FirefoxOptions options = new FirefoxOptions();
        private boolean maximize = false;
        private Dimension dimension;

        private FirefoxDriverBuilder(SeleniumClient client) {
            this.client = client;
        }

        public FirefoxDriverBuilder headless() {
            return headless(true);
        }

        public FirefoxDriverBuilder headless(boolean headless) {
            if (headless) {
                options.addArguments("--headless");
            }
            return this;
        }

        public FirefoxDriverBuilder maximize() {
            this.maximize = true;
            return this;
        }

        public FirefoxDriverBuilder windowSize(int width, int height) {
            this.dimension = new Dimension(width, height);
            return this;
        }

        public FirefoxDriverBuilder privateBrowsing() {
            options.addArguments("-private");
            return this;
        }

        public FirefoxDriverBuilder addArgument(String argument) {
            options.addArguments(argument);
            return this;
        }

        public FirefoxDriverBuilder acceptInsecureCerts() {
            options.setAcceptInsecureCerts(true);
            return this;
        }

        public WebDriver build() {
            WebDriverManager.firefoxdriver().setup();
            WebDriver driver = new FirefoxDriver(options);
            configureDriver(driver);
            client.setDriver(driver);
            log.info("Firefox driver created for thread: {}", Thread.currentThread().getName());
            return driver;
        }

        private void configureDriver(WebDriver driver) {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(client.defaultImplicitWait));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(client.defaultPageLoadTimeout));
            
            if (maximize) {
                driver.manage().window().maximize();
            } else if (dimension != null) {
                driver.manage().window().setSize(dimension);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Edge Driver Builder
    // ═══════════════════════════════════════════════════════════════════════════

    public class EdgeDriverBuilder {
        private final SeleniumClient client;
        private final EdgeOptions options = new EdgeOptions();
        private boolean maximize = false;
        private Dimension dimension;

        private EdgeDriverBuilder(SeleniumClient client) {
            this.client = client;
            options.addArguments("--remote-allow-origins=*");
        }

        public EdgeDriverBuilder headless() {
            return headless(true);
        }

        public EdgeDriverBuilder headless(boolean headless) {
            if (headless) {
                options.addArguments("--headless=new");
            }
            return this;
        }

        public EdgeDriverBuilder maximize() {
            this.maximize = true;
            return this;
        }

        public EdgeDriverBuilder windowSize(int width, int height) {
            this.dimension = new Dimension(width, height);
            return this;
        }

        public EdgeDriverBuilder inPrivate() {
            options.addArguments("--inprivate");
            return this;
        }

        public EdgeDriverBuilder addArgument(String argument) {
            options.addArguments(argument);
            return this;
        }

        public EdgeDriverBuilder acceptInsecureCerts() {
            options.setAcceptInsecureCerts(true);
            return this;
        }

        public WebDriver build() {
            WebDriverManager.edgedriver().setup();
            WebDriver driver = new EdgeDriver(options);
            configureDriver(driver);
            client.setDriver(driver);
            log.info("Edge driver created for thread: {}", Thread.currentThread().getName());
            return driver;
        }

        private void configureDriver(WebDriver driver) {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(client.defaultImplicitWait));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(client.defaultPageLoadTimeout));
            
            if (maximize) {
                driver.manage().window().maximize();
            } else if (dimension != null) {
                driver.manage().window().setSize(dimension);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Safari Driver Builder
    // ═══════════════════════════════════════════════════════════════════════════

    public class SafariDriverBuilder {
        private final SeleniumClient client;
        private final SafariOptions options = new SafariOptions();
        private boolean maximize = false;

        private SafariDriverBuilder(SeleniumClient client) {
            this.client = client;
        }

        public SafariDriverBuilder maximize() {
            this.maximize = true;
            return this;
        }

        public SafariDriverBuilder automaticInspection(boolean enable) {
            options.setAutomaticInspection(enable);
            return this;
        }

        public WebDriver build() {
            WebDriver driver = new SafariDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(client.defaultImplicitWait));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(client.defaultPageLoadTimeout));
            
            if (maximize) {
                driver.manage().window().maximize();
            }
            
            client.setDriver(driver);
            log.info("Safari driver created for thread: {}", Thread.currentThread().getName());
            return driver;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Element Finder
    // ═══════════════════════════════════════════════════════════════════════════

    public static class ElementFinder {
        private final WebDriver driver;
        private final WebDriverWait wait;
        private By locator;

        private ElementFinder(WebDriver driver, int waitSeconds) {
            this.driver = driver;
            this.wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
        }

        public ElementFinder byId(String id) {
            this.locator = By.id(id);
            return this;
        }

        public ElementFinder byName(String name) {
            this.locator = By.name(name);
            return this;
        }

        public ElementFinder byClassName(String className) {
            this.locator = By.className(className);
            return this;
        }

        public ElementFinder byTagName(String tagName) {
            this.locator = By.tagName(tagName);
            return this;
        }

        public ElementFinder byCss(String cssSelector) {
            this.locator = By.cssSelector(cssSelector);
            return this;
        }

        public ElementFinder byXPath(String xpath) {
            this.locator = By.xpath(xpath);
            return this;
        }

        public ElementFinder byLinkText(String linkText) {
            this.locator = By.linkText(linkText);
            return this;
        }

        public ElementFinder byPartialLinkText(String partialLinkText) {
            this.locator = By.partialLinkText(partialLinkText);
            return this;
        }

        public WebElement find() {
            log.debug("Finding element: {}", locator);
            return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        }

        public List<WebElement> findAll() {
            log.debug("Finding elements: {}", locator);
            wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            return driver.findElements(locator);
        }

        public WebElement findVisible() {
            log.debug("Finding visible element: {}", locator);
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        }

        public WebElement findClickable() {
            log.debug("Finding clickable element: {}", locator);
            return wait.until(ExpectedConditions.elementToBeClickable(locator));
        }

        public boolean exists() {
            try {
                driver.findElement(locator);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        public boolean isVisible() {
            try {
                return find().isDisplayed();
            } catch (Exception e) {
                return false;
            }
        }

        public ElementFinder click() {
            findClickable().click();
            log.info("Clicked element: {}", locator);
            return this;
        }

        public ElementFinder sendKeys(CharSequence... keys) {
            findVisible().sendKeys(keys);
            log.info("Sent keys to element: {}", locator);
            return this;
        }

        public ElementFinder clear() {
            findVisible().clear();
            log.info("Cleared element: {}", locator);
            return this;
        }

        public ElementFinder submit() {
            find().submit();
            log.info("Submitted form: {}", locator);
            return this;
        }

        public String getText() {
            String text = findVisible().getText();
            log.debug("Got text '{}' from element: {}", text, locator);
            return text;
        }

        public String getAttribute(String attribute) {
            String value = find().getAttribute(attribute);
            log.debug("Got attribute '{}' = '{}' from element: {}", attribute, value, locator);
            return value;
        }

        public String getCssValue(String property) {
            return find().getCssValue(property);
        }

        public boolean isEnabled() {
            return find().isEnabled();
        }

        public boolean isSelected() {
            return find().isSelected();
        }

        public ElementFinder scrollIntoView() {
            WebElement element = find();
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            log.info("Scrolled element into view: {}", locator);
            return this;
        }

        public ElementFinder highlight() {
            WebElement element = find();
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].style.border='3px solid red'", element);
            return this;
        }

        public Select asSelect() {
            return new Select(find());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Wait Handler
    // ═══════════════════════════════════════════════════════════════════════════

    public static class WaitHandler {
        private final WebDriver driver;
        private final WebDriverWait wait;
        private By locator;

        private WaitHandler(WebDriver driver, int waitSeconds) {
            this.driver = driver;
            this.wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
        }

        public WaitHandler forElement(By locator) {
            this.locator = locator;
            return this;
        }

        public WaitHandler toBePresent() {
            wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            return this;
        }

        public WaitHandler toBeVisible() {
            wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            return this;
        }

        public WaitHandler toBeInvisible() {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
            return this;
        }

        public WaitHandler toBeClickable() {
            wait.until(ExpectedConditions.elementToBeClickable(locator));
            return this;
        }

        public WaitHandler toBeSelected() {
            wait.until(ExpectedConditions.elementToBeSelected(locator));
            return this;
        }

        public WaitHandler forTitle(String title) {
            wait.until(ExpectedConditions.titleIs(title));
            return this;
        }

        public WaitHandler forTitleContains(String title) {
            wait.until(ExpectedConditions.titleContains(title));
            return this;
        }

        public WaitHandler forUrl(String url) {
            wait.until(ExpectedConditions.urlToBe(url));
            return this;
        }

        public WaitHandler forUrlContains(String url) {
            wait.until(ExpectedConditions.urlContains(url));
            return this;
        }

        public WaitHandler forTextToBePresent(String text) {
            wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
            return this;
        }

        public WaitHandler forAttributeValue(String attribute, String value) {
            wait.until(ExpectedConditions.attributeToBe(locator, attribute, value));
            return this;
        }

        public WaitHandler forAlertPresent() {
            wait.until(ExpectedConditions.alertIsPresent());
            return this;
        }

        public WaitHandler forNumberOfWindows(int number) {
            wait.until(ExpectedConditions.numberOfWindowsToBe(number));
            return this;
        }

        public <V> V until(Function<WebDriver, V> condition) {
            return wait.until(condition);
        }

        public FluentWait<WebDriver> fluent(Duration timeout, Duration pollingInterval) {
            return new FluentWait<>(driver)
                    .withTimeout(timeout)
                    .pollingEvery(pollingInterval)
                    .ignoring(Exception.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Navigation Handler
    // ═══════════════════════════════════════════════════════════════════════════

    public static class NavigationHandler {
        private final WebDriver driver;

        private NavigationHandler(WebDriver driver) {
            this.driver = driver;
        }

        public NavigationHandler to(String url) {
            driver.get(url);
            log.info("Navigated to: {}", url);
            return this;
        }

        public NavigationHandler back() {
            driver.navigate().back();
            log.info("Navigated back");
            return this;
        }

        public NavigationHandler forward() {
            driver.navigate().forward();
            log.info("Navigated forward");
            return this;
        }

        public NavigationHandler refresh() {
            driver.navigate().refresh();
            log.info("Page refreshed");
            return this;
        }

        public String getCurrentUrl() {
            return driver.getCurrentUrl();
        }

        public String getTitle() {
            return driver.getTitle();
        }

        public String getPageSource() {
            return driver.getPageSource();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // JavaScript Handler
    // ═══════════════════════════════════════════════════════════════════════════

    public static class JavaScriptHandler {
        private final JavascriptExecutor js;

        private JavaScriptHandler(WebDriver driver) {
            this.js = (JavascriptExecutor) driver;
        }

        public Object execute(String script, Object... args) {
            log.debug("Executing JavaScript: {}", script);
            return js.executeScript(script, args);
        }

        public Object executeAsync(String script, Object... args) {
            log.debug("Executing async JavaScript: {}", script);
            return js.executeAsyncScript(script, args);
        }

        public JavaScriptHandler click(WebElement element) {
            execute("arguments[0].click();", element);
            log.info("Clicked element via JavaScript");
            return this;
        }

        public JavaScriptHandler scrollToElement(WebElement element) {
            execute("arguments[0].scrollIntoView(true);", element);
            return this;
        }

        public JavaScriptHandler scrollToBottom() {
            execute("window.scrollTo(0, document.body.scrollHeight);");
            return this;
        }

        public JavaScriptHandler scrollToTop() {
            execute("window.scrollTo(0, 0);");
            return this;
        }

        public JavaScriptHandler highlight(WebElement element) {
            execute("arguments[0].style.border='3px solid red'", element);
            return this;
        }

        public JavaScriptHandler setValue(WebElement element, String value) {
            execute("arguments[0].value=arguments[1];", element, value);
            return this;
        }

        public String getInnerText(WebElement element) {
            return (String) execute("return arguments[0].innerText;", element);
        }

        public JavaScriptHandler openNewTab() {
            execute("window.open();");
            return this;
        }

        public Long getScrollHeight() {
            return (Long) execute("return document.body.scrollHeight;");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Actions Handler
    // ═══════════════════════════════════════════════════════════════════════════

    public static class ActionsHandler {
        private final Actions actions;

        private ActionsHandler(WebDriver driver) {
            this.actions = new Actions(driver);
        }

        public ActionsHandler moveToElement(WebElement element) {
            actions.moveToElement(element);
            return this;
        }

        public ActionsHandler click(WebElement element) {
            actions.click(element);
            return this;
        }

        public ActionsHandler doubleClick(WebElement element) {
            actions.doubleClick(element);
            return this;
        }

        public ActionsHandler contextClick(WebElement element) {
            actions.contextClick(element);
            return this;
        }

        public ActionsHandler dragAndDrop(WebElement source, WebElement target) {
            actions.dragAndDrop(source, target);
            log.info("Performed drag and drop");
            return this;
        }

        public ActionsHandler dragAndDropBy(WebElement source, int xOffset, int yOffset) {
            actions.dragAndDropBy(source, xOffset, yOffset);
            return this;
        }

        public ActionsHandler clickAndHold(WebElement element) {
            actions.clickAndHold(element);
            return this;
        }

        public ActionsHandler release() {
            actions.release();
            return this;
        }

        public ActionsHandler sendKeys(CharSequence... keys) {
            actions.sendKeys(keys);
            return this;
        }

        public ActionsHandler keyDown(Keys key) {
            actions.keyDown(key);
            return this;
        }

        public ActionsHandler keyUp(Keys key) {
            actions.keyUp(key);
            return this;
        }

        public void perform() {
            actions.perform();
            log.info("Performed actions");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Window Handler
    // ═══════════════════════════════════════════════════════════════════════════

    public static class WindowHandler {
        private final WebDriver driver;

        private WindowHandler(WebDriver driver) {
            this.driver = driver;
        }

        public WindowHandler maximize() {
            driver.manage().window().maximize();
            log.info("Window maximized");
            return this;
        }

        public WindowHandler minimize() {
            driver.manage().window().minimize();
            log.info("Window minimized");
            return this;
        }

        public WindowHandler fullscreen() {
            driver.manage().window().fullscreen();
            log.info("Window fullscreen");
            return this;
        }

        public WindowHandler setSize(int width, int height) {
            driver.manage().window().setSize(new Dimension(width, height));
            log.info("Window size set to: {}x{}", width, height);
            return this;
        }

        public WindowHandler setPosition(int x, int y) {
            driver.manage().window().setPosition(new Point(x, y));
            return this;
        }

        public Dimension getSize() {
            return driver.manage().window().getSize();
        }

        public Point getPosition() {
            return driver.manage().window().getPosition();
        }

        public WindowHandler switchToWindow(String windowHandle) {
            driver.switchTo().window(windowHandle);
            log.info("Switched to window: {}", windowHandle);
            return this;
        }

        public WindowHandler switchToNewWindow() {
            Set<String> handles = driver.getWindowHandles();
            String currentHandle = driver.getWindowHandle();
            for (String handle : handles) {
                if (!handle.equals(currentHandle)) {
                    driver.switchTo().window(handle);
                    log.info("Switched to new window");
                    break;
                }
            }
            return this;
        }

        public WindowHandler closeCurrentWindow() {
            driver.close();
            log.info("Closed current window");
            return this;
        }

        public Set<String> getAllWindowHandles() {
            return driver.getWindowHandles();
        }

        public String getCurrentWindowHandle() {
            return driver.getWindowHandle();
        }

        public int getWindowCount() {
            return driver.getWindowHandles().size();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Alert Handler
    // ═══════════════════════════════════════════════════════════════════════════

    public static class AlertHandler {
        private final WebDriver driver;

        private AlertHandler(WebDriver driver) {
            this.driver = driver;
        }

        public AlertHandler accept() {
            driver.switchTo().alert().accept();
            log.info("Alert accepted");
            return this;
        }

        public AlertHandler dismiss() {
            driver.switchTo().alert().dismiss();
            log.info("Alert dismissed");
            return this;
        }

        public String getText() {
            return driver.switchTo().alert().getText();
        }

        public AlertHandler sendKeys(String text) {
            driver.switchTo().alert().sendKeys(text);
            log.info("Sent keys to alert: {}", text);
            return this;
        }

        public boolean isPresent() {
            try {
                driver.switchTo().alert();
                return true;
            } catch (NoAlertPresentException e) {
                return false;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Frame Handler
    // ═══════════════════════════════════════════════════════════════════════════

    public static class FrameHandler {
        private final WebDriver driver;

        private FrameHandler(WebDriver driver) {
            this.driver = driver;
        }

        public FrameHandler switchToFrame(int index) {
            driver.switchTo().frame(index);
            log.info("Switched to frame index: {}", index);
            return this;
        }

        public FrameHandler switchToFrame(String nameOrId) {
            driver.switchTo().frame(nameOrId);
            log.info("Switched to frame: {}", nameOrId);
            return this;
        }

        public FrameHandler switchToFrame(WebElement element) {
            driver.switchTo().frame(element);
            log.info("Switched to frame element");
            return this;
        }

        public FrameHandler switchToParentFrame() {
            driver.switchTo().parentFrame();
            log.info("Switched to parent frame");
            return this;
        }

        public FrameHandler switchToDefaultContent() {
            driver.switchTo().defaultContent();
            log.info("Switched to default content");
            return this;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Cookie Handler
    // ═══════════════════════════════════════════════════════════════════════════

    public static class CookieHandler {
        private final WebDriver driver;

        private CookieHandler(WebDriver driver) {
            this.driver = driver;
        }

        public CookieHandler addCookie(String name, String value) {
            driver.manage().addCookie(new Cookie(name, value));
            log.info("Cookie added: {}", name);
            return this;
        }

        public CookieHandler addCookie(Cookie cookie) {
            driver.manage().addCookie(cookie);
            log.info("Cookie added: {}", cookie.getName());
            return this;
        }

        public Cookie getCookie(String name) {
            return driver.manage().getCookieNamed(name);
        }

        public Set<Cookie> getAllCookies() {
            return driver.manage().getCookies();
        }

        public CookieHandler deleteCookie(String name) {
            driver.manage().deleteCookieNamed(name);
            log.info("Cookie deleted: {}", name);
            return this;
        }

        public CookieHandler deleteCookie(Cookie cookie) {
            driver.manage().deleteCookie(cookie);
            return this;
        }

        public CookieHandler deleteAllCookies() {
            driver.manage().deleteAllCookies();
            log.info("All cookies deleted");
            return this;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Screenshot Handler
    // ═══════════════════════════════════════════════════════════════════════════

    public static class ScreenshotHandler {
        private final WebDriver driver;
        private final String baseDir;

        private ScreenshotHandler(WebDriver driver, String baseDir) {
            this.driver = driver;
            this.baseDir = baseDir;
        }

        public File capture(String fileName) {
            try {
                File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                File destination = new File(baseDir, fileName + ".png");
                destination.getParentFile().mkdirs();
                Files.copy(screenshot.toPath(), destination.toPath());
                log.info("Screenshot saved: {}", destination.getAbsolutePath());
                return destination;
            } catch (IOException e) {
                log.error("Failed to save screenshot", e);
                throw new SeleniumTestException("Screenshot failed", e);
            }
        }

        public byte[] captureBytes() {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        }

        public String captureBase64() {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        }

        public File captureElement(WebElement element, String fileName) {
            try {
                File screenshot = element.getScreenshotAs(OutputType.FILE);
                File destination = new File(baseDir, fileName + ".png");
                destination.getParentFile().mkdirs();
                Files.copy(screenshot.toPath(), destination.toPath());
                log.info("Element screenshot saved: {}", destination.getAbsolutePath());
                return destination;
            } catch (IOException e) {
                log.error("Failed to save element screenshot", e);
                throw new SeleniumTestException("Element screenshot failed", e);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Exception
    // ═══════════════════════════════════════════════════════════════════════════

    public static class SeleniumTestException extends RuntimeException {
        public SeleniumTestException(String message) {
            super(message);
        }

        public SeleniumTestException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

