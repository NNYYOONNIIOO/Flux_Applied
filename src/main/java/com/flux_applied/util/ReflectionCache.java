package com.flux_applied.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache for reflection Method objects to avoid repeated lookup.
 */
public class ReflectionCache {

    private static final Map<String, Method> methodCache = new HashMap<>();

    /**
     * Get a cached Method object, looking it up if not already cached.
     */
    public static Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        String key = clazz.getName() + "#" + name;
        return methodCache.computeIfAbsent(key, k -> {
            try {
                Method m = clazz.getMethod(name, parameterTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                return null;
            }
        });
    }

    /**
     * Invoke a cached method. Returns null on failure.
     */
    public static Object invokeMethod(Method method, Object obj, Object... args) {
        if (method == null) return null;
        try {
            return method.invoke(obj, args);
        } catch (Exception e) {
            return null;
        }
    }
}
