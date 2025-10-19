package org.spring.bdd.configs;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A thread-safe context for storing scenario state.
 *
 * This class acts as a singleton but uses a ThreadLocal<Map> internally to ensure
 * that each test execution thread has its own isolated storage. This prevents
 * state from bleeding between scenarios running in parallel.
 *
 * This pattern is useful for understanding thread-safe state management but is
 * often superseded by Spring's "cucumber-glue" scope in pure Cucumber-Spring projects.
 */
@Component // Make it a singleton bean, managed by Spring
public class TestContext {

    // The core of the implementation: A ThreadLocal that holds a Map.
    // Each thread gets its own independent copy of the Map.
    private static final ThreadLocal<Map<String, Object>> context = ThreadLocal.withInitial(HashMap::new);

    /**
     * Retrieves the state map for the current thread.
     * @return A Map unique to the current thread.
     */
    private Map<String, Object> getContext() {
        return context.get();
    }

    /**
     * Stores a value in the current thread's context.
     * @param key The key to identify the value.
     * @param value The value to store.
     */
    public void setValue(String key, Object value) {
        getContext().put(key, value);
    }

    /**
     * Retrieves a value from the current thread's context.
     * @param key The key of the value to retrieve.
     * @return The retrieved object, or null if not found.
     */
    public Object getValue(String key) {
        return getContext().get(key);
    }

    /**
     * Retrieves a value and casts it to the specified type for convenience.
     * @param key The key of the value to retrieve.
     * @param type The class to cast the value to.
     * @return The retrieved and casted object.
     * @throws ClassCastException if the object cannot be cast to the specified type.
     * @throws NullPointerException if the object is not found for the given key.
     */
    public <T> T getValue(String key, Class<T> type) {
        Object value = getValue(key);
        Objects.requireNonNull(value, "No value found in context for key: " + key);
        return type.cast(value);
    }

    /**
     * Checks if a key exists in the current thread's context.
     * @param key The key to check.
     * @return true if the key exists, false otherwise.
     */
    public boolean containsKey(String key) {
        return getContext().containsKey(key);
    }

    /**
     * CRITICAL: Clears the context for the current thread.
     * This must be called after each scenario to prevent memory leaks and state bleeding.
     */
    public void cleanup() {
        context.remove();
    }
}
