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
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.NopAnnotationIntrospector;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.FatalBeanException;

/**
 * Test cases for {@link JacksonObjectMapperBeanFactory} class.
 * 
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 */
public class JacksonObjectMapperBeanFactoryTest {

	private JacksonObjectMapperBeanFactory beanFactory;

	@Before
	public void setUp() {
		beanFactory = new JacksonObjectMapperBeanFactory();
	}

	@Test
	public void testChecks() {
		try {
			beanFactory.setFeaturesToEnable(null);

			fail("FatalBeanException should be thrown");
		}
		catch (FatalBeanException e) {
			// should be thrown
			assertNotNull(e);
		}

		beanFactory.setFeaturesToEnable(new Object[0]);

		try {
			beanFactory.setFeaturesToDisable(null);

			fail("FatalBeanException should be thrown");
		}
		catch (FatalBeanException e) {
			// should be thrown
			assertNotNull(e);
		}

		beanFactory.setFeaturesToDisable(new Object[0]);
	}

	@Test(expected = FatalBeanException.class)
	public void testUnknownFeature() {
		beanFactory.setFeaturesToEnable(new Object[] { Boolean.TRUE });
		beanFactory.afterPropertiesSet();

		fail("FatalBeanException should be thrown");
	}

	@Test
	public void testBooleanSetters() {

		beanFactory.setAutoDetectFields(false);
		beanFactory.setAutoDetectGettersSetters(false);
		beanFactory.setFailOnEmptyBeans(false);
		beanFactory.setIndentOutput(true);

		beanFactory.afterPropertiesSet();

		ObjectMapper objectMapper = beanFactory.getObject();

		assertFalse(objectMapper.getSerializationConfig().isEnabled(
				SerializationConfig.Feature.AUTO_DETECT_FIELDS));
		assertFalse(objectMapper.getDeserializationConfig().isEnabled(
				DeserializationConfig.Feature.AUTO_DETECT_FIELDS));

		assertFalse(objectMapper.getSerializationConfig().isEnabled(
				SerializationConfig.Feature.AUTO_DETECT_GETTERS));
		assertFalse(objectMapper.getDeserializationConfig().isEnabled(
				DeserializationConfig.Feature.AUTO_DETECT_SETTERS));

		assertFalse(objectMapper.getSerializationConfig().isEnabled(
				SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS));

		assertTrue(objectMapper.getSerializationConfig().isEnabled(
				SerializationConfig.Feature.INDENT_OUTPUT));
	}

	@Test
	public void testDateTimeFormatSetter() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				JacksonObjectMapperBeanFactory.DATE_FORMAT);

		beanFactory.setDateTimeFormat(dateFormat);
		beanFactory.afterPropertiesSet();

		assertEquals(dateFormat, beanFactory.getObject()
				.getSerializationConfig().getDateFormat());
	}

	@Test
	public void testDateTimeFormatStringSetter() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				JacksonObjectMapperBeanFactory.DATE_FORMAT);

		beanFactory
				.setDateTimeFormat(JacksonObjectMapperBeanFactory.DATE_FORMAT);
		beanFactory.afterPropertiesSet();

		assertEquals(dateFormat, beanFactory.getObject()
				.getSerializationConfig().getDateFormat());
	}

	@Test
	public void testDateTimeFormatStringTimezoneSetter() {
		beanFactory
				.setUtcDateTimeFormat(JacksonObjectMapperBeanFactory.DATE_FORMAT);
		beanFactory.afterPropertiesSet();

		assertEquals(TimeZone.getTimeZone("GMT"), beanFactory.getObject()
				.getSerializationConfig().getDateFormat().getTimeZone());
	}

	@Test
	public void testSimpleFlow() {
		beanFactory.afterPropertiesSet();
		assertNotNull(beanFactory.getObject());
		assertTrue(beanFactory.isSingleton());
		assertEquals(ObjectMapper.class, beanFactory.getObjectType());
	}

	@Test
	public void testCompleteFlow() {
		NopAnnotationIntrospector annotationIntrospector = new NopAnnotationIntrospector();
		ObjectMapper objectMapper = new ObjectMapper();

		assertTrue(beanFactory.isSingleton());
		assertEquals(ObjectMapper.class, beanFactory.getObjectType());

		beanFactory.setObjectMapper(objectMapper);
		beanFactory.setAnnotationIntrospector(annotationIntrospector);
		beanFactory.setFeaturesToEnable(new Object[] {
				SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS,
				DeserializationConfig.Feature.USE_ANNOTATIONS,
				JsonParser.Feature.ALLOW_SINGLE_QUOTES,
				JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS });
		beanFactory.setFeaturesToDisable(new Object[] {
				SerializationConfig.Feature.AUTO_DETECT_GETTERS,
				DeserializationConfig.Feature.AUTO_DETECT_FIELDS,
				JsonParser.Feature.AUTO_CLOSE_SOURCE,
				JsonGenerator.Feature.QUOTE_FIELD_NAMES });

		beanFactory.afterPropertiesSet();

		assertTrue(objectMapper == beanFactory.getObject());

		assertTrue(annotationIntrospector == objectMapper
				.getSerializationConfig().getAnnotationIntrospector());
		assertTrue(annotationIntrospector == objectMapper
				.getDeserializationConfig().getAnnotationIntrospector());

		assertTrue(objectMapper.getSerializationConfig().isEnabled(
				SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS));
		assertTrue(objectMapper.getDeserializationConfig().isEnabled(
				DeserializationConfig.Feature.USE_ANNOTATIONS));
		assertTrue(objectMapper.getJsonFactory().isEnabled(
				JsonParser.Feature.ALLOW_SINGLE_QUOTES));
		assertTrue(objectMapper.getJsonFactory().isEnabled(
				JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS));

		assertFalse(objectMapper.getSerializationConfig().isEnabled(
				SerializationConfig.Feature.AUTO_DETECT_GETTERS));
		assertFalse(objectMapper.getDeserializationConfig().isEnabled(
				DeserializationConfig.Feature.AUTO_DETECT_FIELDS));
		assertFalse(objectMapper.getJsonFactory().isEnabled(
				JsonParser.Feature.AUTO_CLOSE_SOURCE));
		assertFalse(objectMapper.getJsonFactory().isEnabled(
				JsonGenerator.Feature.QUOTE_FIELD_NAMES));
	}
}
