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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.FatalBeanException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.deser.BasicDeserializerFactory;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers.DateDeserializer;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.BasicSerializerFactory;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.NumberSerializers.NumberSerializer;
import com.fasterxml.jackson.databind.ser.std.StdJdkSerializers.ClassSerializer;
import com.fasterxml.jackson.databind.type.SimpleType;

/**
 * Test cases for {@link org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean} class.
 *
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 */
public class Jackson2ObjectMapperFactoryBeanTests {

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private Jackson2ObjectMapperFactoryBean factory;

	@Before
	public void setUp() {
		factory = new Jackson2ObjectMapperFactoryBean();
	}

	@Test
	public void testSettersWithNullValues() {
		// Should not crash:
		factory.setSerializers((JsonSerializer<?>[]) null);
		factory.setSerializersByType(null);
		factory.setDeserializersByType(null);
		factory.setFeaturesToEnable((Object[]) null);
		factory.setFeaturesToDisable((Object[]) null);
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

		assertFalse(objectMapper.getSerializationConfig().isEnabled(MapperFeature.AUTO_DETECT_FIELDS));
		assertFalse(objectMapper.getDeserializationConfig().isEnabled(MapperFeature.AUTO_DETECT_FIELDS));
		assertFalse(objectMapper.getSerializationConfig().isEnabled(MapperFeature.AUTO_DETECT_GETTERS));
		assertFalse(objectMapper.getDeserializationConfig().isEnabled(MapperFeature.AUTO_DETECT_SETTERS));
		assertFalse(objectMapper.getSerializationConfig().isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
		assertTrue(objectMapper.getSerializationConfig().isEnabled(SerializationFeature.INDENT_OUTPUT));
		assertTrue(objectMapper.getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.ALWAYS);
	}

	@Test
	public void testSetNotNullSerializationInclusion() {
		factory.setNotNullSerializationInclusion(false);
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.ALWAYS);

		factory.setNotNullSerializationInclusion(true);
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.NON_NULL);
	}

	@Test
	public void testSetNotDefaultSerializationInclusion() {
		factory.setNotDefaultSerializationInclusion(false);
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.ALWAYS);

		factory.setNotDefaultSerializationInclusion(true);
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.NON_DEFAULT);
	}

	@Test
	public void testSetNotEmptySerializationInclusion() {
		factory.setNotEmptySerializationInclusion(false);
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.ALWAYS);

		factory.setNotEmptySerializationInclusion(true);
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.NON_EMPTY);
	}

	@Test
	public void testDateTimeFormatSetter() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

		factory.setDateFormat(dateFormat);
		factory.afterPropertiesSet();

		assertEquals(dateFormat, factory.getObject().getSerializationConfig().getDateFormat());
		assertEquals(dateFormat, factory.getObject().getDeserializationConfig().getDateFormat());
	}

	@Test
	public void testSimpleDateFormatStringSetter() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

		factory.setSimpleDateFormat(DATE_FORMAT);
		factory.afterPropertiesSet();

		assertEquals(dateFormat, factory.getObject().getSerializationConfig().getDateFormat());
		assertEquals(dateFormat, factory.getObject().getDeserializationConfig().getDateFormat());
	}

	@Test
	public void testSimpleSetup() {
		factory.afterPropertiesSet();

		assertNotNull(factory.getObject());
		assertTrue(factory.isSingleton());
		assertEquals(ObjectMapper.class, factory.getObjectType());
	}

	private static final SerializerFactoryConfig getSerializerFactoryConfig(ObjectMapper objectMapper) {
		return ((BasicSerializerFactory) objectMapper.getSerializerFactory()).getFactoryConfig();
	}

	private static final DeserializerFactoryConfig getDeserializerFactoryConfig(ObjectMapper objectMapper) {
		return ((BasicDeserializerFactory) objectMapper.getDeserializationContext().getFactory()).getFactoryConfig();
	}

	@Test
	public void testCompleteSetup() {
		NopAnnotationIntrospector annotationIntrospector = NopAnnotationIntrospector.instance;
		ObjectMapper objectMapper = new ObjectMapper();

		assertTrue(factory.isSingleton());
		assertEquals(ObjectMapper.class, factory.getObjectType());

		Map<Class<?>, JsonDeserializer<?>> deserializers = new HashMap<Class<?>, JsonDeserializer<?>>();
		deserializers.put(Date.class, new DateDeserializer());

		factory.setObjectMapper(objectMapper);

		JsonSerializer serializer1 = new ClassSerializer();
		JsonSerializer serializer2 = new NumberSerializer();

		factory.setSerializers(serializer1);
		factory.setSerializersByType(Collections.<Class<?>, JsonSerializer<?>> singletonMap(Boolean.class, serializer2));
		factory.setDeserializersByType(deserializers);
		factory.setAnnotationIntrospector(annotationIntrospector);

		factory.setFeaturesToEnable(SerializationFeature.FAIL_ON_EMPTY_BEANS, DeserializationFeature.UNWRAP_ROOT_VALUE,
				JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER,
				JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS);

		factory.setFeaturesToDisable(MapperFeature.AUTO_DETECT_GETTERS, MapperFeature.AUTO_DETECT_FIELDS,
				JsonParser.Feature.AUTO_CLOSE_SOURCE, JsonGenerator.Feature.QUOTE_FIELD_NAMES);

		assertFalse(getSerializerFactoryConfig(objectMapper).hasSerializers());
		assertFalse(getDeserializerFactoryConfig(objectMapper).hasDeserializers());

		factory.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		factory.afterPropertiesSet();

		assertTrue(objectMapper == factory.getObject());

		assertTrue(getSerializerFactoryConfig(objectMapper).hasSerializers());
		assertTrue(getDeserializerFactoryConfig(objectMapper).hasDeserializers());

		Serializers serializers = getSerializerFactoryConfig(objectMapper).serializers().iterator().next();

		assertTrue(serializers.findSerializer(null, SimpleType.construct(Class.class), null) == serializer1);
		assertTrue(serializers.findSerializer(null, SimpleType.construct(Boolean.class), null) == serializer2);
		assertNull(serializers.findSerializer(null, SimpleType.construct(Number.class), null));

		assertTrue(annotationIntrospector == objectMapper.getSerializationConfig().getAnnotationIntrospector());
		assertTrue(annotationIntrospector == objectMapper.getDeserializationConfig().getAnnotationIntrospector());

		assertTrue(objectMapper.getSerializationConfig().isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
		assertTrue(objectMapper.getDeserializationConfig().isEnabled(DeserializationFeature.UNWRAP_ROOT_VALUE));
		assertTrue(objectMapper.getFactory().isEnabled(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER));
		assertTrue(objectMapper.getFactory().isEnabled(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS));

		assertFalse(objectMapper.getSerializationConfig().isEnabled(MapperFeature.AUTO_DETECT_GETTERS));
		assertFalse(objectMapper.getDeserializationConfig().isEnabled(MapperFeature.AUTO_DETECT_FIELDS));
		assertFalse(objectMapper.getFactory().isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
		assertFalse(objectMapper.getFactory().isEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));

		assertTrue(objectMapper.getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.NON_NULL);
	}
}
