/*
 * Copyright 2002-2007 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * The bean configurator for Jackson {@link ObjectMapper}. Allows to enable/disable certain features for Jackson mapper.
 * Examples of usage:
 * 
 * <pre>
 * &lt;mvc:annotation-driven>
 * 	&lt;mvc:message-converters>
 * 		&lt;bean class="org.springframework.http.converter.json.MappingJacksonHttpMessageConverter">
 * 			&lt;property name="objectMapper">
 * 				&lt;bean class="org.springframework.web.context.support.JacksonObjectMapperBeanFactory"
 * 					p:autoDetectFields="false"
 * 					p:autoDetectGettersSetters="false"
 * 					p:annotationIntrospector-ref="jaxbAnnotationIntrospector" />
 * 			&lt;/property>
 * 		&lt;/bean>
 * 	&lt;/mvc:message-converters>
 * &lt;/mvc:annotation-driven>
 * </pre>
 * 
 * <pre>
 * &lt;bean class="org.springframework.web.servlet.view.json.MappingJacksonJsonView">
 * 	&lt;property name="objectMapper">
 * 		&lt;bean class="org.springframework.web.context.support.JacksonObjectMapperBeanFactory">
 * 			&lt;property name="featuresToEnable">
 * 				&lt;array>
 * 					&lt;util:constant static-field="org.codehaus.jackson.map.SerializationConfig$Feature.AUTO_DETECT_FIELDS"/>
 * 					&lt;util:constant static-field="org.codehaus.jackson.map.SerializationConfig$Feature.AUTO_DETECT_GETTERS"/>
 * 				&lt;/array>
 * 			&lt;/property>
 * 			&lt;property name="featuresToDisable">
 * 				&lt;array>
 * 					&lt;util:constant static-field="org.codehaus.jackson.map.SerializationConfig$Feature.AUTO_DETECT_FIELDS"/>
 * 					&lt;util:constant static-field="org.codehaus.jackson.map.SerializationConfig$Feature.AUTO_DETECT_GETTERS"/>
 * 				&lt;/array>
 * 			&lt;/property>
 * 		&lt;/bean>
 * 	&lt;/property>
 * &lt;/bean>
 * </pre>
 * 
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 */
public class JacksonObjectMapperBeanFactory implements FactoryBean<ObjectMapper>, InitializingBean {

	private ObjectMapper objectMapper;

	private Map<Object, Boolean> features = new HashMap<Object, Boolean>();

	private AnnotationIntrospector annotationIntrospector;

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

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * Define annotationIntrospector for both serializer and deserializer.
	 */
	public void setAnnotationIntrospector(AnnotationIntrospector annotationIntrospector) {
		this.annotationIntrospector = annotationIntrospector;
	}

	/**
	 * Generic setter for features to be enabled.
	 */
	public void setFeaturesToEnable(Object[] featuresToEnable) {
		if (featuresToEnable == null) {
			throw new FatalBeanException("featuresToEnable property should not be null");
		}

		for (Object feature : featuresToEnable) {
			features.put(feature, Boolean.TRUE);
		}
	}

	/**
	 * Generic setter for features to be disabled.
	 */
	public void setFeaturesToDisable(Object[] featuresToDisable) {
		if (featuresToDisable == null) {
			throw new FatalBeanException("featuresToDisable property should not be null");
		}

		for (Object feature : featuresToDisable) {
			features.put(feature, Boolean.FALSE);
		}
	}

	/**
	 * Shortcut for <code>AUTO_DETECT_FIELDS</code> option.
	 */
	public void setAutoDetectFields(boolean autoDetectFields) {
		features.put(DeserializationConfig.Feature.AUTO_DETECT_FIELDS, Boolean.valueOf(autoDetectFields));
		features.put(SerializationConfig.Feature.AUTO_DETECT_FIELDS, Boolean.valueOf(autoDetectFields));
	}

	/**
	 * Shortcut for <code>AUTO_DETECT_GETTERS / AUTO_DETECT_SETTERS</code> option.
	 */
	public void setAutoDetectGettersSetters(boolean autoDetectGettersSetters) {
		features.put(DeserializationConfig.Feature.AUTO_DETECT_SETTERS, Boolean.valueOf(autoDetectGettersSetters));
		features.put(SerializationConfig.Feature.AUTO_DETECT_GETTERS, Boolean.valueOf(autoDetectGettersSetters));
	}

	/**
	 * Shortcut for <code>FAIL_ON_EMPTY_BEANS</code> option.
	 */
	public void setFailOnEmptyBeans(boolean failOnEmptyBeans) {
		features.put(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, Boolean.valueOf(failOnEmptyBeans));
	}

	/**
	 * Shortcut for <code>INDENT_OUTPUT</code> option.
	 */
	public void setIndentOutput(boolean indentOutput) {
		features.put(SerializationConfig.Feature.INDENT_OUTPUT, Boolean.valueOf(indentOutput));
	}

	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws FatalBeanException {
		if (objectMapper == null) {
			objectMapper = new ObjectMapper();
		}

		if (annotationIntrospector != null) {
			objectMapper.getSerializationConfig().setAnnotationIntrospector(annotationIntrospector);
			objectMapper.getDeserializationConfig().setAnnotationIntrospector(annotationIntrospector);
		}

		for (Map.Entry<Object, Boolean> entry : features.entrySet()) {
			setFeatureEnabled(entry.getKey(), entry.getValue().booleanValue());
		}
	}

	private void setFeatureEnabled(Object feature, boolean enabled) {
		if (feature instanceof DeserializationConfig.Feature) {
			objectMapper.configure((DeserializationConfig.Feature) feature, enabled);
		} else if (feature instanceof SerializationConfig.Feature) {
			objectMapper.configure((SerializationConfig.Feature) feature, enabled);
		} else if (feature instanceof JsonParser.Feature) {
			objectMapper.configure((JsonParser.Feature) feature, enabled);
		} else if (feature instanceof JsonGenerator.Feature) {
			objectMapper.configure((JsonGenerator.Feature) feature, enabled);
		} else {
			throw new FatalBeanException("Unknown feature class " + feature.getClass().getName());
		}
	}
}
