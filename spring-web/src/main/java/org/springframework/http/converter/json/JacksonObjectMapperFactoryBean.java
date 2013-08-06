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
import java.util.Map;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * A {@link FactoryBean} for creating a Jackson 1.x {@link ObjectMapper} with setters
 * to enable or disable Jackson features from within XML configuration.
 *
 * <p>Example usage with MappingJacksonHttpMessageConverter:
 * <pre class="code">
 * &lt;bean class="org.springframework.http.converter.json.MappingJacksonHttpMessageConverter">
 * 	&lt;property name="objectMapper">
 * 		&lt;bean class="org.springframework.web.context.support.JacksonObjectMapperFactoryBean"
 * 			p:autoDetectFields="false"
 * 			p:autoDetectGettersSetters="false"
 * 			p:annotationIntrospector-ref="jaxbAnnotationIntrospector" />
 * 	&lt;/property>
 * &lt;/bean>
 * </pre>
 *
 * <p>Example usage with MappingJacksonJsonView:
 * <pre class="code">
 * &lt;bean class="org.springframework.web.servlet.view.json.MappingJacksonJsonView">
 * 	&lt;property name="objectMapper">
 * 		&lt;bean class="org.springframework.web.context.support.JacksonObjectMapperFactoryBean"
 * 			p:autoDetectFields="false"
 * 			p:autoDetectGettersSetters="false"
 * 			p:annotationIntrospector-ref="jaxbAnnotationIntrospector" />
 * 	&lt;/property>
 * &lt;/bean>
 * </pre>
 *
 * <p>In case there are no specific setters provided (for some rarely used
 * options), you can still use the more general methods
 * {@link #setFeaturesToEnable(Object[])} and {@link #setFeaturesToDisable(Object[])}.
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.web.context.support.JacksonObjectMapperFactoryBean">
 * 	&lt;property name="featuresToEnable">
 * 		&lt;array>
 * 			&lt;util:constant static-field="org.codehaus.jackson.map.SerializationConfig$Feature.WRAP_ROOT_VALUE"/>
 * 			&lt;util:constant static-field="org.codehaus.jackson.map.SerializationConfig$Feature.CLOSE_CLOSEABLE"/>
 * 		&lt;/array>
 * 	&lt;/property>
 * 	&lt;property name="featuresToDisable">
 * 		&lt;array>
 * 			&lt;util:constant static-field="org.codehaus.jackson.map.DeserializationConfig$Feature.USE_ANNOTATIONS"/>
 * 		&lt;/array>
 * 	&lt;/property>
 * &lt;/bean>
 * </pre>
 *
 * <p><b>NOTE:</b> Requires Jackson 1.8 or higher, as of Spring 4.0.
 *
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class JacksonObjectMapperFactoryBean implements FactoryBean<ObjectMapper>, InitializingBean {

	private ObjectMapper objectMapper;

	private Map<Object, Boolean> features = new HashMap<Object, Boolean>();

	private DateFormat dateFormat;

	private AnnotationIntrospector annotationIntrospector;


	/**
	 * Set the ObjectMapper instance to use.
	 * If not set an instance will be created using the default constructor.
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
	 * Set the {@link AnnotationIntrospector} for serialization and deserialization.
	 * @see SerializationConfig#setAnnotationIntrospector(AnnotationIntrospector)
	 * @see DeserializationConfig#setAnnotationIntrospector(AnnotationIntrospector)
	 */
	public void setAnnotationIntrospector(AnnotationIntrospector annotationIntrospector) {
		this.annotationIntrospector = annotationIntrospector;
	}

	/**
	 * Shortcut for {@link org.codehaus.jackson.map.SerializationConfig.Feature#AUTO_DETECT_FIELDS} and
	 * {@link org.codehaus.jackson.map.DeserializationConfig.Feature#AUTO_DETECT_FIELDS}.
	 */
	public void setAutoDetectFields(boolean autoDetectFields) {
		this.features.put(SerializationConfig.Feature.AUTO_DETECT_FIELDS, autoDetectFields);
		this.features.put(DeserializationConfig.Feature.AUTO_DETECT_FIELDS, autoDetectFields);
	}

	/**
	 * Shortcut for {@link org.codehaus.jackson.map.SerializationConfig.Feature#AUTO_DETECT_GETTERS} and
	 * {@link org.codehaus.jackson.map.DeserializationConfig.Feature#AUTO_DETECT_SETTERS}.
	 */
	public void setAutoDetectGettersSetters(boolean autoDetectGettersSetters) {
		this.features.put(SerializationConfig.Feature.AUTO_DETECT_GETTERS, autoDetectGettersSetters);
		this.features.put(DeserializationConfig.Feature.AUTO_DETECT_SETTERS, autoDetectGettersSetters);
	}

	/**
	 * Shortcut for {@link org.codehaus.jackson.map.SerializationConfig.Feature#FAIL_ON_EMPTY_BEANS}.
	 */
	public void setFailOnEmptyBeans(boolean failOnEmptyBeans) {
		this.features.put(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, failOnEmptyBeans);
	}

	/**
	 * Shortcut for {@link org.codehaus.jackson.map.SerializationConfig.Feature#INDENT_OUTPUT}.
	 */
	public void setIndentOutput(boolean indentOutput) {
		this.features.put(SerializationConfig.Feature.INDENT_OUTPUT, indentOutput);
	}

	/**
	 * Specify features to enable.
	 * @see org.codehaus.jackson.JsonParser.Feature
	 * @see org.codehaus.jackson.JsonGenerator.Feature
	 * @see org.codehaus.jackson.map.SerializationConfig.Feature
	 * @see org.codehaus.jackson.map.DeserializationConfig.Feature
	 */
	public void setFeaturesToEnable(Object[] featuresToEnable) {
		if (featuresToEnable != null) {
			for (Object feature : featuresToEnable) {
				this.features.put(feature, Boolean.TRUE);
			}
		}
	}

	/**
	 * Specify features to disable.
	 * @see org.codehaus.jackson.JsonParser.Feature
	 * @see org.codehaus.jackson.JsonGenerator.Feature
	 * @see org.codehaus.jackson.map.SerializationConfig.Feature
	 * @see org.codehaus.jackson.map.DeserializationConfig.Feature
	 */
	public void setFeaturesToDisable(Object[] featuresToDisable) {
		if (featuresToDisable != null) {
			for (Object feature : featuresToDisable) {
				this.features.put(feature, Boolean.FALSE);
			}
		}
	}


	@Override
	public void afterPropertiesSet() {
		if (this.objectMapper == null) {
			this.objectMapper = new ObjectMapper();
		}
		if (this.annotationIntrospector != null) {
			this.objectMapper.setSerializationConfig(
					this.objectMapper.getSerializationConfig().withAnnotationIntrospector(this.annotationIntrospector));
			this.objectMapper.setDeserializationConfig(
					this.objectMapper.getDeserializationConfig().withAnnotationIntrospector(this.annotationIntrospector));
		}
		if (this.dateFormat != null) {
			this.objectMapper.setDateFormat(this.dateFormat);
		}
		for (Map.Entry<Object, Boolean> entry : this.features.entrySet()) {
			configureFeature(entry.getKey(), entry.getValue());
		}
	}

	private void configureFeature(Object feature, boolean enabled) {
		if (feature instanceof JsonParser.Feature) {
			this.objectMapper.configure((JsonParser.Feature) feature, enabled);
		}
		else if (feature instanceof JsonGenerator.Feature) {
			this.objectMapper.configure((JsonGenerator.Feature) feature, enabled);
		}
		else if (feature instanceof SerializationConfig.Feature) {
			this.objectMapper.configure((SerializationConfig.Feature) feature, enabled);
		}
		else if (feature instanceof DeserializationConfig.Feature) {
			this.objectMapper.configure((DeserializationConfig.Feature) feature, enabled);
		}
		else {
			throw new IllegalArgumentException("Unknown feature class: " + feature.getClass().getName());
		}
	}


	/**
	 * Return the singleton ObjectMapper.
	 */
	@Override
	public ObjectMapper getObject() {
		return this.objectMapper;
	}

	@Override
	public Class<?> getObjectType() {
		return ObjectMapper.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
