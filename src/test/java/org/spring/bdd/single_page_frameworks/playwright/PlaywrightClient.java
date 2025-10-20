package org.spring.bdd.single_page_frameworks.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Production-grade Playwright Testing Framework
 * 
 * Features:
 * - Multi-browser support (Chromium, Firefox, WebKit)
 * - Thread-safe parallel execution
 * - Context isolation per test
 * - Fluent API design
 * - Auto-wait for elements
 * - Screenshot and video recording
 * - Network interception and mocking
 * - Tracing for debugging
 * - Cookie and storage management
 * - Mobile emulation
 * - Geolocation and permissions
 * - File upload/download
 * 
 * Usage:
 * <pre>
 * Page page = playwrightClient.chromium()
 *     .headless(false)
 *     .viewport(1920, 1080)
 *     .newPage();
 * 
 * playwrightClient.element(page)
 *     .locator("button#submit")
 *     .click();
 * 
 * playwrightClient.navigation(page)
 *     .to("https://example.com");
 * </pre>
 */
@Component
public class PlaywrightClient {

    private static final Logger log = LogManager.getLogger(PlaywrightClient.class);
    
    // Thread-local storage for parallel execution
    private static final ThreadLocal<Playwright> playwrightThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Browser> browserThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> contextThreadLocal = new ThreadLocal<>();
    private static final Map<Long, List<Page>> threadPages = new ConcurrentHashMap<>();

    @Value("${playwright.headless:true}")
    private boolean defaultHeadless;

    @Value("${playwright.slow.mo:0}")
    private int defaultSlowMo;

    @Value("${playwright.timeout:30000}")
    private int defaultTimeout;

    @Value("${playwright.screenshot.dir:./screenshots}")
    private String screenshotDir;

    @Value("${playwright.video.dir:./videos}")
    private String videoDir;

    @Value("${playwright.trace.dir:./traces}")
    private String traceDir;

    /**
     * Gets or creates Playwright instance for current thread
     */
    private Playwright getPlaywright() {
        if (playwrightThreadLocal.get() == null) {
            Playwright playwright = Playwright.create();
            playwrightThreadLocal.set(playwright);
            log.info("Playwright instance created for thread: {}", Thread.currentThread().getName());
        }
        return playwrightThreadLocal.get();
    }

    /**
     * Creates Chromium browser builder
     */
    public BrowserBuilder chromium() {
        return new BrowserBuilder(this, BrowserKind.CHROMIUM);
    }

    /**
     * Creates Firefox browser builder
     */
    public BrowserBuilder firefox() {
        return new BrowserBuilder(this, BrowserKind.FIREFOX);
    }

    /**
     * Creates WebKit browser builder
     */
    public BrowserBuilder webkit() {
        return new BrowserBuilder(this, BrowserKind.WEBKIT);
    }

    /**
     * Creates element handler for page
     */
    public ElementHandler element(Page page) {
        return new ElementHandler(page);
    }

    /**
     * Creates navigation handler for page
     */
    public NavigationHandler navigation(Page page) {
        return new NavigationHandler(page);
    }

    /**
     * Creates screenshot manager for page
     */
    public ScreenshotManager screenshot(Page page) {
        return new ScreenshotManager(page, screenshotDir);
    }

    /**
     * Creates network handler for page
     */
    public NetworkHandler network(Page page) {
        return new NetworkHandler(page);
    }

    /**
     * Creates storage handler for context
     */
    public StorageHandler storage(BrowserContext context) {
        return new StorageHandler(context);
    }

    /**
     * Creates tracing manager for context
     */
    public TracingManager tracing(BrowserContext context) {
        return new TracingManager(context, traceDir);
    }

    /**
     * Closes all resources for current thread
     */
    public void cleanup() {
        long threadId = Thread.currentThread().getId();
        
        // Close all pages
        List<Page> pages = threadPages.remove(threadId);
        if (pages != null) {
            pages.forEach(page -> {
                try {
                    if (!page.isClosed()) {
                        page.close();
                    }
                } catch (Exception e) {
                    log.warn("Error closing page", e);
                }
            });
        }

        // Close context
        BrowserContext context = contextThreadLocal.get();
        if (context != null) {
            try {
                context.close();
            } catch (Exception e) {
                log.warn("Error closing context", e);
            }
            contextThreadLocal.remove();
        }

        // Close browser
        Browser browser = browserThreadLocal.get();
        if (browser != null) {
            try {
                browser.close();
            } catch (Exception e) {
                log.warn("Error closing browser", e);
            }
            browserThreadLocal.remove();
        }

        // Close Playwright
        Playwright playwright = playwrightThreadLocal.get();
        if (playwright != null) {
            try {
                playwright.close();
            } catch (Exception e) {
                log.warn("Error closing Playwright", e);
            }
            playwrightThreadLocal.remove();
        }

        log.info("Cleanup completed for thread: {}", Thread.currentThread().getName());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Device Descriptor
    // ═══════════════════════════════════════════════════════════════════════════

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeviceDescriptor {
        public ViewportSize viewport = new ViewportSize(1920, 1080);
        public String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";
        public double deviceScaleFactor = 1.0;
        public boolean isMobile = false;
        public boolean isTouch = false;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Browser Type Enum
    // ═══════════════════════════════════════════════════════════════════════════

    private enum BrowserKind {
        CHROMIUM, FIREFOX, WEBKIT
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Browser Builder
    // ═══════════════════════════════════════════════════════════════════════════

    public static class BrowserBuilder {
        private final PlaywrightClient client;
        private final BrowserKind browserKind;
        private boolean headless;
        private int slowMo;
        private final List<String> args = new ArrayList<>();
        private String downloadsPath;
        private boolean devtools = false;
        private Integer viewportWidth;
        private Integer viewportHeight;
        private String userAgent;
        private Geolocation geolocation;
        private List<String> permissions;
        private String locale;
        private String timezone;
        private boolean recordVideo = false;
        private boolean recordTrace = false;
        private DeviceDescriptor deviceDescriptor;

        private BrowserBuilder(PlaywrightClient client, BrowserKind browserKind) {
            this.client = client;
            this.browserKind = browserKind;
            this.headless = client.defaultHeadless;
            this.slowMo = client.defaultSlowMo;
        }

        public BrowserBuilder headless(boolean headless) {
            this.headless = headless;
            return this;
        }

        public BrowserBuilder slowMo(int milliseconds) {
            this.slowMo = milliseconds;
            return this;
        }

        public BrowserBuilder args(String... args) {
            this.args.addAll(Arrays.asList(args));
            return this;
        }

        public BrowserBuilder devtools() {
            this.devtools = true;
            return this;
        }

        public BrowserBuilder viewport(int width, int height) {
            this.viewportWidth = width;
            this.viewportHeight = height;
            return this;
        }

        public BrowserBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public BrowserBuilder geolocation(double latitude, double longitude) {
            this.geolocation = new Geolocation(latitude, longitude);
            return this;
        }

        public BrowserBuilder permissions(String... permissions) {
            this.permissions = Arrays.asList(permissions);
            return this;
        }

        public BrowserBuilder locale(String locale) {
            this.locale = locale;
            return this;
        }

        public BrowserBuilder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }

        public BrowserBuilder downloadsPath(String path) {
            this.downloadsPath = path;
            return this;
        }

        public BrowserBuilder recordVideo() {
            this.recordVideo = true;
            return this;
        }

        public BrowserBuilder recordTrace() {
            this.recordTrace = true;
            return this;
        }

        public BrowserBuilder emulateDevice(DeviceDescriptor deviceDesc) {
            this.deviceDescriptor = deviceDesc;
            if (deviceDescriptor == null) {
                throw new PlaywrightTestException("Unknown device: " + deviceDesc);
            }
            return this;
        }

        public Page newPage() {
            // Launch browser if not already launched for this thread
            if (browserThreadLocal.get() == null) {
                BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setSlowMo(slowMo)
                        .setDevtools(devtools);

                if (!args.isEmpty()) {
                    launchOptions.setArgs(args);
                }

                Browser browser = switch (browserKind) {
                    case CHROMIUM -> client.getPlaywright().chromium().launch(launchOptions);
                    case FIREFOX -> client.getPlaywright().firefox().launch(launchOptions);
                    case WEBKIT -> client.getPlaywright().webkit().launch(launchOptions);
                };

                browserThreadLocal.set(browser);
                log.info("Browser launched: {} (headless: {})", browserKind, headless);
            }

            // Create context
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions();

            if (viewportWidth != null && viewportHeight != null) {
                contextOptions.setViewportSize(viewportWidth, viewportHeight);
            }

            if (userAgent != null) {
                contextOptions.setUserAgent(userAgent);
            }

            if (geolocation != null) {
                contextOptions.setGeolocation(geolocation);
            }

            if (permissions != null) {
                contextOptions.setPermissions(permissions);
            }

            if (locale != null) {
                contextOptions.setLocale(locale);
            }

            if (timezone != null) {
                contextOptions.setTimezoneId(timezone);
            }

            if (downloadsPath != null) {
                contextOptions.setAcceptDownloads(true);
            }

            if (recordVideo) {
                contextOptions.setRecordVideoDir(Paths.get(client.videoDir));
            }

            if (deviceDescriptor != null) {
                contextOptions.setViewportSize(deviceDescriptor.getViewport().width, deviceDescriptor.getViewport().height);
                contextOptions.setUserAgent(deviceDescriptor.getUserAgent());
                contextOptions.setDeviceScaleFactor(deviceDescriptor.getDeviceScaleFactor());
                contextOptions.setIsMobile(deviceDescriptor.isMobile());
                contextOptions.setHasTouch(deviceDescriptor.isTouch());
            }

            BrowserContext context = browserThreadLocal.get().newContext(contextOptions);
            context.setDefaultTimeout(client.defaultTimeout);
            contextThreadLocal.set(context);

            if (recordTrace) {
                context.tracing().start(new Tracing.StartOptions()
                        .setScreenshots(true)
                        .setSnapshots(true));
            }

            // Create page
            Page page = context.newPage();
            
            // Track page for cleanup
            long threadId = Thread.currentThread().getId();
            threadPages.computeIfAbsent(threadId, k -> new ArrayList<>()).add(page);

            log.info("New page created in context");
            return page;
        }

        public BrowserContext newContext() {
            if (browserThreadLocal.get() == null) {
                newPage(); // This will create browser and context
                return contextThreadLocal.get();
            }
            return contextThreadLocal.get();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Element Handler
    // ═══════════════════════════════════════════════════════════════════════════

    public static class ElementHandler {
        private final Page page;
        private Locator locator;

        private ElementHandler(Page page) {
            this.page = page;
        }

        public ElementHandler locator(String selector) {
            this.locator = page.locator(selector);
            return this;
        }

        public ElementHandler byTestId(String testId) {
            this.locator = page.getByTestId(testId);
            return this;
        }

        public ElementHandler byRole(AriaRole role) {
            this.locator = page.getByRole(role);
            return this;
        }

        public ElementHandler byText(String text) {
            this.locator = page.getByText(text);
            return this;
        }

        public ElementHandler byLabel(String label) {
            this.locator = page.getByLabel(label);
            return this;
        }

        public ElementHandler byPlaceholder(String placeholder) {
            this.locator = page.getByPlaceholder(placeholder);
            return this;
        }

        public ElementHandler byTitle(String title) {
            this.locator = page.getByTitle(title);
            return this;
        }

        public ElementHandler filter(String text) {
            this.locator = locator.filter(new Locator.FilterOptions().setHasText(text));
            return this;
        }

        public ElementHandler nth(int index) {
            this.locator = locator.nth(index);
            return this;
        }

        public ElementHandler first() {
            this.locator = locator.first();
            return this;
        }

        public ElementHandler last() {
            this.locator = locator.last();
            return this;
        }

        public ElementHandler click() {
            locator.click();
            log.debug("Clicked element: {}", locator);
            return this;
        }

        public ElementHandler doubleClick() {
            locator.dblclick();
            log.debug("Double-clicked element: {}", locator);
            return this;
        }

        public ElementHandler rightClick() {
            locator.click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            log.debug("Right-clicked element: {}", locator);
            return this;
        }

        public ElementHandler fill(String text) {
            locator.fill(text);
            log.debug("Filled '{}' into element: {}", text, locator);
            return this;
        }

        public ElementHandler type(String text, int delayMs) {
            locator.pressSequentially(text, new Locator.PressSequentiallyOptions().setDelay(delayMs));
            log.debug("Typed '{}' into element with delay {}ms", text, delayMs);
            return this;
        }

        public ElementHandler clear() {
            locator.clear();
            log.debug("Cleared element: {}", locator);
            return this;
        }

        public ElementHandler check() {
            locator.check();
            log.debug("Checked element: {}", locator);
            return this;
        }

        public ElementHandler uncheck() {
            locator.uncheck();
            log.debug("Unchecked element: {}", locator);
            return this;
        }

        public ElementHandler selectOption(String value) {
            locator.selectOption(value);
            log.debug("Selected option '{}' in element", value);
            return this;
        }

        public ElementHandler hover() {
            locator.hover();
            log.debug("Hovered over element: {}", locator);
            return this;
        }

        public ElementHandler press(String key) {
            locator.press(key);
            log.debug("Pressed key '{}' on element", key);
            return this;
        }

        public ElementHandler scrollIntoView() {
            locator.scrollIntoViewIfNeeded();
            log.debug("Scrolled element into view: {}", locator);
            return this;
        }

        public ElementHandler focus() {
            locator.focus();
            log.debug("Focused element: {}", locator);
            return this;
        }

        public String getText() {
            String text = locator.textContent();
            log.debug("Got text '{}' from element", text);
            return text;
        }

        public String getInnerText() {
            return locator.innerText();
        }

        public String getAttribute(String name) {
            return locator.getAttribute(name);
        }

        public String getInputValue() {
            return locator.inputValue();
        }

        public boolean isVisible() {
            return locator.isVisible();
        }

        public boolean isHidden() {
            return locator.isHidden();
        }

        public boolean isEnabled() {
            return locator.isEnabled();
        }

        public boolean isDisabled() {
            return locator.isDisabled();
        }

        public boolean isChecked() {
            return locator.isChecked();
        }

        public ElementHandler waitFor() {
            locator.waitFor();
            return this;
        }

        public ElementHandler waitForVisible() {
            locator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            return this;
        }

        public ElementHandler waitForHidden() {
            locator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
            return this;
        }

        public int count() {
            return locator.count();
        }

        public List<String> allTextContents() {
            return locator.allTextContents();
        }

        public ElementHandler uploadFile(String filePath) {
            locator.setInputFiles(Paths.get(filePath));
            log.info("Uploaded file: {}", filePath);
            return this;
        }

        public ElementHandler uploadFiles(String... filePaths) {
            Path[] paths = Arrays.stream(filePaths).map(Paths::get).toArray(Path[]::new);
            locator.setInputFiles(paths);
            log.info("Uploaded {} files", filePaths.length);
            return this;
        }

        public BoundingBox boundingBox() {
            return locator.boundingBox();
        }

        public byte[] screenshot() {
            return locator.screenshot();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Navigation Handler
    // ═══════════════════════════════════════════════════════════════════════════

    public static class NavigationHandler {
        private final Page page;

        private NavigationHandler(Page page) {
            this.page = page;
        }

        public NavigationHandler to(String url) {
            page.navigate(url);
            log.info("Navigated to: {}", url);
            return this;
        }

        public NavigationHandler reload() {
            page.reload();
            log.info("Page reloaded");
            return this;
        }

        public NavigationHandler back() {
            page.goBack();
            log.info("Navigated back");
            return this;
        }

        public NavigationHandler forward() {
            page.goForward();
            log.info("Navigated forward");
            return this;
        }

        public NavigationHandler waitForLoadState() {
            page.waitForLoadState();
            return this;
        }

        public NavigationHandler waitForLoadState(LoadState state) {
            page.waitForLoadState(state);
            return this;
        }

        public NavigationHandler waitForURL(String url) {
            page.waitForURL(url);
            return this;
        }

        public NavigationHandler waitForURL(Pattern pattern) {
            page.waitForURL(pattern);
            return this;
        }

        public String getURL() {
            return page.url();
        }

        public String getTitle() {
            return page.title();
        }

        public NavigationHandler setViewportSize(int width, int height) {
            page.setViewportSize(width, height);
            log.info("Viewport set to: {}x{}", width, height);
            return this;
        }

        public Object evaluate(String script) {
            return page.evaluate(script);
        }

        public Object evaluateHandle(String script) {
            return page.evaluateHandle(script);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Screenshot Manager
    // ═══════════════════════════════════════════════════════════════════════════

    public static class ScreenshotManager {
        private final Page page;
        private final String baseDir;

        private ScreenshotManager(Page page, String baseDir) {
            this.page = page;
            this.baseDir = baseDir;
        }

        public File capture(String fileName) {
            Path path = Paths.get(baseDir, fileName + ".png");
            path.getParent().toFile().mkdirs();
            page.screenshot(new Page.ScreenshotOptions().setPath(path));
            log.info("Screenshot saved: {}", path);
            return path.toFile();
        }

        public File captureFullPage(String fileName) {
            Path path = Paths.get(baseDir, fileName + ".png");
            path.getParent().toFile().mkdirs();
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(path)
                    .setFullPage(true));
            log.info("Full page screenshot saved: {}", path);
            return path.toFile();
        }

        public byte[] captureBytes() {
            return page.screenshot();
        }

        public byte[] captureFullPageBytes() {
            return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Network Handler
    // ═══════════════════════════════════════════════════════════════════════════

    public static class NetworkHandler {
        private final Page page;
        private final List<Request> requests = new ArrayList<>();
        private final List<Response> responses = new ArrayList<>();

        private NetworkHandler(Page page) {
            this.page = page;
        }

        public NetworkHandler startMonitoring() {
            page.onRequest(requests::add);
            page.onResponse(responses::add);
            log.info("Network monitoring started");
            return this;
        }

        public NetworkHandler intercept(String urlPattern, Consumer<Route> handler) {
            page.route(urlPattern, handler);
            log.info("Route intercepted: {}", urlPattern);
            return this;
        }

        public NetworkHandler mockResponse(String urlPattern, String body, int status) {
            page.route(urlPattern, route -> {
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(status)
                        .setBody(body));
            });
            log.info("Mocked response for: {}", urlPattern);
            return this;
        }

        public NetworkHandler blockRequests(String urlPattern) {
            page.route(urlPattern, Route::abort);
            log.info("Blocked requests to: {}", urlPattern);
            return this;
        }

        public NetworkHandler waitForResponse(String urlPattern) {
            page.waitForResponse(urlPattern, () -> {});
            return this;
        }

        public NetworkHandler waitForRequest(String urlPattern) {
            page.waitForRequest(urlPattern, () -> {});
            return this;
        }

        public List<Request> getRequests() {
            return new ArrayList<>(requests);
        }

        public List<Response> getResponses() {
            return new ArrayList<>(responses);
        }

        public NetworkHandler clearHistory() {
            requests.clear();
            responses.clear();
            return this;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Storage Handler
    // ═══════════════════════════════════════════════════════════════════════════

    public static class StorageHandler {
        private final BrowserContext context;

        private StorageHandler(BrowserContext context) {
            this.context = context;
        }

        public StorageHandler addCookie(String name, String value, String domain) {
            context.addCookies(Arrays.asList(new Cookie(name, value)
                    .setDomain(domain)));
            log.info("Cookie added: {}", name);
            return this;
        }

        public StorageHandler addCookies(List<Cookie> cookies) {
            context.addCookies(cookies);
            log.info("Added {} cookies", cookies.size());
            return this;
        }

        public List<Cookie> getCookies() {
            return context.cookies();
        }

        public List<Cookie> getCookies(String url) {
            return context.cookies(url);
        }

        public StorageHandler clearCookies() {
            context.clearCookies();
            log.info("Cookies cleared");
            return this;
        }

        public StorageHandler saveState(String filePath) {
            context.storageState(new BrowserContext.StorageStateOptions()
                    .setPath(Paths.get(filePath)));
            log.info("Storage state saved: {}", filePath);
            return this;
        }

        public StorageHandler loadState(String filePath) {
            // Note: State must be loaded when creating context
            log.info("Storage state should be loaded during context creation");
            return this;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Tracing Manager
    // ═══════════════════════════════════════════════════════════════════════════

    public static class TracingManager {
        private final BrowserContext context;
        private final String baseDir;

        private TracingManager(BrowserContext context, String baseDir) {
            this.context = context;
            this.baseDir = baseDir;
        }

        public TracingManager start() {
            context.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true)
                    .setSnapshots(true)
                    .setSources(true));
            log.info("Tracing started");
            return this;
        }

        public TracingManager stop(String fileName) {
            Path path = Paths.get(baseDir, fileName + ".zip");
            path.getParent().toFile().mkdirs();
            context.tracing().stop(new Tracing.StopOptions().setPath(path));
            log.info("Trace saved: {}", path);
            return this;
        }

        public TracingManager startChunk() {
            context.tracing().startChunk();
            return this;
        }

        public TracingManager stopChunk(String fileName) {
            Path path = Paths.get(baseDir, fileName + ".zip");
            path.getParent().toFile().mkdirs();
            context.tracing().stopChunk(new Tracing.StopChunkOptions().setPath(path));
            log.info("Trace chunk saved: {}", path);
            return this;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Custom Exception
    // ═══════════════════════════════════════════════════════════════════════════

    public static class PlaywrightTestException extends RuntimeException {
        public PlaywrightTestException(String message) {
            super(message);
        }

        public PlaywrightTestException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

