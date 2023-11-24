package org.springframework.cglib.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cglib.core.Customizer;
import org.springframework.cglib.core.KeyFactoryCustomizer;

@SuppressWarnings({"rawtypes", "unchecked"})
public class CustomizerRegistry {
    private final Class[] customizerTypes;
    private Map<Class, List<KeyFactoryCustomizer>> customizers = new HashMap<>();

    public CustomizerRegistry(Class[] customizerTypes) {
        this.customizerTypes = customizerTypes;
    }

    public void add(KeyFactoryCustomizer customizer) {
        Class<? extends KeyFactoryCustomizer> klass = customizer.getClass();
        for (Class type : customizerTypes) {
            if (type.isAssignableFrom(klass)) {
                List<KeyFactoryCustomizer> list = customizers.computeIfAbsent(type, k -> new ArrayList<>());
                list.add(customizer);
            }
        }
    }

    public <T> List<T> get(Class<T> klass) {
        List<KeyFactoryCustomizer> list = customizers.get(klass);
        if (list == null) {
            return Collections.emptyList();
        }
        return (List<T>) list;
    }

    /**
     * @deprecated Only to keep backward compatibility.
     */
    @Deprecated
    public static CustomizerRegistry singleton(Customizer customizer)
    {
        CustomizerRegistry registry = new CustomizerRegistry(new Class[]{Customizer.class});
        registry.add(customizer);
        return registry;
    }
}
