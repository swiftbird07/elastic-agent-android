package de.swiftbird.elasticandroid;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ComponentFactory {
    private static final Map<String, Supplier<?>> components = new HashMap<>();

    static {
        // New components can be added here
        components.put("android://self-log", SelfLogComp::new);
        components.put("android://security-logs", SecurityLogsComp::new);
        components.put("android://network-logs", NetworkLogsComp::new);
        components.put("android://location", LocationComp::new);
    }

    public static Component createInstance(String key) {
        Supplier<?> supplier = components.get(key);
        if (supplier != null) {
            return (Component) supplier.get();
        }
        throw new IllegalArgumentException("Component with key " + key + " not found.");
    }

    public static Component[] getAllInstances() {
        return components.values().stream().map(Supplier::get).toArray(Component[]::new);
    }
}