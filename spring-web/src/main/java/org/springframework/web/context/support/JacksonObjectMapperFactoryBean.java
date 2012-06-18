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
import java.util.TimeZone;

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
 * The bean configurator for Jackson {@link ObjectMapper}. Allows to
 * enable/disable certain features for Jackson mapper. Examples of usage:
 * 
 * <pre>
 * &lt;mvc:annotation-driven>
 * 	&lt;mvc:message-converters>
 * 		&lt;bean class="org.springframework.http.converter.json.MappingJacksonHttpMessageConverter">
 * 			&lt;property name="objectMapper">
 * 				&lt;bean class="org.springframework.web.context.support.JacksonObjectMapperFactoryBean"
 * 					p:autoDetectFields="false"
 * 					p:autoDetectGettersSetters="false"
 * 					p:annotationIntrospector-ref="jaxbAnnotationIntrospector" />
 * 			&lt;/property>
 * 		&lt;/bean>
 * 	&lt;/mvc:message-converters>
 * &lt;/mvc:annotation-driven>
 * </pre>
 * 
 * In case there are no specific setters (for some rarely used options) they can
 * be configured by enumerating the specific options via
 * <code>featuresToEnable</code> and <code>featuresToDisable</code>
 * respectively.
 * 
 * <pre>
 * &lt;bean class="org.springframework.web.servlet.view.json.MappingJacksonJsonView">
 * 	&lt;property name="objectMapper">
 * 		&lt;bean class="org.springframework.web.context.support.JacksonObjectMapperFactoryBean">
 * 			&lt;property name="featuresToEnable">
 * 				&lt;array>
 * 					&lt;util:constant static-field="org.codehaus.jackson.map.SerializationConfig$Feature.WRAP_ROOT_VALUE"/>
 * 					&lt;util:constant static-field="org.codehaus.jackson.map.SerializationConfig$Feature.CLOSE_CLOSEABLE"/>
 * 				&lt;/array>
 * 			&lt;/property>
 * 			&lt;property name="featuresToDisable">
 * 				&lt;array>
 * 					&lt;util:constant static-field="org.codehaus.jackson.map.DeserializationConfig$Feature.USE_ANNOTATIONS"/>
 * 				&lt;/array>
 * 			&lt;/property>
 * 		&lt;/bean>
 * 	&lt;/property>
 * &lt;/bean>
 * </pre>
 * 
 * In case you mix two above approaches the final result depends on the order
 * the setters are called (the call has a priority).<br>
 * 
 * Note: This bean factory is singleton, so if you need several
 * {@link ObjectMapper}s which are configured differently, you need one separate
 * factory for each (may cause ambiguity for autowired-by-type properties).
 * 
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 * @since 3.2
 */
public class JacksonObjectMapperFactoryBean implements
		FactoryBean<ObjectMapper>, InitializingBean {

	private ObjectMapper objectMapper;

	private Map<Object, Boolean> features = new HashMap<Object, Boolean>();

	private AnnotationIntrospector annotationIntrospector;

	private DateFormat dateFormat;

	public final static String DATETIME_FORMAT_ISO8601_NS = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

	public final static String DATETIME_FORMAT_ISO8601_NS_Z = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	public final static String DATETIME_FORMAT_ISO8601_SECONDS = "yyyy-MM-dd'T'HH:mm:ssZ";

	public final static String DATETIME_FORMAT_ISO8601_SECONDS_Z = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	public final static String DATETIME_FORMAT_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

	public final static String DATE_FORMAT = "yyyy-MM-dd";

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
	public void setAnnotationIntrospector(
			AnnotationIntrospector annotationIntrospector) {
		this.annotationIntrospector = annotationIntrospector;
	}

	/**
	 * Generic setter for features to be enabled.
	 */
	public void setFeaturesToEnable(Object[] featuresToEnable) {
		if (featuresToEnable == null) {
			throw new FatalBeanException(
					"featuresToEnable property should not be null");
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
			throw new FatalBeanException(
					"featuresToDisable property should not be null");
		}

		for (Object feature : featuresToDisable) {
			features.put(feature, Boolean.FALSE);
		}
	}

	/**
	 * Shortcut for <code>AUTO_DETECT_FIELDS</code> option.
	 */
	public void setAutoDetectFields(boolean autoDetectFields) {
		features.put(DeserializationConfig.Feature.AUTO_DETECT_FIELDS,
				Boolean.valueOf(autoDetectFields));
		features.put(SerializationConfig.Feature.AUTO_DETECT_FIELDS,
				Boolean.valueOf(autoDetectFields));
	}

	/**
	 * Shortcut for <code>AUTO_DETECT_GETTERS / AUTO_DETECT_SETTERS</code>
	 * option.
	 */
	public void setAutoDetectGettersSetters(boolean autoDetectGettersSetters) {
		features.put(DeserializationConfig.Feature.AUTO_DETECT_SETTERS,
				Boolean.valueOf(autoDetectGettersSetters));
		features.put(SerializationConfig.Feature.AUTO_DETECT_GETTERS,
				Boolean.valueOf(autoDetectGettersSetters));
	}

	/**
	 * Shortcut for <code>FAIL_ON_EMPTY_BEANS</code> option.
	 */
	public void setFailOnEmptyBeans(boolean failOnEmptyBeans) {
		features.put(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS,
				Boolean.valueOf(failOnEmptyBeans));
	}

	/**
	 * Shortcut for <code>INDENT_OUTPUT</code> option. Check <a
	 * href="https://jira.springsource.org/browse/SPR-9302">SPR-9302</a>
	 * concerning when this feature will be included into Spring.
	 */
	public void setIndentOutput(boolean indentOutput) {
		features.put(SerializationConfig.Feature.INDENT_OUTPUT,
				Boolean.valueOf(indentOutput));
	}

	/**
	 * Define the format for date/time as given {@link DateFormat} instance.
	 */
	public void setDateTimeFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}

	/**
	 * Define the format for date/time as given format string. Timezone is set
	 * to current system timezone. Supposed to be used in conjunction with e.g.
	 * {@link #DATETIME_FORMAT_ISO8601_NS},
	 * {@link #DATETIME_FORMAT_ISO8601_SECONDS} constants.
	 */
	public void setDateTimeFormat(String format) {
		dateFormat = new SimpleDateFormat(format);
	}

	/**
	 * Define the format for date/time as given format string in UTC (GMT)
	 * timezone. Supposed to be used in conjunction with e.g.
	 * {@link #DATETIME_FORMAT_ISO8601_NS_Z},
	 * {@link #DATETIME_FORMAT_ISO8601_SECONDS_Z} constants.
	 */
	public void setUtcDateTimeFormat(String format) {
		dateFormat = new SimpleDateFormat(format);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws FatalBeanException {
		if (objectMapper == null) {
			objectMapper = new ObjectMapper();
		}

		if (annotationIntrospector != null) {
			objectMapper.getSerializationConfig().setAnnotationIntrospector(
					annotationIntrospector);
			objectMapper.getDeserializationConfig().setAnnotationIntrospector(
					annotationIntrospector);
		}

		if (dateFormat != null) {
			// Deprecated for 1.8+, use
			// objectMapper.setDateFormat(dateFormat);
			objectMapper.getSerializationConfig().setDateFormat(dateFormat);
		}

		for (Map.Entry<Object, Boolean> entry : features.entrySet()) {
			setFeatureEnabled(entry.getKey(), entry.getValue().booleanValue());
		}
	}

	private void setFeatureEnabled(Object feature, boolean enabled) {
		if (feature instanceof DeserializationConfig.Feature) {
			objectMapper.configure((DeserializationConfig.Feature) feature,
					enabled);
		}
		else if (feature instanceof SerializationConfig.Feature) {
			objectMapper.configure((SerializationConfig.Feature) feature,
					enabled);
		}
		else if (feature instanceof JsonParser.Feature) {
			objectMapper.configure((JsonParser.Feature) feature, enabled);
		}
		else if (feature instanceof JsonGenerator.Feature) {
			objectMapper.configure((JsonGenerator.Feature) feature, enabled);
		}
		else {
			throw new FatalBeanException("Unknown feature class "
					+ feature.getClass().getName());
		}
	}
}
