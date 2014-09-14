/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.converter.json;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A builder used to create {@link ObjectMapper} instances with a fluent API.
 *
 * <p>Note that Jackson's JSR-310 and Joda-Time support modules will be registered automatically
 * when available (and when Java 8 and Joda-Time themselves are available, respectively).
 *
 * <p>Tested against Jackson 2.2 and 2.3; compatible with Jackson 2.0 and higher.
 *
 * @author Sebastien Deleuze
 * @since 4.1.1
 */
public class Jackson2ObjectMapperBuilder {

	private static final boolean jackson2XmlPresent =
			ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", getClassLoader());

	private ObjectMapper objectMapper;

	private boolean createXmlMapper = false;

	private DateFormat dateFormat;

	private JsonInclude.Include serializationInclusion;

	private AnnotationIntrospector annotationIntrospector;

	private final Map<Class<?>, JsonSerializer<?>>
			serializers = new LinkedHashMap<Class<?>, JsonSerializer<?>>();

	private final Map<Class<?>, JsonDeserializer<?>>
			deserializers = new LinkedHashMap<Class<?>, JsonDeserializer<?>>();

	private final Map<Object, Boolean> features = new HashMap<Object, Boolean>();

	private List<Module> modules;

	private Class<? extends Module>[] modulesToInstall;

	private boolean findModulesViaServiceLoader;

	private PropertyNamingStrategy propertyNamingStrategy;


	private Jackson2ObjectMapperBuilder() {
	}

	private Jackson2ObjectMapperBuilder(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * Obtain a {@link Jackson2ObjectMapperBuilder} instance in order to build an {@link ObjectMapper} instance.
	 */
	public static Jackson2ObjectMapperBuilder json() {
		return new Jackson2ObjectMapperBuilder();
	}

	/**
	 * Obtain a {@link Jackson2ObjectMapperBuilder} instance in order to build a {@link XmlMapper} instance.
	 */
	@SuppressWarnings("unchecked")
	public static Jackson2ObjectMapperBuilder xml() {
		return new Jackson2ObjectMapperBuilder().createXmlMapper(true);
	}

	/**
	 * Obtain a {@link Jackson2ObjectMapperBuilder} in order to customize the {@link ObjectMapper} parameter.
	 */
	public static Jackson2ObjectMapperBuilder instance(ObjectMapper objectMapper) {
		return new Jackson2ObjectMapperBuilder(objectMapper);
	}

	private static ClassLoader getClassLoader() {
		ClassLoader classLoader  = ClassUtils.getDefaultClassLoader();
		Assert.state(classLoader != null, "No classloader available");
		return classLoader;
	}

	/**
	 * If set to true and no custom {@link ObjectMapper} has been set, a {@link XmlMapper}
	 * will be created using its default constructor.
	 */
	public Jackson2ObjectMapperBuilder createXmlMapper(boolean createXmlMapper) {
		this.createXmlMapper = createXmlMapper;
		return this;
	}

	/**
	 * Define the format for date/time with the given {@link DateFormat}.
	 * <p>Note: Setting this property makes the exposed {@link ObjectMapper}
	 * non-thread-safe, according to Jackson's thread safety rules.
	 * @see #simpleDateFormat(String)
	 */
	public Jackson2ObjectMapperBuilder dateFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
		return this;
	}

	/**
	 * Define the date/time format with a {@link SimpleDateFormat}.
	 * <p>Note: Setting this property makes the exposed {@link ObjectMapper}
	 * non-thread-safe, according to Jackson's thread safety rules.
	 * @see #dateFormat(DateFormat)
	 */
	public Jackson2ObjectMapperBuilder simpleDateFormat(String format) {
		this.dateFormat = new SimpleDateFormat(format);
		return this;
	}

	/**
	 * Set an {@link AnnotationIntrospector} for both serialization and deserialization.
	 */
	public Jackson2ObjectMapperBuilder annotationIntrospector(AnnotationIntrospector annotationIntrospector) {
		this.annotationIntrospector = annotationIntrospector;
		return this;
	}

	/**
	 * Set a custom inclusion strategy for serialization.
	 * @see com.fasterxml.jackson.annotation.JsonInclude.Include
	 */
	public Jackson2ObjectMapperBuilder serializationInclusion(JsonInclude.Include serializationInclusion) {
		this.serializationInclusion = serializationInclusion;
		return this;
	}

	/**
	 * Configure custom serializers. Each serializer is registered for the type
	 * returned by {@link JsonSerializer#handledType()}, which must not be
	 * {@code null}.
	 * @see #serializersByType(Map)
	 */
	public Jackson2ObjectMapperBuilder serializers(JsonSerializer<?>... serializers) {
		if (serializers != null) {
			for (JsonSerializer<?> serializer : serializers) {
				Class<?> handledType = serializer.handledType();
				Assert.isTrue(handledType != null && handledType != Object.class,
						"Unknown handled type in " + serializer.getClass().getName());
				this.serializers.put(serializer.handledType(), serializer);
			}
		}
		return this;
	}

	/**
	 * Configure custom serializers for the given types.
	 * @see #serializers(JsonSerializer...)
	 */
	public Jackson2ObjectMapperBuilder serializersByType(Map<Class<?>, JsonSerializer<?>> serializers) {
		if (serializers != null) {
			this.serializers.putAll(serializers);
		}
		return this;
	}

	/**
	 * Configure custom deserializers for the given types.
	 */
	public Jackson2ObjectMapperBuilder deserializersByType(Map<Class<?>, JsonDeserializer<?>> deserializers) {
		if (deserializers != null) {
			this.deserializers.putAll(deserializers);
		}
		return this;
	}

	/**
	 * Shortcut for {@link MapperFeature#AUTO_DETECT_FIELDS} option.
	 */
	public Jackson2ObjectMapperBuilder autoDetectFields(boolean autoDetectFields) {
		this.features.put(MapperFeature.AUTO_DETECT_FIELDS, autoDetectFields);
		return this;
	}

	/**
	 * Shortcut for {@link MapperFeature#AUTO_DETECT_SETTERS}/
	 * {@link MapperFeature#AUTO_DETECT_GETTERS} option.
	 */
	public Jackson2ObjectMapperBuilder autoDetectGettersSetters(boolean autoDetectGettersSetters) {
		this.features.put(MapperFeature.AUTO_DETECT_GETTERS, autoDetectGettersSetters);
		this.features.put(MapperFeature.AUTO_DETECT_SETTERS, autoDetectGettersSetters);
		return this;
	}

	/**
	 * Shortcut for {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} option.
	 */
	public Jackson2ObjectMapperBuilder failOnUnknownProperties(boolean failOnUnknownProperties) {
		this.features.put(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknownProperties);
		return this;
	}

	/**
	 * Shortcut for {@link MapperFeature#DEFAULT_VIEW_INCLUSION} option.
	 */
	public Jackson2ObjectMapperBuilder defaultViewInclusion(boolean defaultViewInclusion) {
		this.features.put(MapperFeature.DEFAULT_VIEW_INCLUSION, defaultViewInclusion);
		return this;
	}

	/**
	 * Shortcut for {@link SerializationFeature#FAIL_ON_EMPTY_BEANS} option.
	 */
	public Jackson2ObjectMapperBuilder failOnEmptyBeans(boolean failOnEmptyBeans) {
		this.features.put(SerializationFeature.FAIL_ON_EMPTY_BEANS, failOnEmptyBeans);
		return this;
	}

	/**
	 * Shortcut for {@link SerializationFeature#INDENT_OUTPUT} option.
	 */
	public Jackson2ObjectMapperBuilder indentOutput(boolean indentOutput) {
		this.features.put(SerializationFeature.INDENT_OUTPUT, indentOutput);
		return this;
	}

	/**
	 * Specify features to enable.
	 * @see com.fasterxml.jackson.core.JsonParser.Feature
	 * @see com.fasterxml.jackson.core.JsonGenerator.Feature
	 * @see com.fasterxml.jackson.databind.SerializationFeature
	 * @see com.fasterxml.jackson.databind.DeserializationFeature
	 * @see com.fasterxml.jackson.databind.MapperFeature
	 */
	public Jackson2ObjectMapperBuilder featuresToEnable(Object... featuresToEnable) {
		if (featuresToEnable != null) {
			for (Object feature : featuresToEnable) {
				this.features.put(feature, Boolean.TRUE);
			}
		}
		return this;
	}

	/**
	 * Specify features to disable.
	 * @see com.fasterxml.jackson.core.JsonParser.Feature
	 * @see com.fasterxml.jackson.core.JsonGenerator.Feature
	 * @see com.fasterxml.jackson.databind.SerializationFeature
	 * @see com.fasterxml.jackson.databind.DeserializationFeature
	 * @see com.fasterxml.jackson.databind.MapperFeature
	 */
	public Jackson2ObjectMapperBuilder featuresToDisable(Object... featuresToDisable) {
		if (featuresToDisable != null) {
			for (Object feature : featuresToDisable) {
				this.features.put(feature, Boolean.FALSE);
			}
		}
		return this;
	}

	/**
	 * Set a complete list of modules to be registered with the {@link ObjectMapper}.
	 * <p>Note: If this is set, no finding of modules is going to happen - not by
	 * Jackson, and not by Spring either (see {@link #findModulesViaServiceLoader}).
	 * As a consequence, specifying an empty list here will suppress any kind of
	 * module detection.
	 * <p>Specify either this or {@link #modulesToInstall}, not both.
	 * @see com.fasterxml.jackson.databind.Module
	 */
	public Jackson2ObjectMapperBuilder modules(List<Module> modules) {
		this.modules = new LinkedList<Module>(modules);
		return this;
	}

	/**
	 * Specify one or more modules by class (or class name in XML),
	 * to be registered with the {@link ObjectMapper}.
	 * <p>Modules specified here will be registered in combination with
	 * Spring's autodetection of JSR-310 and Joda-Time, or Jackson's
	 * finding of modules (see {@link #findModulesViaServiceLoader}).
	 * <p>Specify either this or {@link #modules}, not both.
	 * @see com.fasterxml.jackson.databind.Module
	 */
	public Jackson2ObjectMapperBuilder modulesToInstall(Class<? extends Module>... modules) {
		this.modulesToInstall = modules;
		return this;
	}

	/**
	 * Set whether to let Jackson find available modules via the JDK ServiceLoader,
	 * based on META-INF metadata in the classpath. Requires Jackson 2.2 or higher.
	 * <p>If this mode is not set, Spring's Jackson2ObjectMapperBuilder itself
	 * will try to find the JSR-310 and Joda-Time support modules on the classpath -
	 * provided that Java 8 and Joda-Time themselves are available, respectively.
	 * @see com.fasterxml.jackson.databind.ObjectMapper#findModules()
	 */
	public Jackson2ObjectMapperBuilder findModulesViaServiceLoader(boolean findModules) {
		this.findModulesViaServiceLoader = findModules;
		return this;
	}

	/**
	 * Specify a {@link com.fasterxml.jackson.databind.PropertyNamingStrategy} to
	 * configure the {@link ObjectMapper} with.
	 */
	public Jackson2ObjectMapperBuilder propertyNamingStrategy(PropertyNamingStrategy propertyNamingStrategy) {
		this.propertyNamingStrategy = propertyNamingStrategy;
		return this;
	}

	/**
	 * Build a new {@link T} instance.
	 */
	@SuppressWarnings("unchecked")
	public <T extends ObjectMapper> T build() {
		if (this.objectMapper == null) {
			if(this.createXmlMapper) {
				ClassLoader cl = getClassLoader();
				try {
					Class<? extends ObjectMapper> xmlMapper = (Class<? extends ObjectMapper>)
							cl.loadClass("com.fasterxml.jackson.dataformat.xml.XmlMapper");
					this.objectMapper = BeanUtils.instantiate(xmlMapper);
				}
				catch (ClassNotFoundException ex) {
					throw new IllegalStateException("Could not instantiate XmlMapper, it has not been found on the classpath");
				}
			}
			else {
				this.objectMapper = new ObjectMapper();
			}
		}

		if (this.dateFormat != null) {
			this.objectMapper.setDateFormat(this.dateFormat);
		}

		if (this.annotationIntrospector != null) {
			this.objectMapper.setAnnotationIntrospector(this.annotationIntrospector);
		}

		if (this.serializationInclusion != null) {
			this.objectMapper.setSerializationInclusion(this.serializationInclusion);
		}

		if (!this.serializers.isEmpty() || !this.deserializers.isEmpty()) {
			SimpleModule module = new SimpleModule();
			addSerializers(module);
			addDeserializers(module);
			this.objectMapper.registerModule(module);
		}

		for (Object feature : this.features.keySet()) {
			configureFeature(feature, this.features.get(feature));
		}

		if (this.modules != null) {
			// Complete list of modules given
			for (Module module : this.modules) {
				// Using Jackson 2.0+ registerModule method, not Jackson 2.2+ registerModules
				this.objectMapper.registerModule(module);
			}
		}
		else {
			// Combination of modules by class names specified and class presence in the classpath
			if (this.modulesToInstall != null) {
				for (Class<? extends Module> module : this.modulesToInstall) {
					this.objectMapper.registerModule(BeanUtils.instantiate(module));
				}
			}
			if (this.findModulesViaServiceLoader) {
				// Jackson 2.2+
				this.objectMapper.registerModules(ObjectMapper.findModules(getClassLoader()));
			}
			else {
				registerWellKnownModulesIfAvailable();
			}
		}

		if (this.propertyNamingStrategy != null) {
			this.objectMapper.setPropertyNamingStrategy(this.propertyNamingStrategy);
		}

		return (T)this.objectMapper;
	}

	@SuppressWarnings("unchecked")
	private <T> void addSerializers(SimpleModule module) {
		for (Class<?> type : this.serializers.keySet()) {
			module.addSerializer((Class<? extends T>) type, (JsonSerializer<T>) this.serializers.get(type));
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void addDeserializers(SimpleModule module) {
		for (Class<?> type : this.deserializers.keySet()) {
			module.addDeserializer((Class<T>) type, (JsonDeserializer<? extends T>) this.deserializers.get(type));
		}
	}

	private void configureFeature(Object feature, boolean enabled) {
		if (feature instanceof JsonParser.Feature) {
			this.objectMapper.configure((JsonParser.Feature) feature, enabled);
		}
		else if (feature instanceof JsonGenerator.Feature) {
			this.objectMapper.configure((JsonGenerator.Feature) feature, enabled);
		}
		else if (feature instanceof SerializationFeature) {
			this.objectMapper.configure((SerializationFeature) feature, enabled);
		}
		else if (feature instanceof DeserializationFeature) {
			this.objectMapper.configure((DeserializationFeature) feature, enabled);
		}
		else if (feature instanceof MapperFeature) {
			this.objectMapper.configure((MapperFeature) feature, enabled);
		}
		else {
			throw new FatalBeanException("Unknown feature class: " + feature.getClass().getName());
		}
	}

	@SuppressWarnings("unchecked")
	private void registerWellKnownModulesIfAvailable() {
		ClassLoader cl =  getClassLoader();
		// Java 8 java.time package present?
		if (ClassUtils.isPresent("java.time.LocalDate", cl)) {
			try {
				Class<? extends Module> jsr310Module = (Class<? extends Module>)
						cl.loadClass("com.fasterxml.jackson.datatype.jsr310.JSR310Module");
				this.objectMapper.registerModule(BeanUtils.instantiate(jsr310Module));
			}
			catch (ClassNotFoundException ex) {
				// jackson-datatype-jsr310 not available
			}
		}
		// Joda-Time present?
		if (ClassUtils.isPresent("org.joda.time.LocalDate", cl)) {
			try {
				Class<? extends Module> jodaModule = (Class<? extends Module>)
						cl.loadClass("com.fasterxml.jackson.datatype.joda.JodaModule");
				this.objectMapper.registerModule(BeanUtils.instantiate(jodaModule));
			}
			catch (ClassNotFoundException ex) {
				// jackson-datatype-joda not available
			}
		}
	}

}
