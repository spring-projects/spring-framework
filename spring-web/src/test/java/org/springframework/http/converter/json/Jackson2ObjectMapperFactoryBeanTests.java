/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.converter.json;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.deser.BasicDeserializerFactory;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers.DateDeserializer;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.BasicSerializerFactory;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.ser.std.ClassSerializer;
import com.fasterxml.jackson.databind.ser.std.NumberSerializer;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;

import org.springframework.beans.FatalBeanException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test cases for {@link Jackson2ObjectMapperFactoryBean}.
 *
 * @author Dmitry Katsubo
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
@SuppressWarnings("deprecation")
public class Jackson2ObjectMapperFactoryBeanTests {

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

	private final Jackson2ObjectMapperFactoryBean factory = new Jackson2ObjectMapperFactoryBean();


	@Test
	public void unknownFeature() {
		this.factory.setFeaturesToEnable(Boolean.TRUE);
		assertThatExceptionOfType(FatalBeanException.class).isThrownBy(
				this.factory::afterPropertiesSet);
	}

	@Test
	public void booleanSetters() {
		this.factory.setAutoDetectFields(false);
		this.factory.setAutoDetectGettersSetters(false);
		this.factory.setDefaultViewInclusion(false);
		this.factory.setFailOnEmptyBeans(false);
		this.factory.setIndentOutput(true);
		this.factory.afterPropertiesSet();

		ObjectMapper objectMapper = this.factory.getObject();

		assertThat(objectMapper.getSerializationConfig().isEnabled(MapperFeature.AUTO_DETECT_FIELDS)).isFalse();
		assertThat(objectMapper.getDeserializationConfig().isEnabled(MapperFeature.AUTO_DETECT_FIELDS)).isFalse();
		assertThat(objectMapper.getSerializationConfig().isEnabled(MapperFeature.AUTO_DETECT_GETTERS)).isFalse();
		assertThat(objectMapper.getDeserializationConfig().isEnabled(MapperFeature.AUTO_DETECT_SETTERS)).isFalse();
		assertThat(objectMapper.getDeserializationConfig().isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION)).isFalse();
		assertThat(objectMapper.getSerializationConfig().isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS)).isFalse();
		assertThat(objectMapper.getSerializationConfig().isEnabled(SerializationFeature.INDENT_OUTPUT)).isTrue();
		assertThat(objectMapper.getSerializationConfig().getSerializationInclusion()).isSameAs(Include.ALWAYS);
	}

	@Test
	public void defaultSerializationInclusion() {
		this.factory.afterPropertiesSet();
		assertThat(this.factory.getObject().getSerializationConfig().getSerializationInclusion()).isSameAs(Include.ALWAYS);
	}

	@Test
	public void nonNullSerializationInclusion() {
		this.factory.setSerializationInclusion(Include.NON_NULL);
		this.factory.afterPropertiesSet();
		assertThat(this.factory.getObject().getSerializationConfig().getSerializationInclusion()).isSameAs(Include.NON_NULL);
	}

	@Test
	public void nonDefaultSerializationInclusion() {
		this.factory.setSerializationInclusion(Include.NON_DEFAULT);
		this.factory.afterPropertiesSet();
		assertThat(this.factory.getObject().getSerializationConfig().getSerializationInclusion()).isSameAs(Include.NON_DEFAULT);
	}

	@Test
	public void nonEmptySerializationInclusion() {
		this.factory.setSerializationInclusion(Include.NON_EMPTY);
		this.factory.afterPropertiesSet();
		assertThat(this.factory.getObject().getSerializationConfig().getSerializationInclusion()).isSameAs(Include.NON_EMPTY);
	}

	@Test
	public void setDateFormat() {
		this.factory.setDateFormat(this.dateFormat);
		this.factory.afterPropertiesSet();

		assertThat(this.factory.getObject().getSerializationConfig().getDateFormat()).isEqualTo(this.dateFormat);
		assertThat(this.factory.getObject().getDeserializationConfig().getDateFormat()).isEqualTo(this.dateFormat);
	}

	@Test
	public void setSimpleDateFormat() {
		this.factory.setSimpleDateFormat(DATE_FORMAT);
		this.factory.afterPropertiesSet();

		assertThat(this.factory.getObject().getSerializationConfig().getDateFormat()).isEqualTo(this.dateFormat);
		assertThat(this.factory.getObject().getDeserializationConfig().getDateFormat()).isEqualTo(this.dateFormat);
	}

	@Test
	public void setLocale() {
		this.factory.setLocale(Locale.FRENCH);
		this.factory.afterPropertiesSet();

		assertThat(this.factory.getObject().getSerializationConfig().getLocale()).isEqualTo(Locale.FRENCH);
		assertThat(this.factory.getObject().getDeserializationConfig().getLocale()).isEqualTo(Locale.FRENCH);
	}

	@Test
	public void setTimeZone() {
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");

		this.factory.setTimeZone(timeZone);
		this.factory.afterPropertiesSet();

		assertThat(this.factory.getObject().getSerializationConfig().getTimeZone()).isEqualTo(timeZone);
		assertThat(this.factory.getObject().getDeserializationConfig().getTimeZone()).isEqualTo(timeZone);
	}

	@Test
	public void setTimeZoneWithInvalidZoneId() {
		this.factory.setTimeZone(TimeZone.getTimeZone("bogusZoneId"));
		this.factory.afterPropertiesSet();

		TimeZone timeZone = TimeZone.getTimeZone("GMT");
		assertThat(this.factory.getObject().getSerializationConfig().getTimeZone()).isEqualTo(timeZone);
		assertThat(this.factory.getObject().getDeserializationConfig().getTimeZone()).isEqualTo(timeZone);
	}

	@Test
	public void setModules() {
		NumberSerializer serializer = new NumberSerializer(Integer.class);
		SimpleModule module = new SimpleModule();
		module.addSerializer(Integer.class, serializer);

		this.factory.setModules(Arrays.asList(new Module[]{module}));
		this.factory.afterPropertiesSet();
		ObjectMapper objectMapper = this.factory.getObject();

		Serializers serializers = getSerializerFactoryConfig(objectMapper).serializers().iterator().next();
		assertThat(serializers.findSerializer(null, SimpleType.construct(Integer.class), null)).isSameAs(serializer);
	}

	@Test
	public void simpleSetup() {
		this.factory.afterPropertiesSet();

		assertThat(this.factory.getObject()).isNotNull();
		assertThat(this.factory.isSingleton()).isTrue();
		assertThat(this.factory.getObjectType()).isEqualTo(ObjectMapper.class);
	}

	@Test
	public void undefinedObjectType() {
		assertThat((Object) this.factory.getObjectType()).isNull();
	}

	private static SerializerFactoryConfig getSerializerFactoryConfig(ObjectMapper objectMapper) {
		return ((BasicSerializerFactory) objectMapper.getSerializerFactory()).getFactoryConfig();
	}

	private static DeserializerFactoryConfig getDeserializerFactoryConfig(ObjectMapper objectMapper) {
		return ((BasicDeserializerFactory) objectMapper.getDeserializationContext().getFactory()).getFactoryConfig();
	}

	@Test
	public void propertyNamingStrategy() {
		PropertyNamingStrategy strategy = new PropertyNamingStrategy.SnakeCaseStrategy();
		this.factory.setPropertyNamingStrategy(strategy);
		this.factory.afterPropertiesSet();

		assertThat(this.factory.getObject().getSerializationConfig().getPropertyNamingStrategy()).isSameAs(strategy);
		assertThat(this.factory.getObject().getDeserializationConfig().getPropertyNamingStrategy()).isSameAs(strategy);
	}

	@Test
	public void setMixIns() {
		Class<?> target = String.class;
		Class<?> mixinSource = Object.class;
		Map<Class<?>, Class<?>> mixIns = new HashMap<>();
		mixIns.put(target, mixinSource);

		this.factory.setModules(Collections.emptyList());
		this.factory.setMixIns(mixIns);
		this.factory.afterPropertiesSet();
		ObjectMapper objectMapper = this.factory.getObject();

		assertThat(objectMapper.mixInCount()).isEqualTo(1);
		assertThat(objectMapper.findMixInClassFor(target)).isSameAs(mixinSource);
	}

	@Test
	public void setFilters() throws JsonProcessingException {
		this.factory.setFilters(new SimpleFilterProvider().setFailOnUnknownId(false));
		this.factory.afterPropertiesSet();
		ObjectMapper objectMapper = this.factory.getObject();

		JacksonFilteredBean bean = new JacksonFilteredBean("value1", "value2");
		String output = objectMapper.writeValueAsString(bean);
		assertThat(output).contains("value1");
		assertThat(output).contains("value2");
	}

	@Test
	public void completeSetup() {
		NopAnnotationIntrospector annotationIntrospector = NopAnnotationIntrospector.instance;
		ObjectMapper objectMapper = new ObjectMapper();

		this.factory.setObjectMapper(objectMapper);
		assertThat(this.factory.isSingleton()).isTrue();
		assertThat(this.factory.getObjectType()).isEqualTo(ObjectMapper.class);

		Map<Class<?>, JsonDeserializer<?>> deserializers = new HashMap<>();
		deserializers.put(Date.class, new DateDeserializer());

		JsonSerializer<Class<?>> serializer1 = new ClassSerializer();
		JsonSerializer<Number> serializer2 = new NumberSerializer(Integer.class);

		// Disable well-known modules detection
		this.factory.setModules(new ArrayList<>());
		this.factory.setSerializers(serializer1);
		this.factory.setSerializersByType(Collections.singletonMap(Boolean.class, serializer2));
		this.factory.setDeserializersByType(deserializers);
		this.factory.setAnnotationIntrospector(annotationIntrospector);

		this.factory.setFeaturesToEnable(SerializationFeature.FAIL_ON_EMPTY_BEANS,
				DeserializationFeature.UNWRAP_ROOT_VALUE,
				JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER,
				JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS);

		this.factory.setFeaturesToDisable(MapperFeature.AUTO_DETECT_GETTERS,
				MapperFeature.AUTO_DETECT_FIELDS,
				JsonParser.Feature.AUTO_CLOSE_SOURCE,
				JsonGenerator.Feature.QUOTE_FIELD_NAMES);

		assertThat(getSerializerFactoryConfig(objectMapper).hasSerializers()).isFalse();
		assertThat(getDeserializerFactoryConfig(objectMapper).hasDeserializers()).isFalse();

		this.factory.setSerializationInclusion(Include.NON_NULL);
		this.factory.afterPropertiesSet();

		assertThat(this.factory.getObject()).isSameAs(objectMapper);
		assertThat(getSerializerFactoryConfig(objectMapper).hasSerializers()).isTrue();
		assertThat(getDeserializerFactoryConfig(objectMapper).hasDeserializers()).isTrue();

		Serializers serializers = getSerializerFactoryConfig(objectMapper).serializers().iterator().next();
		assertThat(serializers.findSerializer(null, SimpleType.construct(Class.class), null)).isSameAs(serializer1);
		assertThat(serializers.findSerializer(null, SimpleType.construct(Boolean.class), null)).isSameAs(serializer2);
		assertThat(serializers.findSerializer(null, SimpleType.construct(Number.class), null)).isNull();

		assertThat(objectMapper.getSerializationConfig().getAnnotationIntrospector()).isSameAs(annotationIntrospector);
		assertThat(objectMapper.getDeserializationConfig().getAnnotationIntrospector()).isSameAs(annotationIntrospector);

		assertThat(objectMapper.getSerializationConfig().isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS)).isTrue();
		assertThat(objectMapper.getDeserializationConfig().isEnabled(DeserializationFeature.UNWRAP_ROOT_VALUE)).isTrue();
		assertThat(objectMapper.getFactory().isEnabled(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)).isTrue();
		assertThat(objectMapper.getFactory().isEnabled(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS)).isTrue();

		assertThat(objectMapper.getSerializationConfig().isEnabled(MapperFeature.AUTO_DETECT_GETTERS)).isFalse();
		assertThat(objectMapper.getDeserializationConfig().isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION)).isFalse();
		assertThat(objectMapper.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
		assertThat(objectMapper.getDeserializationConfig().isEnabled(MapperFeature.AUTO_DETECT_FIELDS)).isFalse();
		assertThat(objectMapper.getFactory().isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE)).isFalse();
		assertThat(objectMapper.getFactory().isEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES)).isFalse();
		assertThat(objectMapper.getSerializationConfig().getSerializationInclusion()).isSameAs(Include.NON_NULL);
	}

	@Test
	public void setObjectMapper() {
		this.factory.setObjectMapper(new XmlMapper());
		this.factory.afterPropertiesSet();

		assertThat(this.factory.getObject()).isNotNull();
		assertThat(this.factory.isSingleton()).isTrue();
		assertThat(this.factory.getObjectType()).isEqualTo(XmlMapper.class);
	}

	@Test
	public void setCreateXmlMapper() {
		this.factory.setCreateXmlMapper(true);
		this.factory.afterPropertiesSet();

		assertThat(this.factory.getObject()).isNotNull();
		assertThat(this.factory.isSingleton()).isTrue();
		assertThat(this.factory.getObjectType()).isEqualTo(XmlMapper.class);
	}

	@Test  // SPR-14435
	public void setFactory() {
		this.factory.setFactory(new SmileFactory());
		this.factory.afterPropertiesSet();

		assertThat(this.factory.getObject()).isNotNull();
		assertThat(this.factory.isSingleton()).isTrue();
		assertThat(this.factory.getObject().getFactory().getClass()).isEqualTo(SmileFactory.class);
	}


	public static class CustomIntegerModule extends Module {

		@Override
		public String getModuleName() {
			return this.getClass().getSimpleName();
		}

		@Override
		public Version version() {
			return Version.unknownVersion();
		}

		@Override
		public void setupModule(SetupContext context) {
			SimpleSerializers serializers = new SimpleSerializers();
			serializers.addSerializer(Integer.class, new CustomIntegerSerializer());
			context.addSerializers(serializers);
		}
	}


	public static class CustomIntegerSerializer extends JsonSerializer<Integer> {

		@Override
		public void serialize(Integer value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeStartObject();
			gen.writeNumberField("customid", value);
			gen.writeEndObject();
		}
	}


	@JsonFilter("myJacksonFilter")
	public static class JacksonFilteredBean {

		private String property1;
		private String property2;


		public JacksonFilteredBean(String property1, String property2) {
			this.property1 = property1;
			this.property2 = property2;
		}

		public String getProperty1() {
			return property1;
		}

		public void setProperty1(String property1) {
			this.property1 = property1;
		}

		public String getProperty2() {
			return property2;
		}

		public void setProperty2(String property2) {
			this.property2 = property2;
		}
	}

}
