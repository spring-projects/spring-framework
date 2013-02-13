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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.NopAnnotationIntrospector;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 */
public class JacksonObjectMapperFactoryBeanTests {

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private JacksonObjectMapperFactoryBean factory;

	@Before
	public void setUp() {
		factory = new JacksonObjectMapperFactoryBean();
	}

	@Test
	public void testSetFeaturesToEnableEmpty() {
		factory.setFeaturesToEnable(new Object[0]);
		factory.setFeaturesToDisable(new Object[0]);
	}

	@Test(expected = IllegalArgumentException.class)
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

		SerializationConfig serializeConfig = objectMapper.getSerializationConfig();
		DeserializationConfig deserializeConfig = objectMapper.getDeserializationConfig();

		assertFalse(serializeConfig.isEnabled(SerializationConfig.Feature.AUTO_DETECT_FIELDS));
		assertFalse(deserializeConfig.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_FIELDS));
		assertFalse(serializeConfig.isEnabled(SerializationConfig.Feature.AUTO_DETECT_GETTERS));
		assertFalse(deserializeConfig.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_SETTERS));
		assertFalse(serializeConfig.isEnabled(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS));
		assertTrue(serializeConfig.isEnabled(SerializationConfig.Feature.INDENT_OUTPUT));
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

	@Test
	public void testCompleteSetup() {
		NopAnnotationIntrospector annotationIntrospector = new NopAnnotationIntrospector();
		ObjectMapper objectMapper = new ObjectMapper();

		assertTrue(factory.isSingleton());
		assertEquals(ObjectMapper.class, factory.getObjectType());

		factory.setObjectMapper(objectMapper);
		factory.setAnnotationIntrospector(annotationIntrospector);
		factory.setFeaturesToEnable(new Object[] {
				SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS,
				DeserializationConfig.Feature.USE_ANNOTATIONS,
				JsonParser.Feature.ALLOW_SINGLE_QUOTES,
				JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS
			});
		factory.setFeaturesToDisable(new Object[] {
				SerializationConfig.Feature.AUTO_DETECT_GETTERS,
				DeserializationConfig.Feature.AUTO_DETECT_FIELDS,
				JsonParser.Feature.AUTO_CLOSE_SOURCE,
				JsonGenerator.Feature.QUOTE_FIELD_NAMES
			});

		factory.afterPropertiesSet();

		assertTrue(objectMapper == factory.getObject());

		SerializationConfig serializeConfig = objectMapper.getSerializationConfig();
		DeserializationConfig deserializeConfig = objectMapper.getDeserializationConfig();
		JsonFactory jsonFactory = objectMapper.getJsonFactory();

		assertTrue(annotationIntrospector == serializeConfig.getAnnotationIntrospector());
		assertTrue(annotationIntrospector == deserializeConfig.getAnnotationIntrospector());

		assertTrue(serializeConfig.isEnabled(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS));
		assertTrue(deserializeConfig.isEnabled(DeserializationConfig.Feature.USE_ANNOTATIONS));
		assertTrue(jsonFactory.isEnabled(JsonParser.Feature.ALLOW_SINGLE_QUOTES));
		assertTrue(jsonFactory.isEnabled(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS));

		assertFalse(serializeConfig.isEnabled(SerializationConfig.Feature.AUTO_DETECT_GETTERS));
		assertFalse(deserializeConfig.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_FIELDS));
		assertFalse(jsonFactory.isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
		assertFalse(jsonFactory.isEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
	}

}
