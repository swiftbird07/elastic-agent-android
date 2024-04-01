package de.swiftbird.elasticandroid;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ComponentFactory {
    private static final Map<String, Supplier<?>> components = new HashMap<>();

    static {
        // New components can be added here
        components.put("android://self-log", SelfLogComp::new);
    }

    public static Component createInstance(String key) {
        Supplier<?> supplier = components.get(key);
        if (supplier != null) {
            return (Component) supplier.get();
        }
        throw new IllegalArgumentException("Component with key " + key + " not found.");
    }
}