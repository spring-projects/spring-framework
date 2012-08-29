/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.context.support;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
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
import org.springframework.util.CollectionUtils;

/**
 * A FactoryBean for creating a Jackson {@link ObjectMapper} with setters to
 * enable or disable Jackson features from within XML configuration.
 * 
 * <p>
 * Example usage with
 * {@link org.springframework.http.converter.json.MappingJackson2HttpMessageConverter}
 * :
 * </p>
 * 
 * <pre>
 * &lt;bean class="org.springframework.http.converter.json.MappingJackson2HttpMessageConverter">
 *   &lt;property name="objectMapper">
 *     &lt;bean class="org.springframework.web.context.support.Jackson2ObjectMapperFactoryBean"
 *       p:autoDetectFields="false"
 *       p:autoDetectGettersSetters="false"
 *       p:annotationIntrospector-ref="jaxbAnnotationIntrospector" />
 *   &lt;/property>
 * &lt;/bean>
 * </pre>
 * 
 * <p>
 * Example usage with
 * {@link org.springframework.web.servlet.view.json.MappingJackson2JsonView}:
 * </p>
 * 
 * <pre>
 * &lt;bean class="org.springframework.web.servlet.view.json.MappingJackson2JsonView">
 *   &lt;property name="objectMapper">
 *     &lt;bean class="org.springframework.web.context.support.Jackson2ObjectMapperFactoryBean"
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
 * <p>
 * In case there are no specific setters provided (for some rarely used
 * options), you can still use the more general methods
 * {@link #setFeaturesToEnable(Object[])} and
 * {@link #setFeaturesToDisable(Object[])}.
 * 
 * <pre>
 * &lt;bean class="org.springframework.web.context.support.Jackson2ObjectMapperFactoryBean">
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
 * <p>
 * Note: This BeanFctory is singleton, so if you need more than one you'll need
 * to configure multiple instances.
 * 
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 * @author Rossen Stoyanchev
 * 
 * @since 3.2
 */
public class Jackson2ObjectMapperFactoryBean implements FactoryBean<ObjectMapper>, InitializingBean {

	private ObjectMapper objectMapper;

	private Map<Object, Boolean> features = new HashMap<Object, Boolean>();

	private DateFormat dateFormat;

	private AnnotationIntrospector annotationIntrospector;

	private JsonSerializer<?>[] serializers;

	private Map<Class<?>, JsonDeserializer<?>> deserializers;

	/**
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	public ObjectMapper getObject() {
		return objectMapper;
	}

	/**
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	public Class<?> getObjectType() {
		return ObjectMapper.class;
	}

	/**
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
	public boolean isSingleton() {
		return true;
	}

	/**
	 * Set the ObjectMapper instance to use. If not set an instance will be
	 * created using the default constructor.
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * Define the date/time format with the given string, which is in turn used
	 * to create a {@link SimpleDateFormat}.
	 * @see #setDateFormat(DateFormat)
	 */
	public void setSimpleDateFormat(String format) {
		this.dateFormat = new SimpleDateFormat(format);
	}

	/**
	 * Define the format for date/time with the given {@link DateFormat}
	 * instance.
	 * 
	 * @see #setSimpleDateFormat(String)
	 */
	public void setDateFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}

	/**
	 * Define {@link AnnotationIntrospector} for both serializer and
	 * deserializer.
	 */
	public void setAnnotationIntrospector(AnnotationIntrospector annotationIntrospector) {
		this.annotationIntrospector = annotationIntrospector;
	}

	/**
	 * Use custom serializers. Each serializer should correctly implement
	 * {@link JsonSerializer#handledType()}.
	 */
	public void setSerializers(JsonSerializer<?>... serializers) {
		this.serializers = serializers;
	}

	/**
	 * Use custom deserializers binded to given types.
	 */
	public void setDeserializers(Map<Class<?>, JsonDeserializer<?>> deserializers) {
		this.deserializers = deserializers;
	}

	/**
	 * Specify features to enable.
	 * 
	 * @see MapperFeature
	 * @see SerializationFeature
	 * @see DeserializationFeature
	 * @see JsonParser.Feature
	 * @see JsonGenerator.Feature
	 */
	public void setFeaturesToEnable(Object... featuresToEnable) {
		if (featuresToEnable == null) {
			throw new FatalBeanException("featuresToEnable property should not be null");
		}

		for (Object feature : featuresToEnable) {
			features.put(feature, Boolean.TRUE);
		}
	}

	/**
	 * Specify features to disable.
	 * 
	 * @see MapperFeature
	 * @see SerializationFeature
	 * @see DeserializationFeature
	 * @see JsonParser.Feature
	 * @see JsonGenerator.Feature
	 */
	public void setFeaturesToDisable(Object... featuresToDisable) {
		if (featuresToDisable == null) {
			throw new FatalBeanException("featuresToDisable property should not be null");
		}

		for (Object feature : featuresToDisable) {
			features.put(feature, Boolean.FALSE);
		}
	}

	/**
	 * Shortcut for {@link MapperFeature#AUTO_DETECT_FIELDS} option.
	 */
	public void setAutoDetectFields(boolean autoDetectFields) {
		features.put(MapperFeature.AUTO_DETECT_FIELDS, Boolean.valueOf(autoDetectFields));
	}

	/**
	 * Shortcut for {@link MapperFeature#AUTO_DETECT_SETTERS}/
	 * {@link MapperFeature#AUTO_DETECT_GETTERS} option.
	 */
	public void setAutoDetectGettersSetters(boolean autoDetectGettersSetters) {
		features.put(MapperFeature.AUTO_DETECT_SETTERS, Boolean.valueOf(autoDetectGettersSetters));
		features.put(MapperFeature.AUTO_DETECT_GETTERS, Boolean.valueOf(autoDetectGettersSetters));
	}

	/**
	 * Shortcut for {@link SerializationFeature#FAIL_ON_EMPTY_BEANS} option.
	 */
	public void setFailOnEmptyBeans(boolean failOnEmptyBeans) {
		features.put(SerializationFeature.FAIL_ON_EMPTY_BEANS, Boolean.valueOf(failOnEmptyBeans));
	}

	/**
	 * Shortcut for {@link SerializationFeature#INDENT_OUTPUT} option.
	 */
	public void setIndentOutput(boolean indentOutput) {
		features.put(SerializationFeature.INDENT_OUTPUT, Boolean.valueOf(indentOutput));
	}

	private void addSerializers(SimpleModule module) {
		if (serializers == null) {
			return;
		}

		for (JsonSerializer<?> serializer : serializers) {
			module.addSerializer(serializer);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void addDeserializers(SimpleModule module) {
		if (CollectionUtils.isEmpty(deserializers)) {
			return;
		}

		for (Map.Entry<Class<?>, JsonDeserializer<?>> deserializer : deserializers.entrySet()) {
			module.addDeserializer((Class<T>) deserializer.getKey(),
					(JsonDeserializer<T>) deserializer.getValue());
		}
	}

	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws FatalBeanException {
		if (objectMapper == null) {
			objectMapper = new ObjectMapper();
		}

		if (dateFormat != null) {
			objectMapper.setDateFormat(this.dateFormat);
		}

		if (serializers != null || deserializers != null) {
			SimpleModule module = new SimpleModule("CustomSerializersModule", new Version(1, 0, 0,
					null, null, null));

			addSerializers(module);
			addDeserializers(module);

			objectMapper.registerModule(module);
		}

		if (annotationIntrospector != null) {
			objectMapper.setAnnotationIntrospector(annotationIntrospector);
		}

		for (Map.Entry<Object, Boolean> entry : features.entrySet()) {
			setFeatureEnabled(entry.getKey(), entry.getValue().booleanValue());
		}
	}

	private void setFeatureEnabled(Object feature, boolean enabled) {
		if (feature instanceof MapperFeature) {
			objectMapper.configure((MapperFeature) feature, enabled);
		}
		else if (feature instanceof DeserializationFeature) {
			objectMapper.configure((DeserializationFeature) feature, enabled);
		}
		else if (feature instanceof SerializationFeature) {
			objectMapper.configure((SerializationFeature) feature, enabled);
		}
		else if (feature instanceof JsonParser.Feature) {
			objectMapper.configure((JsonParser.Feature) feature, enabled);
		}
		else if (feature instanceof JsonGenerator.Feature) {
			objectMapper.configure((JsonGenerator.Feature) feature, enabled);
		}
		else {
			throw new FatalBeanException("Unknown feature class " + feature.getClass().getName());
		}
	}
}
