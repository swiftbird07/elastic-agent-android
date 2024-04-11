package de.swiftbird.elasticandroid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Manages the creation and retrieval of {@link Component} instances based on URIs defined in the fleet agent policy response.
 * Utilizes a registry pattern where components are registered with a unique key (URI scheme), allowing for dynamic instantiation
 * based on administrative configurations. This approach supports flexible component activation and configuration through policy settings.
 */
public class ComponentFactory {
    private static final Map<String, Supplier<?>> components = new ConcurrentHashMap<>();

    static {
        // Initial component registration.
        components.put("android://self-log", SelfLogComp::getInstance);
        components.put("android://security-logs", SecurityLogsComp::getInstance);
        components.put("android://network-logs", NetworkLogsComp::getInstance);
        components.put("android://location", LocationComp::getInstance);
        // Additional components can be registered here following the URI scheme.
    }

    /**
     * Creates an instance of a {@link Component} based on the specified key.
     * The key corresponds to a URI that not only identifies the component type but can also include configuration directives.
     *
     * @param key The unique key (URI) identifying the component type only (URI until the first colon, e.g., "android://location").
     * @return An instance of the requested Component.
     * @throws IllegalArgumentException If no component is registered under the specified key.
     */
    public static Component createInstance(String key) {
        Supplier<?> supplier = components.get(key);
        if (supplier != null) {
            return (Component) supplier.get();
        }
        throw new IllegalArgumentException("Component with key " + key + " not found.");
    }

    /**
     * Retrieves instances of all registered components.
     * This method can be used to initialize or manage all available components dynamically, based on the current policy configuration.
     *
     * @return An array of all Component instances currently registered.
     */
    public static Component[] getAllInstances() {
        List<Component> list = new ArrayList<>();
        for (Supplier<?> supplier : components.values()) {
            Component o = (Component) supplier.get();
            list.add(o);
        }
        return list.toArray(new Component[0]);
    }

    /**
     * Registers a new component with the factory, associating it with a unique key.
     * The key should be a URI scheme that identifies the component type and can include configuration directives.
     *
     * @param key      The unique key (URI) identifying the component type only (URI until the first colon, e.g., "android://location").
     * @param supplier A supplier function that creates a new instance of the component.
     */
    public static void registerComponent(String key, Supplier<Component> supplier) {
        components.put(key, supplier);
    }
}
