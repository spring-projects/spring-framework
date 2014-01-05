/*
 * Copyright 2002-2013 the original author or authors.
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
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * A {@link FactoryBean} for creating a Jackson 2.x {@link ObjectMapper} with setters
 * to enable or disable Jackson features from within XML configuration.
 *
 * <p>Example usage with
 * {@link org.springframework.http.converter.json.MappingJackson2HttpMessageConverter}:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.http.converter.json.MappingJackson2HttpMessageConverter">
 *   &lt;property name="objectMapper">
 *     &lt;bean class="org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean"
 *       p:autoDetectFields="false"
 *       p:autoDetectGettersSetters="false"
 *       p:annotationIntrospector-ref="jaxbAnnotationIntrospector" />
 *   &lt;/property>
 * &lt;/bean>
 * </pre>
 *
 * <p>Example usage with MappingJackson2JsonView:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.web.servlet.view.json.MappingJackson2JsonView">
 *   &lt;property name="objectMapper">
 *     &lt;bean class="org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean"
 *       p:failOnEmptyBeans="false"
 *       p:indentOutput="true">
 *       &lt;property name="serializers">
 *         &lt;array>
 *           &lt;bean class="org.mycompany.MyCustomSerializer" />
 *         &lt;/array>
 *       &lt;/property>
 *     &lt;/bean>
 *   &lt;/property>
 * &lt;/bean>
 * </pre>
 *
 * <p>In case there are no specific setters provided (for some rarely used options),
 * you can still use the more general methods  {@link #setFeaturesToEnable} and
 * {@link #setFeaturesToDisable}.
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean">
 *   &lt;property name="featuresToEnable">
 *     &lt;array>
 *       &lt;util:constant static-field="com.fasterxml.jackson.databind.SerializationFeature$WRAP_ROOT_VALUE"/>
 *       &lt;util:constant static-field="com.fasterxml.jackson.databind.SerializationFeature$CLOSE_CLOSEABLE"/>
 *     &lt;/array>
 *   &lt;/property>
 *   &lt;property name="featuresToDisable">
 *     &lt;array>
 *       &lt;util:constant static-field="com.fasterxml.jackson.databind.MapperFeature$USE_ANNOTATIONS"/>
 *     &lt;/array>
 *   &lt;/property>
 * &lt;/bean>
 * </pre>
 *
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class Jackson2ObjectMapperFactoryBean implements FactoryBean<ObjectMapper>, InitializingBean {

	private ObjectMapper objectMapper;

	private DateFormat dateFormat;

	private AnnotationIntrospector annotationIntrospector;

	private final Map<Class<?>, JsonSerializer<?>> serializers = new LinkedHashMap<Class<?>, JsonSerializer<?>>();

	private final Map<Class<?>, JsonDeserializer<?>> deserializers = new LinkedHashMap<Class<?>, JsonDeserializer<?>>();

	private final Map<Object, Boolean> features = new HashMap<Object, Boolean>();


	/**
	 * Set the ObjectMapper instance to use. If not set, the ObjectMapper will
	 * be created using its default constructor.
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * Define the format for date/time with the given {@link DateFormat}.
	 * <p>Note: Setting this property makes the exposed {@link ObjectMapper}
	 * non-thread-safe, according to Jackson's thread safety rules.
	 * @see #setSimpleDateFormat(String)
	 */
	public void setDateFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}

	/**
	 * Define the date/time format with a {@link SimpleDateFormat}.
	 * <p>Note: Setting this property makes the exposed {@link ObjectMapper}
	 * non-thread-safe, according to Jackson's thread safety rules.
	 * @see #setDateFormat(DateFormat)
	 */
	public void setSimpleDateFormat(String format) {
		this.dateFormat = new SimpleDateFormat(format);
	}

	/**
	 * Set an {@link AnnotationIntrospector} for both serialization and deserialization.
	 */
	public void setAnnotationIntrospector(AnnotationIntrospector annotationIntrospector) {
		this.annotationIntrospector = annotationIntrospector;
	}

	/**
	 * Configure custom serializers. Each serializer is registered for the type
	 * returned by {@link JsonSerializer#handledType()}, which must not be
	 * {@code null}.
	 * @see #setSerializersByType(Map)
	 */
	public void setSerializers(JsonSerializer<?>... serializers) {
		if (serializers != null) {
			for (JsonSerializer<?> serializer : serializers) {
				Class<?> handledType = serializer.handledType();
				Assert.isTrue(handledType != null && handledType != Object.class,
						"Unknown handled type in " + serializer.getClass().getName());
				this.serializers.put(serializer.handledType(), serializer);
			}
		}
	}

	/**
	 * Configure custom serializers for the given types.
	 * @see #setSerializers(JsonSerializer...)
	 */
	public void setSerializersByType(Map<Class<?>, JsonSerializer<?>> serializers) {
		if (serializers != null) {
			this.serializers.putAll(serializers);
		}
	}

	/**
	 * Configure custom deserializers for the given types.
	 */
	public void setDeserializersByType(Map<Class<?>, JsonDeserializer<?>> deserializers) {
		if (deserializers != null) {
			this.deserializers.putAll(deserializers);
		}
	}

	/**
	 * Shortcut for {@link MapperFeature#AUTO_DETECT_FIELDS} option.
	 */
	public void setAutoDetectFields(boolean autoDetectFields) {
		this.features.put(MapperFeature.AUTO_DETECT_FIELDS, autoDetectFields);
	}

	/**
	 * Shortcut for {@link MapperFeature#AUTO_DETECT_SETTERS}/
	 * {@link MapperFeature#AUTO_DETECT_GETTERS} option.
	 */
	public void setAutoDetectGettersSetters(boolean autoDetectGettersSetters) {
		this.features.put(MapperFeature.AUTO_DETECT_GETTERS, autoDetectGettersSetters);
		this.features.put(MapperFeature.AUTO_DETECT_SETTERS, autoDetectGettersSetters);
	}

	/**
	 * Shortcut for {@link SerializationFeature#FAIL_ON_EMPTY_BEANS} option.
	 */
	public void setFailOnEmptyBeans(boolean failOnEmptyBeans) {
		this.features.put(SerializationFeature.FAIL_ON_EMPTY_BEANS, failOnEmptyBeans);
	}

	/**
	 * Shortcut for {@link SerializationFeature#INDENT_OUTPUT} option.
	 */
	public void setIndentOutput(boolean indentOutput) {
		this.features.put(SerializationFeature.INDENT_OUTPUT, indentOutput);
	}

	/**
	 * Specify features to enable.
	 * @see com.fasterxml.jackson.core.JsonParser.Feature
	 * @see com.fasterxml.jackson.core.JsonGenerator.Feature
	 * @see com.fasterxml.jackson.databind.SerializationFeature
	 * @see com.fasterxml.jackson.databind.DeserializationFeature
	 * @see com.fasterxml.jackson.databind.MapperFeature
	 */
	public void setFeaturesToEnable(Object... featuresToEnable) {
		if (featuresToEnable != null) {
			for (Object feature : featuresToEnable) {
				this.features.put(feature, Boolean.TRUE);
			}
		}
	}

	/**
	 * Specify features to disable.
	 * @see com.fasterxml.jackson.core.JsonParser.Feature
	 * @see com.fasterxml.jackson.core.JsonGenerator.Feature
	 * @see com.fasterxml.jackson.databind.SerializationFeature
	 * @see com.fasterxml.jackson.databind.DeserializationFeature
	 * @see com.fasterxml.jackson.databind.MapperFeature
	 */
	public void setFeaturesToDisable(Object... featuresToDisable) {
		if (featuresToDisable != null) {
			for (Object feature : featuresToDisable) {
				this.features.put(feature, Boolean.FALSE);
			}
		}
	}


	public void afterPropertiesSet() {
		if (this.objectMapper == null) {
			this.objectMapper = new ObjectMapper();
		}

		if (this.dateFormat != null) {
			this.objectMapper.setDateFormat(this.dateFormat);
		}

		if (this.annotationIntrospector != null) {
			this.objectMapper.setAnnotationIntrospector(this.annotationIntrospector);
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


	/**
	 * Return the singleton ObjectMapper.
	 */
	public ObjectMapper getObject() {
		return this.objectMapper;
	}

	public Class<?> getObjectType() {
		return ObjectMapper.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
