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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers.DateDeserializer;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.std.StdJdkSerializers.ClassSerializer;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.FatalBeanException;

/**
 * Test cases for {@link Jackson2ObjectMapperFactoryBean} class.
 * 
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 */
public class Jackson2ObjectMapperFactoryBeanTest {

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private Jackson2ObjectMapperFactoryBean factory;

	@Before
	public void setUp() {
		factory = new Jackson2ObjectMapperFactoryBean();
	}

	@Test(expected = FatalBeanException.class)
	public void testSetFeaturesToEnableNull() throws Exception {
		factory.setFeaturesToEnable((Object[]) null);
		factory.setFeaturesToEnable(new Object[0]);
	}

	@Test(expected = FatalBeanException.class)
	public void testSetFeaturesToDisableNull() {
		factory.setFeaturesToDisable((Object[]) null);
	}

	@Test
	public void testSetFeaturesToEnableEmpty() {
		factory.setFeaturesToEnable(new Object[0]);
		factory.setFeaturesToDisable(new Object[0]);
	}

	@Test(expected = FatalBeanException.class)
	public void testUnknownFeature() {
		factory.setFeaturesToEnable(new Object[] { Boolean.TRUE });
		factory.afterPropertiesSet();
	}

	@Test
	public void testBooleanSetters() {
		factory.setAutoDetectFields(false);
		factory.setAutoDetectGettersSetters(false);
		factory.setFailOnEmptyBeans(false);
		factory.setIndentOutput(true);

		factory.afterPropertiesSet();

		ObjectMapper objectMapper = factory.getObject();

		assertFalse(objectMapper.getSerializationConfig().isEnabled(
				MapperFeature.AUTO_DETECT_FIELDS));
		assertFalse(objectMapper.getDeserializationConfig().isEnabled(
				MapperFeature.AUTO_DETECT_FIELDS));

		assertFalse(objectMapper.getSerializationConfig().isEnabled(
				MapperFeature.AUTO_DETECT_GETTERS));
		assertFalse(objectMapper.getDeserializationConfig().isEnabled(
				MapperFeature.AUTO_DETECT_SETTERS));

		assertFalse(objectMapper.getSerializationConfig().isEnabled(
				SerializationFeature.FAIL_ON_EMPTY_BEANS));

		assertTrue(objectMapper.getSerializationConfig().isEnabled(
				SerializationFeature.INDENT_OUTPUT));
	}

	@Test
	public void testDateTimeFormatSetter() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

		factory.setDateFormat(dateFormat);
		factory.afterPropertiesSet();

		assertEquals(dateFormat, factory.getObject().getSerializationConfig().getDateFormat());
	}

	@Test
	public void testSimpleDateFormatStringSetter() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

		factory.setSimpleDateFormat(DATE_FORMAT);
		factory.afterPropertiesSet();

		assertEquals(dateFormat, factory.getObject().getSerializationConfig().getDateFormat());
	}

	@Test
	public void testSimpleSetup() {
		factory.afterPropertiesSet();
		assertNotNull(factory.getObject());
		assertTrue(factory.isSingleton());
		assertEquals(ObjectMapper.class, factory.getObjectType());
	}

	/**
	 * TODO: Replace usage of {@link DirectFieldAccessor} with getters. See <a
	 * href
	 * ="https://github.com/FasterXML/jackson-databind/issues/65">issue#65</a>.
	 */
	private static final SerializerFactoryConfig getSerializerFactoryConfig(
			ObjectMapper objectMapper) {
		DirectFieldAccessor objectMapperAccessor = new DirectFieldAccessor(objectMapper);
		DirectFieldAccessor serializerFactoryAccessor = new DirectFieldAccessor(
				objectMapperAccessor.getPropertyValue("_serializerFactory"));

		return (SerializerFactoryConfig) serializerFactoryAccessor
				.getPropertyValue("_factoryConfig");
	}

	private static final DeserializerFactoryConfig getDeserializerFactoryConfig(
			ObjectMapper objectMapper) {
		DirectFieldAccessor objectMapperAccessor = new DirectFieldAccessor(objectMapper);
		DirectFieldAccessor deserializationContextAccessor = new DirectFieldAccessor(
				objectMapperAccessor.getPropertyValue("_deserializationContext"));
		DirectFieldAccessor deserializerFactoryAccessor = new DirectFieldAccessor(
				deserializationContextAccessor.getPropertyValue("_factory"));

		return (DeserializerFactoryConfig) deserializerFactoryAccessor
				.getPropertyValue("_factoryConfig");
	}

	@Test
	public void testCompleteSetup() {
		NopAnnotationIntrospector annotationIntrospector = NopAnnotationIntrospector.instance;
		ObjectMapper objectMapper = new ObjectMapper();

		assertTrue(factory.isSingleton());
		assertEquals(ObjectMapper.class, factory.getObjectType());

		factory.setObjectMapper(objectMapper);
		factory.setSerializers(new ClassSerializer());
		factory.setDeserializers(Collections.<Class<?>, JsonDeserializer<?>> singletonMap(
				Date.class, new DateDeserializer()));
		factory.setAnnotationIntrospector(annotationIntrospector);
		factory.setFeaturesToEnable(SerializationFeature.FAIL_ON_EMPTY_BEANS,
				DeserializationFeature.UNWRAP_ROOT_VALUE,
				JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER,
				JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS);
		factory.setFeaturesToDisable(MapperFeature.AUTO_DETECT_GETTERS,
				MapperFeature.AUTO_DETECT_FIELDS, JsonParser.Feature.AUTO_CLOSE_SOURCE,
				JsonGenerator.Feature.QUOTE_FIELD_NAMES);

		assertFalse(getSerializerFactoryConfig(objectMapper).hasSerializers());
		assertFalse(getDeserializerFactoryConfig(objectMapper).hasDeserializers());

		factory.afterPropertiesSet();

		assertTrue(objectMapper == factory.getObject());

		assertTrue(getSerializerFactoryConfig(objectMapper).hasSerializers());
		assertTrue(getDeserializerFactoryConfig(objectMapper).hasDeserializers());

		assertTrue(annotationIntrospector == objectMapper.getSerializationConfig()
				.getAnnotationIntrospector());
		assertTrue(annotationIntrospector == objectMapper.getDeserializationConfig()
				.getAnnotationIntrospector());

		assertTrue(objectMapper.getSerializationConfig().isEnabled(
				SerializationFeature.FAIL_ON_EMPTY_BEANS));
		assertTrue(objectMapper.getDeserializationConfig().isEnabled(
				DeserializationFeature.UNWRAP_ROOT_VALUE));
		assertTrue(objectMapper.getJsonFactory().isEnabled(
				JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER));
		assertTrue(objectMapper.getJsonFactory().isEnabled(
				JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS));

		assertFalse(objectMapper.getSerializationConfig().isEnabled(
				MapperFeature.AUTO_DETECT_GETTERS));
		assertFalse(objectMapper.getDeserializationConfig().isEnabled(
				MapperFeature.AUTO_DETECT_FIELDS));
		assertFalse(objectMapper.getJsonFactory().isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
		assertFalse(objectMapper.getJsonFactory()
				.isEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
	}
}
