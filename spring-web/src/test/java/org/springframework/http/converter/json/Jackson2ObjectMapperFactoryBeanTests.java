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

package org.springframework.http.converter.json;

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

import static org.junit.Assert.*;

/**
 * Test cases for {@link Jackson2ObjectMapperFactoryBean} class.
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
		this.factory.setFeaturesToEnable(new Object[] { Boolean.TRUE });
		this.factory.afterPropertiesSet();
	}

	@Test
	public void testBooleanSetters() {
		this.factory.setAutoDetectFields(false);
		this.factory.setAutoDetectGettersSetters(false);
		this.factory.setFailOnEmptyBeans(false);
		this.factory.setIndentOutput(true);
		this.factory.afterPropertiesSet();

		ObjectMapper objectMapper = this.factory.getObject();

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
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.ALWAYS);

		factory.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.NON_NULL);
	}

	@Test
	public void testSetNotDefaultSerializationInclusion() {
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.ALWAYS);

		factory.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.NON_DEFAULT);
	}

	@Test
	public void testSetNotEmptySerializationInclusion() {
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.ALWAYS);

		factory.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
		factory.afterPropertiesSet();
		assertTrue(factory.getObject().getSerializationConfig().getSerializationInclusion() == JsonInclude.Include.NON_EMPTY);
	}

	@Test
	public void testDateTimeFormatSetter() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

		this.factory.setDateFormat(dateFormat);
		this.factory.afterPropertiesSet();

		assertEquals(dateFormat, this.factory.getObject().getSerializationConfig().getDateFormat());
		assertEquals(dateFormat, this.factory.getObject().getDeserializationConfig().getDateFormat());
	}

	@Test
	public void testSimpleDateFormatStringSetter() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

		this.factory.setSimpleDateFormat(DATE_FORMAT);
		this.factory.afterPropertiesSet();

		assertEquals(dateFormat, this.factory.getObject().getSerializationConfig().getDateFormat());
		assertEquals(dateFormat, this.factory.getObject().getDeserializationConfig().getDateFormat());
	}

	@Test
	public void testSimpleSetup() {
		this.factory.afterPropertiesSet();

		assertNotNull(this.factory.getObject());
		assertTrue(this.factory.isSingleton());
		assertEquals(ObjectMapper.class, this.factory.getObjectType());
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

		assertTrue(this.factory.isSingleton());
		assertEquals(ObjectMapper.class, this.factory.getObjectType());

		Map<Class<?>, JsonDeserializer<?>> deserializers = new HashMap<Class<?>, JsonDeserializer<?>>();
		deserializers.put(Date.class, new DateDeserializer());

		factory.setObjectMapper(objectMapper);

		JsonSerializer serializer1 = new ClassSerializer();
		JsonSerializer serializer2 = new NumberSerializer();

		factory.setSerializers(serializer1);
		factory.setSerializersByType(Collections.<Class<?>, JsonSerializer<?>> singletonMap(Boolean.class, serializer2));
		factory.setDeserializersByType(deserializers);
		factory.setAnnotationIntrospector(annotationIntrospector);

		this.factory.setFeaturesToEnable(SerializationFeature.FAIL_ON_EMPTY_BEANS,
				DeserializationFeature.UNWRAP_ROOT_VALUE,
				JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER,
				JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS);

		this.factory.setFeaturesToDisable(MapperFeature.AUTO_DETECT_GETTERS,
				MapperFeature.AUTO_DETECT_FIELDS,
				JsonParser.Feature.AUTO_CLOSE_SOURCE,
				JsonGenerator.Feature.QUOTE_FIELD_NAMES);

		assertFalse(getSerializerFactoryConfig(objectMapper).hasSerializers());
		assertFalse(getDeserializerFactoryConfig(objectMapper).hasDeserializers());

		this.factory.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		this.factory.afterPropertiesSet();

		assertTrue(objectMapper == this.factory.getObject());

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
