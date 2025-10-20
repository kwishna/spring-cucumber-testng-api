# 🌱 Spring Dependency Injection Explained — Step by Step

> **Goal:** Understand how Spring automatically injects your `DriverFactory` into the `webDriver()` method.

---

## 🎯 The Core Question

```java
@Bean
public WebDriver webDriver(DriverFactory factory) {  // ← How does this get the factory?
    return factory.createWebDriver(browser);
}
```

You never call `new DriverFactory()` yourself — yet, magically, it’s available.
Let’s break down exactly **how Spring makes that happen**.

---

## 🧩 Step 1: Spring Discovers Components

When your app starts, Spring scans the packages you specify:

```java
@ComponentScan(basePackages = "org.spring.bdd")  // from TestConfig.java
```

Spring finds your factory:

```java
@Component  // 👈 "Spring, please manage me as a bean!"
public class DriverFactory {

    @Value("${browser:chrome}")
    private String defaultBrowser;

    public WebDriver createWebDriver(String browser) {
        // Implementation
    }
}
```

### 🧠 What Spring Does Behind the Scenes

```java
DriverFactory driverFactory = new DriverFactory();

// Injects all @Value properties:
driverFactory.defaultBrowser = "chrome";

// Stores it in the ApplicationContext (like a registry of beans)
applicationContext.put("driverFactory", driverFactory);
```

Spring now “owns” this instance. You don’t need to `new` it again.

---

## 🧱 Step 2: Spring Finds @Configuration Classes

```java
@Configuration  // 👈 "I declare beans here!"
public class WebDriverConfig {

    @Bean(destroyMethod = "quit")
    @Scope("cucumber-glue")
    public WebDriver webDriver(DriverFactory factory) {  // 👈 Parameter injection
        return factory.createWebDriver(browser);
    }
}
```

### 🧠 What Spring Does

Spring registers a “recipe” for this bean:

```java
beanDefinitions.put("webDriver", {
    type: WebDriver.class,
    scope: "cucumber-glue",
    parameters: [DriverFactory.class],
    destroyMethod: "quit"
});
```

At this point, **Spring knows**:

* How to create a `WebDriver`
* That it needs a `DriverFactory` to do so
* When to destroy it (`quit()`)

---

## 🚀 Step 3: When a Test Needs WebDriver

```java
public class LoginSteps extends BaseUiSteps {

    @Autowired
    private WebDriver driver;  // 👈 "Give me a WebDriver, please!"
}
```

### 🔍 What Happens Internally

```java
// 1️⃣ Spring checks its registry
BeanDefinition def = beanDefinitions.get("webDriver");

// 2️⃣ It sees: needs DriverFactory
DriverFactory factory = applicationContext.get("driverFactory");

// 3️⃣ It finds your WebDriverConfig class
WebDriverConfig config = applicationContext.get("webDriverConfig");

// 4️⃣ It calls your @Bean method
WebDriver driver = config.webDriver(factory);

// 5️⃣ It injects the result
loginSteps.driver = driver;
```

Boom 💥 — dependency injected.

---

## 🔄 Lifecycle Flow Diagram

```
Application Startup
    ↓
[1] Component Scan
     → Finds @Component classes (DriverFactory)
     → Creates and stores them in ApplicationContext
    ↓
[2] @Configuration Processing
     → Registers @Bean definitions (WebDriver)
     → Notes dependencies (DriverFactory)
    ↓
[3] Test Execution
     → @Autowired WebDriver requested
     → Spring resolves dependencies
     → Calls webDriver(factory)
     → Injects into your step class
```

---

## 🧠 Key Spring Concepts Refresher

| Annotation       | Meaning                                 | Example                     |
| ---------------- | --------------------------------------- | --------------------------- |
| `@Component`     | "I’m a managed bean."                   | `DriverFactory`             |
| `@Configuration` | "I define other beans."                 | `WebDriverConfig`           |
| `@Bean`          | "Here’s how to create a bean instance." | `webDriver()`               |
| `@Autowired`     | "Inject a bean into me."                | `private WebDriver driver;` |

### Method Parameter Injection

You can inject any managed bean directly via method parameters:

```java
@Bean
public WebDriver webDriver(
    DriverFactory factory,     // Injected
    Environment env,           // Injected
    SomeOtherBean bean         // Injected
) {
    return factory.createWebDriver(browser);
}
```

---

## 🪵 Add Logging to See It in Action

```java
@Component
public class DriverFactory {

    private static final Logger log = LogManager.getLogger(DriverFactory.class);

    public DriverFactory() {
        log.info("🏭 DriverFactory constructor called");
    }

    public WebDriver createWebDriver(String browser) {
        log.info("🚗 Creating WebDriver for: {}", browser);
        return new ChromeDriver();
    }
}

@Configuration
public class WebDriverConfig {

    private static final Logger log = LogManager.getLogger(WebDriverConfig.class);

    @Bean(destroyMethod = "quit")
    @Scope("cucumber-glue")
    public WebDriver webDriver(DriverFactory factory) {
        log.info("🔧 @Bean method called with factory: {}", factory);
        WebDriver driver = factory.createWebDriver("chrome");
        log.info("✅ WebDriver created: {}", driver);
        return driver;
    }
}
```

**Expected Console Output:**

```
[INFO] 🏭 DriverFactory constructor called
[INFO] ⚙️ WebDriverConfig loaded
[INFO] 🔧 webDriver() @Bean method called
[INFO] 📦 DriverFactory injected: org.spring.bdd.envs.DriverFactory@1a2b3c4d
[INFO] 🚗 Creating WebDriver for: chrome
[INFO] ✅ WebDriver created: ChromeDriver: chrome on MAC (session 1234)
```

---

## ❓ Common Questions

### Q: Who calls `webDriver()`?

> Spring’s **ApplicationContext** calls it when a `WebDriver` is needed.

---

### Q: How does Spring inject `DriverFactory`?

1. It scans and creates all `@Component` beans first
2. It stores them in the `ApplicationContext`
3. When `webDriver()` is called, Spring sees it requires a `DriverFactory`
4. Spring retrieves and injects that existing bean instance

---

### Q: What if `DriverFactory` doesn’t exist?

> You’ll get:
>
> ```
> No qualifying bean of type 'DriverFactory' available
> ```

Spring can only inject managed beans.

---

### Q: Can I inject multiple dependencies?

Absolutely:

```java
@Bean
public WebDriver webDriver(
    DriverFactory factory,
    Environment env,
    ApplicationContext ctx
) { ... }
```

---

### Q: Why use method parameters instead of `@Autowired` fields?

Both are valid — but parameter injection is **cleaner for @Bean methods**:

```java
// ✅ Preferred (parameter injection)
@Bean
public WebDriver webDriver(DriverFactory factory) { ... }

// 👌 Also works (field injection)
@Autowired
private DriverFactory factory;
@Bean
public WebDriver webDriver() { ... }
```

---

## 🧭 Summary

| Step | Description                                                     |
| ---- | --------------------------------------------------------------- |
| 1️⃣  | Spring scans packages and finds `@Component` classes            |
| 2️⃣  | It creates `DriverFactory` and injects its properties           |
| 3️⃣  | It registers `webDriver()` as a bean definition                 |
| 4️⃣  | When a `WebDriver` is needed, Spring calls `webDriver(factory)` |
| 5️⃣  | The created driver is injected wherever `@Autowired`            |

---

### 💡 The Magic in One Snippet

```java
// You write:
@Bean
public WebDriver webDriver(DriverFactory factory) {
    return factory.createWebDriver(browser);
}

// Spring does:
DriverFactory factory = applicationContext.getBean(DriverFactory.class);
WebDriver driver = webDriverConfig.webDriver(factory);
```

This is **Inversion of Control (IoC)** —
You declare what you need, **Spring provides it**. 🌱✨

---
