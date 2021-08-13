/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import java.util.Map;

import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MissingRequiredPropertiesException;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.Profiles;

/**
 * Lazy ConfigurableEnvironment for reduce unnecessary createEnvironment.
 *
 * @author qxo
 * 
 * @since August 14, 2021
 *
 */
public class LazyConfigurableEnvironment implements ConfigurableEnvironment {

    private final AbstractApplicationContext context;
    private ConfigurableEnvironment environment;

    public LazyConfigurableEnvironment(AbstractApplicationContext context) {
        super();
        this.context = context;
    }

    protected final ConfigurableEnvironment getEnvironment() {
        if (environment == null) {
            environment = context.createEnvironment();
        }
        return environment;
    }


    public boolean containsProperty(String key) {
       return getEnvironment().containsProperty(key);
    }

    public String getProperty(String key) {
       return getEnvironment().getProperty(key);
    }

    public ConfigurableConversionService getConversionService() {
       return getEnvironment().getConversionService();
    }

    public String getProperty(String key, String defaultValue) {
       return getEnvironment().getProperty(key, defaultValue);
    }

    public void setConversionService(ConfigurableConversionService conversionService) {
        getEnvironment().setConversionService(conversionService);
    }

    public <T> T getProperty(String key, Class<T> targetType) {
       return getEnvironment().getProperty(key, targetType);
    }

    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
       return getEnvironment().getProperty(key, targetType, defaultValue);
    }

    public void setPlaceholderPrefix(String placeholderPrefix) {
        getEnvironment().setPlaceholderPrefix(placeholderPrefix);
    }

    public String getRequiredProperty(String key) throws IllegalStateException {
       return getEnvironment().getRequiredProperty(key);
    }

    public void setPlaceholderSuffix(String placeholderSuffix) {
        getEnvironment().setPlaceholderSuffix(placeholderSuffix);
    }

    public void setValueSeparator(String valueSeparator) {
        getEnvironment().setValueSeparator(valueSeparator);
    }

    public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
       return getEnvironment().getRequiredProperty(key, targetType);
    }

    public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
        getEnvironment().setIgnoreUnresolvableNestedPlaceholders(ignoreUnresolvableNestedPlaceholders);
    }

    public String resolvePlaceholders(String text) {
       return getEnvironment().resolvePlaceholders(text);
    }

    public void setActiveProfiles(String... profiles) {
        getEnvironment().setActiveProfiles(profiles);
    }

    public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
       return getEnvironment().resolveRequiredPlaceholders(text);
    }

    public void setRequiredProperties(String... requiredProperties) {
        getEnvironment().setRequiredProperties(requiredProperties);
    }

    public String[] getActiveProfiles() {
       return getEnvironment().getActiveProfiles();
    }

    public void validateRequiredProperties() throws MissingRequiredPropertiesException {
        getEnvironment().validateRequiredProperties();
    }

    public void addActiveProfile(String profile) {
        getEnvironment().addActiveProfile(profile);
    }

    public void setDefaultProfiles(String... profiles) {
        getEnvironment().setDefaultProfiles(profiles);
    }

    public String[] getDefaultProfiles() {
       return getEnvironment().getDefaultProfiles();
    }

    public MutablePropertySources getPropertySources() {
       return getEnvironment().getPropertySources();
    }

    public boolean acceptsProfiles(String... profiles) {
       return getEnvironment().acceptsProfiles(profiles);
    }

    public Map<String, Object> getSystemProperties() {
       return getEnvironment().getSystemProperties();
    }

    public boolean acceptsProfiles(Profiles profiles) {
       return getEnvironment().acceptsProfiles(profiles);
    }

    public Map<String, Object> getSystemEnvironment() {
       return getEnvironment().getSystemEnvironment();
    }

    public void merge(ConfigurableEnvironment parent) {
        getEnvironment().merge(parent);
    }
}
