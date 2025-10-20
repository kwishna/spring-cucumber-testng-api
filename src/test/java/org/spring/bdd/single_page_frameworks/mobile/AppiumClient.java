package org.spring.bdd.single_page_frameworks.mobile;


import io.appium.java_client.AppiumDriver;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;

/**
 * Production-grade Appium Mobile Testing Framework * * Features: * - Android and iOS support * - Fluent API for driver creation * - Gestures and touch actions * - Element finding strategies * - Wait mechanisms * - Screenshot capabilities * - App lifecycle management * - Context switching (Native/WebView) * - Device interactions * - Cloud provider support (BrowserStack, Sauce Labs) * * Usage: * <pre> * AppiumDriver driver = appiumClient.android() * .deviceName("Pixel 6") * .platformVersion("13") * .app("/path/to/app.apk") * .build(); * * appiumClient.element(driver) * .byId("com.example:id/button") * .tap(); * * appiumClient.gestures(driver) * .swipeUp() * .swipeLeft(); * </pre>
 */
@Component
public class AppiumClient {
    private static final Logger log = LogManager.getLogger(AppiumClient.class);
    @Value("${appium.server.url:http://localhost:4723}")
    private String serverUrl;
    @Value("${appium.implicit.wait:10}")
    private int implicitWait;
    @Value("${appium.explicit.wait:20}")
    private int explicitWait;
    @Value("${appium.screenshot.dir:./screenshots}")
    private String screenshotDir;

    /**
     * Creates Android driver builder
     */
    public AndroidDriverBuilder android() {
        return new AndroidDriverBuilder(this);
    }

    /**
     * Creates iOS driver builder
     */
    public IOSDriverBuilder ios() {
        return new IOSDriverBuilder(this);
    }

    /**
     * Creates element finder
     */
    public ElementFinder element(AppiumDriver driver) {
        return new ElementFinder(driver, explicitWait);
    }

    /**
     * Creates element finder with custom wait
     */
    public ElementFinder element(AppiumDriver driver, int waitSeconds) {
        return new ElementFinder(driver, waitSeconds);
    }

    /**
     * Creates gesture handler
     */
    public GestureHandler gestures(AppiumDriver driver) {
        return new GestureHandler(driver);
    }

    /**
     * Creates app manager
     */
    public AppManager app(AppiumDriver driver) {
        return new AppManager((AndroidDriver) driver);
    }

    /**
     * Creates device controller
     */
    public DeviceController device(AppiumDriver driver) {
        return new DeviceController((AndroidDriver) driver);
    }

    /**
     * Takes screenshot
     */
    public File takeScreenshot(AppiumDriver driver, String fileName) {
        try {
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File destination = new File(screenshotDir, fileName + ".png");
            destination.getParentFile().mkdirs();
            Files.copy(screenshotFile.toPath(), destination.toPath());
            log.info("Screenshot saved: {}", destination.getAbsolutePath());
            return destination;
        } catch (IOException e) {
            log.error("Failed to save screenshot", e);
            throw new MobileTestException("Screenshot failed", e);
        }
    }

// ═══════════════════════════════════════════════════════════════════════════ // Android Driver Builder // ═══════════════════════════════════════════════════════════════════════════

    public static class AndroidDriverBuilder {
        private final AppiumClient client;
        private final DesiredCapabilities capabilities = new DesiredCapabilities();
        private String serverUrl;

        private AndroidDriverBuilder(AppiumClient client) {
            this.client = client;
            this.serverUrl = client.serverUrl;
            // Default Android capabilities
            capabilities.setCapability(CapabilityType.PLATFORM_NAME, "Android");
            capabilities.setCapability("AUTOMATION_NAME", "UiAutomator2");
            capabilities.setCapability("NEW_COMMAND_TIMEOUT", 300);
        }

        public AndroidDriverBuilder serverUrl(String url) {
            this.serverUrl = url;
            return this;
        }

        public AndroidDriverBuilder deviceName(String name) {
            capabilities.setCapability("DEVICE_NAME", name);
            return this;
        }

        public AndroidDriverBuilder udid(String udid) {
            capabilities.setCapability("UDID", udid);
            return this;
        }

        public AndroidDriverBuilder platformVersion(String version) {
            capabilities.setCapability("PLATFORM_VERSION", version);
            return this;
        }

        public AndroidDriverBuilder app(String appPath) {
            capabilities.setCapability("APP", appPath);
            return this;
        }

        public AndroidDriverBuilder appPackage(String packageName) {
            capabilities.setCapability("APP_PACKAGE", packageName);
            return this;
        }

        public AndroidDriverBuilder appActivity(String activity) {
            capabilities.setCapability("APP_ACTIVITY", activity);
            return this;
        }

        public AndroidDriverBuilder noReset() {
            capabilities.setCapability("NO_RESET", true);
            return this;
        }

        public AndroidDriverBuilder fullReset() {
            capabilities.setCapability("FULL_RESET", true);
            return this;
        }

        public AndroidDriverBuilder autoGrantPermissions() {
            capabilities.setCapability("AUTO_GRANT_PERMISSIONS", true);
            return this;
        }

        public AndroidDriverBuilder chromedriverExecutable(String path) {
            capabilities.setCapability("CHROMEDRIVER_EXECUTABLE", path);
            return this;
        }

        public AndroidDriverBuilder capability(String name, Object value) {
            capabilities.setCapability(name, value);
            return this;
        }

        public AndroidDriver build() {
            try {
                log.info("Creating Android driver with URL: {}", serverUrl);
                log.info("Capabilities: {}", capabilities);
                AndroidDriver driver = new AndroidDriver(new URL(serverUrl), capabilities);
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(client.implicitWait));
                log.info("Android driver created successfully");
                return driver;
            } catch (MalformedURLException e) {
                log.error("Invalid Appium server URL: {}", serverUrl, e);
                throw new MobileTestException("Failed to create Android driver", e);
            }
        }
    }

// ═══════════════════════════════════════════════════════════════════════════ // iOS Driver Builder // ═══════════════════════════════════════════════════════════════════════════

    public static class IOSDriverBuilder {
        private final AppiumClient client;
        private final DesiredCapabilities capabilities = new DesiredCapabilities();
        private String serverUrl;

        private IOSDriverBuilder(AppiumClient client) {
            this.client = client;
            this.serverUrl = client.serverUrl;
            // Default iOS capabilities
//            capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "iOS");
//            capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, "XCUITest");
//            capabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 300);

            capabilities.setCapability("PLATFORM_NAME", "iOS");
            capabilities.setCapability("AUTOMATION_NAME", "XCUITest");
            capabilities.setCapability("NEW_COMMAND_TIMEOUT", 300);
        }

        public IOSDriverBuilder serverUrl(String url) {
            this.serverUrl = url;
            return this;
        }

        public IOSDriverBuilder deviceName(String name) {
            capabilities.setCapability("DEVICE_NAME", name);
            return this;
        }

        public IOSDriverBuilder udid(String udid) {
            capabilities.setCapability("UDID", udid);
            return this;
        }

        public IOSDriverBuilder platformVersion(String version) {
            capabilities.setCapability("PLATFORM_VERSION", version);
            return this;
        }

        public IOSDriverBuilder app(String appPath) {
            capabilities.setCapability("APP", appPath);
            return this;
        }

        public IOSDriverBuilder bundleId(String bundleId) {
            capabilities.setCapability("BUNDLE_ID", bundleId);
            return this;
        }

        public IOSDriverBuilder noReset() {
            capabilities.setCapability("NO_RESET", true);
            return this;
        }

        public IOSDriverBuilder fullReset() {
            capabilities.setCapability("FULL_RESET", true);
            return this;
        }

        public IOSDriverBuilder autoAcceptAlerts() {
            capabilities.setCapability("AUTO_ACCEPT_ALERTS", true);
            return this;
        }

        public IOSDriverBuilder autoDismissAlerts() {
//            capabilities.setCapability(IOSMobileCapabilityType.AUTO_DISMISS_ALERTS, true);
            capabilities.setCapability("AUTO_DISMISS_ALERTS", true);
            return this;
        }

        public IOSDriverBuilder wdaLocalPort(int port) {
//            capabilities.setCapability(IOSMobileCapabilityType.WDA_LOCAL_PORT, port);
            capabilities.setCapability("WDA_LOCAL_PORT", port);
            return this;
        }

        public IOSDriverBuilder capability(String name, Object value) {
            capabilities.setCapability(name, value);
            return this;
        }

        public IOSDriver build() {
            try {
                log.info("Creating iOS driver with URL: {}", serverUrl);
                log.info("Capabilities: {}", capabilities);
                IOSDriver driver = new IOSDriver(new URL(serverUrl), capabilities);
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(client.implicitWait));
                log.info("iOS driver created successfully");
                return driver;
            } catch (MalformedURLException e) {
                log.error("Invalid Appium server URL: {}", serverUrl, e);
                throw new MobileTestException("Failed to create iOS driver", e);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════ // Element Finder // ═══════════════════════════════════════════════════════════════════════════
    public static class ElementFinder {
        private final AppiumDriver driver;
        private final WebDriverWait wait;
        private By locator;

        private ElementFinder(AppiumDriver driver, int waitSeconds) {
            this.driver = driver;
            this.wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
        }

        public ElementFinder byId(String id) {
            this.locator = AppiumBy.id(id);
            return this;
        }

        public ElementFinder byAccessibilityId(String id) {
            this.locator = AppiumBy.accessibilityId(id);
            return this;
        }

        public ElementFinder byXPath(String xpath) {
            this.locator = By.xpath(xpath);
            return this;
        }

        public ElementFinder byClassName(String className) {
            this.locator = By.className(className);
            return this;
        }

        public ElementFinder byAndroidUIAutomator(String uiAutomator) {
            this.locator = AppiumBy.androidUIAutomator(uiAutomator);
            return this;
        }

        public ElementFinder byIOSClassChain(String classChain) {
            this.locator = AppiumBy.iOSClassChain(classChain);
            return this;
        }

        public ElementFinder byIOSNsPredicate(String predicate) {
            this.locator = AppiumBy.iOSNsPredicateString(predicate);
            return this;
        }

        public ElementFinder byText(String text) {
            this.locator = By.xpath(String.format("//*[@text='%s']", text));
            return this;
        }

        public ElementFinder byTextContains(String text) {
            this.locator = By.xpath(String.format("//*[contains(@text, '%s')]", text));
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
            } catch (NoSuchElementException e) {
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

        public ElementFinder tap() {
            findClickable().click();
            log.info("Tapped element: {}", locator);
            return this;
        }

        public ElementFinder type(String text) {
            WebElement element = findVisible();
            element.clear();
            element.sendKeys(text);
            log.info("Typed '{}' into element: {}", text, locator);
            return this;
        }

        public ElementFinder append(String text) {
            findVisible().sendKeys(text);
            log.info("Appended '{}' to element: {}", text, locator);
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

        public ElementFinder clear() {
            findVisible().clear();
            log.info("Cleared element: {}", locator);
            return this;
        }

        public ElementFinder waitUntilVisible() {
            wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            return this;
        }

        public ElementFinder waitUntilInvisible() {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
            return this;
        }

        public ElementFinder waitUntilClickable() {
            wait.until(ExpectedConditions.elementToBeClickable(locator));
            return this;
        }

        public ElementFinder scrollIntoView() {
            WebElement element = find();
            driver.executeScript("arguments[0].scrollIntoView(true);", element);
            return this;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════ // Gesture Handler // ═══════════════════════════════════════════════════════════════════════════
    public static class GestureHandler {
        private final AppiumDriver driver;
        private final Dimension screenSize;

        private GestureHandler(AppiumDriver driver) {
            this.driver = driver;
            this.screenSize = driver.manage().window().getSize();
        }

        public GestureHandler swipeUp() {
            return swipeUp(0.8, 0.2);
        }

        public GestureHandler swipeUp(double startYPercent, double endYPercent) {
            int startX = screenSize.width / 2;
            int startY = (int) (screenSize.height * startYPercent);
            int endY = (int) (screenSize.height * endYPercent);
            swipe(startX, startY, startX, endY, 1000);
            log.info("Swiped up from {}% to {}%", startYPercent * 100, endYPercent * 100);
            return this;
        }

        public GestureHandler swipeDown() {
            return swipeDown(0.2, 0.8);
        }

        public GestureHandler swipeDown(double startYPercent, double endYPercent) {
            int startX = screenSize.width / 2;
            int startY = (int) (screenSize.height * startYPercent);
            int endY = (int) (screenSize.height * endYPercent);
            swipe(startX, startY, startX, endY, 1000);
            log.info("Swiped down from {}% to {}%", startYPercent * 100, endYPercent * 100);
            return this;
        }

        public GestureHandler swipeLeft() {
            return swipeLeft(0.8, 0.2);
        }

        public GestureHandler swipeLeft(double startXPercent, double endXPercent) {
            int startX = (int) (screenSize.width * startXPercent);
            int endX = (int) (screenSize.width * endXPercent);
            int y = screenSize.height / 2;
            swipe(startX, y, endX, y, 1000);
            log.info("Swiped left from {}% to {}%", startXPercent * 100, endXPercent * 100);
            return this;
        }

        public GestureHandler swipeRight() {
            return swipeRight(0.2, 0.8);
        }

        public GestureHandler swipeRight(double startXPercent, double endXPercent) {
            int startX = (int) (screenSize.width * startXPercent);
            int endX = (int) (screenSize.width * endXPercent);
            int y = screenSize.height / 2;
            swipe(startX, y, endX, y, 1000);
            log.info("Swiped right from {}% to {}%", startXPercent * 100, endXPercent * 100);
            return this;
        }

        public GestureHandler swipe(int startX, int startY, int endX, int endY, int durationMs) {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 1);
            swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(new Pause(finger, Duration.ofMillis(200)));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(durationMs), PointerInput.Origin.viewport(), endX, endY));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(swipe));
            return this;
        }

        public GestureHandler tap(int x, int y) {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence tap = new Sequence(finger, 1);
            tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
            tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(new Pause(finger, Duration.ofMillis(100)));
            tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(tap));
            log.info("Tapped at ({}, {})", x, y);
            return this;
        }

        public GestureHandler longPress(int x, int y, int durationMs) {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence longPress = new Sequence(finger, 1);
            longPress.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
            longPress.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            longPress.addAction(new Pause(finger, Duration.ofMillis(durationMs)));
            longPress.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(longPress));
            log.info("Long pressed at ({}, {}) for {}ms", x, y, durationMs);
            return this;
        }

        public GestureHandler scrollToElement(String text) {
            String scrollCommand = String.format("new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text(\"%s\"))", text);
            driver.findElement(AppiumBy.androidUIAutomator(scrollCommand));
            log.info("Scrolled to element with text: {}", text);
            return this;
        }

        public GestureHandler pinchOut() {
            int centerX = screenSize.width / 2;
            int centerY = screenSize.height / 2;
            int offset = 100;
            PointerInput finger1 = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
            PointerInput finger2 = new PointerInput(PointerInput.Kind.TOUCH, "finger2");
            Sequence pinch1 = new Sequence(finger1, 1);
            pinch1.addAction(finger1.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX, centerY));
            pinch1.addAction(finger1.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            pinch1.addAction(finger1.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), centerX - offset, centerY));
            pinch1.addAction(finger1.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            Sequence pinch2 = new Sequence(finger2, 1);
            pinch2.addAction(finger2.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX, centerY));
            pinch2.addAction(finger2.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            pinch2.addAction(finger2.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), centerX + offset, centerY));
            pinch2.addAction(finger2.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Arrays.asList(pinch1, pinch2));
            log.info("Performed pinch out (zoom in)");
            return this;
        }

        public GestureHandler pinchIn() {
            int centerX = screenSize.width / 2;
            int centerY = screenSize.height / 2;
            int offset = 100;
            PointerInput finger1 = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
            PointerInput finger2 = new PointerInput(PointerInput.Kind.TOUCH, "finger2");
            Sequence pinch1 = new Sequence(finger1, 1);
            pinch1.addAction(finger1.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX - offset, centerY));
            pinch1.addAction(finger1.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            pinch1.addAction(finger1.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), centerX, centerY));
            pinch1.addAction(finger1.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            Sequence pinch2 = new Sequence(finger2, 1);
            pinch2.addAction(finger2.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX + offset, centerY));
            pinch2.addAction(finger2.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            pinch2.addAction(finger2.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), centerX, centerY));
            pinch2.addAction(finger2.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Arrays.asList(pinch1, pinch2));
            log.info("Performed pinch in (zoom out)");
            return this;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════ // App Manager // ═══════════════════════════════════════════════════════════════════════════
    public static class AppManager {
        private final AndroidDriver driver;

        private AppManager(AndroidDriver driver) {
            this.driver = driver;
        }

        public AppManager install(String appPath) {
            driver.installApp(appPath);
            log.info("Installed app: {}", appPath);
            return this;
        }

        public AppManager uninstall(String bundleId) {
            driver.removeApp(bundleId);
            log.info("Uninstalled app: {}", bundleId);
            return this;
        }

        public boolean isInstalled(String bundleId) {
            boolean installed = driver.isAppInstalled(bundleId);
            log.debug("App {} installed: {}", bundleId, installed);
            return installed;
        }

        public AppManager launch(String bundleId) {
            driver.activateApp(bundleId);
            log.info("Launched app: {}", bundleId);
            return this;
        }

        public AppManager terminate(String bundleId) {
            driver.terminateApp(bundleId);
            log.info("Terminated app: {}", bundleId);
            return this;
        }

        public AppManager background(int seconds) {
            driver.runAppInBackground(Duration.ofSeconds(seconds));
            log.info("App sent to background for {} seconds", seconds);
            return this;
        }

//        public AppManager reset() {
//            driver.resetApp();
//            log.info("App reset");
//            return this;
//        }
//
//        public AppManager closeApp() {
//            driver.closeApp();
//            log.info("App closed");
//            return this;
//        }

        public String getState(String bundleId) {
            return driver.queryAppState(bundleId).toString();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════ // Device Controller // ═══════════════════════════════════════════════════════════════════════════
    public static class DeviceController {
        private final AndroidDriver driver;

        private DeviceController(AndroidDriver driver) {
            this.driver = driver;
        }

        public DeviceController hideKeyboard() {
            try {
                driver.hideKeyboard();
                log.info("Keyboard hidden");
            } catch (Exception e) {
                log.debug("Keyboard already hidden or not present");
            }
            return this;
        }

        public boolean isKeyboardShown() {
            return driver.isKeyboardShown();
        }

        public DeviceController rotate(ScreenOrientation orientation) {
            driver.rotate(orientation);
            log.info("Screen rotated to: {}", orientation);
            return this;
        }

        public ScreenOrientation getOrientation() {
            return driver.getOrientation();
        }

        public DeviceController lock() {
            driver.lockDevice();
            log.info("Device locked");
            return this;
        }

        public DeviceController unlock() {
            driver.unlockDevice();
            log.info("Device unlocked");
            return this;
        }

        public boolean isLocked() {
            return driver.isDeviceLocked();
        }

        public DeviceController switchToNative() {
            driver.context("NATIVE_APP");
            log.info("Switched to NATIVE context");
            return this;
        }

        public DeviceController switchToWebView() {
            Set<String> contexts = driver.getWindowHandles();
            for (String context : contexts) {
                if (context.contains("WEBVIEW")) {
                    driver.context(context);
                    log.info("Switched to WEBVIEW context: {}", context);
                    return this;
                }
            }
            throw new MobileTestException("No WebView context available");
        }

        public DeviceController switchToContext(String contextName) {
            driver.context(contextName);
            log.info("Switched to context: {}", contextName);
            return this;
        }

        public Set<String> getContexts() {
            return driver.getContextHandles();
        }

        public String getCurrentContext() {
            return driver.getContext();
        }

        public DeviceController pressBack() {
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.BACK));
                log.info("Pressed back button");
            }
            return this;
        }

        public DeviceController pressHome() {
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).pressKey(new io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.HOME));
                log.info("Pressed home button");
            }
            return this;
        }

        public DeviceController openNotifications() {
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).openNotifications();
                log.info("Opened notifications");
            }
            return this;
        }

        public String getClipboard() {
            return driver.getClipboardText();
        }

        public DeviceController setClipboard(String text) {
            driver.setClipboardText(text);
            log.info("Clipboard set to: {}", text);
            return this;
        }

        public Map<String, String> getDeviceInfo() {
            Map<String, String> info = new HashMap<>();
//            info.put("platformName", driver.getPlatformName());
//            info.put("platformVersion", driver.getCapabilities().getPlatformVersion().toString());
            info.put("deviceName", driver.getCapabilities().getCapability("deviceName").toString());
            return info;
        }
    }

// ═══════════════════════════════════════════════════════════════════════════ // Cloud Provider Builders // ═══════════════════════════════════════════════════════════════════════════

    /**
     * BrowserStack configuration builder
     */
    public static class BrowserStackBuilder {
        private final DesiredCapabilities capabilities = new DesiredCapabilities();
        private String username;
        private String accessKey;

        public BrowserStackBuilder username(String username) {
            this.username = username;
            return this;
        }

        public BrowserStackBuilder accessKey(String accessKey) {
            this.accessKey = accessKey;
            return this;
        }

        public BrowserStackBuilder device(String device) {
            capabilities.setCapability("device", device);
            return this;
        }

        public BrowserStackBuilder osVersion(String version) {
            capabilities.setCapability("os_version", version);
            return this;
        }

        public BrowserStackBuilder app(String appUrl) {
            capabilities.setCapability("app", appUrl);
            return this;
        }

        public BrowserStackBuilder projectName(String project) {
            capabilities.setCapability("project", project);
            return this;
        }

        public BrowserStackBuilder buildName(String build) {
            capabilities.setCapability("build", build);
            return this;
        }

        public BrowserStackBuilder testName(String name) {
            capabilities.setCapability("name", name);
            return this;
        }

        public AppiumDriver build() {
            try {
                String url = String.format("https://%s:%s@hub-cloud.browserstack.com/wd/hub", username, accessKey);
                return new AndroidDriver(new URL(url), capabilities);
            } catch (MalformedURLException e) {
                throw new MobileTestException("Failed to create BrowserStack driver", e);
            }
        }
    }
    // ═══════════════════════════════════════════════════════════════════════════
    // Exception Class
    // ═══════════════════════════════════════════════════════════════════════════

    public static class MobileTestException extends RuntimeException {
        public MobileTestException(String message) {
            super(message);
        }

        public MobileTestException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

