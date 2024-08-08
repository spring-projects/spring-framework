/*
 * Copyright 2002-2024 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.cfg.EnumFeature;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.deser.BasicDeserializerFactory;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.BasicSerializerFactory;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.ser.std.ClassSerializer;
import com.fasterxml.jackson.databind.ser.std.NumberSerializer;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kotlin.ranges.IntRange;
import org.junit.jupiter.api.Test;

import org.springframework.http.ProblemDetail;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Test class for {@link Jackson2ObjectMapperBuilder}.
 *
 * @author Sebastien Deleuze
 * @author Eddú Meléndez
 * @author Hyoungjune Kim
 */
@SuppressWarnings("deprecation")
class Jackson2ObjectMapperBuilderTests {

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private static final String DATA = "{\"offsetDateTime\": \"2020-01-01T00:00:00\"}";


	@Test
	void unknownFeature() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Jackson2ObjectMapperBuilder.json().featuresToEnable(Boolean.TRUE).build());
	}

	@Test
	void defaultProperties() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
		assertThat(objectMapper).isNotNull();
		assertThat(objectMapper.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION)).isFalse();
		assertThat(objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
		assertThat(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_FIELDS)).isTrue();
		assertThat(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_GETTERS)).isTrue();
		assertThat(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_IS_GETTERS)).isTrue();
		assertThat(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_SETTERS)).isTrue();
		assertThat(objectMapper.isEnabled(SerializationFeature.INDENT_OUTPUT)).isFalse();
		assertThat(objectMapper.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS)).isTrue();
	}

	@Test
	void propertiesShortcut() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().autoDetectFields(false)
				.defaultViewInclusion(true).failOnUnknownProperties(true).failOnEmptyBeans(false)
				.autoDetectGettersSetters(false).indentOutput(true).build();
		assertThat(objectMapper).isNotNull();
		assertThat(objectMapper.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION)).isTrue();
		assertThat(objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isTrue();
		assertThat(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_FIELDS)).isFalse();
		assertThat(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_GETTERS)).isFalse();
		assertThat(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_IS_GETTERS)).isFalse();
		assertThat(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_SETTERS)).isFalse();
		assertThat(objectMapper.isEnabled(SerializationFeature.INDENT_OUTPUT)).isTrue();
		assertThat(objectMapper.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS)).isFalse();
	}

	@Test
	void booleanSetters() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.featuresToEnable(MapperFeature.DEFAULT_VIEW_INCLUSION,
						DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
						SerializationFeature.INDENT_OUTPUT)
				.featuresToDisable(MapperFeature.AUTO_DETECT_FIELDS,
						MapperFeature.AUTO_DETECT_GETTERS,
						MapperFeature.AUTO_DETECT_SETTERS,
						SerializationFeature.FAIL_ON_EMPTY_BEANS).build();
		assertThat(objectMapper).isNotNull();
		assertThat(objectMapper.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION)).isTrue();
		assertThat(objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isTrue();
		assertThat(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_FIELDS)).isFalse();
		assertThat(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_GETTERS)).isFalse();
		assertThat(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_SETTERS)).isFalse();
		assertThat(objectMapper.isEnabled(SerializationFeature.INDENT_OUTPUT)).isTrue();
		assertThat(objectMapper.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS)).isFalse();
	}

	@Test
	void setNotNullSerializationInclusion() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
		assertThat(objectMapper.getSerializationConfig().getSerializationInclusion()).isSameAs(JsonInclude.Include.ALWAYS);
		objectMapper = Jackson2ObjectMapperBuilder.json().serializationInclusion(JsonInclude.Include.NON_NULL).build();
		assertThat(objectMapper.getSerializationConfig().getSerializationInclusion()).isSameAs(JsonInclude.Include.NON_NULL);
	}

	@Test
	void setNotDefaultSerializationInclusion() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
		assertThat(objectMapper.getSerializationConfig().getSerializationInclusion()).isSameAs(JsonInclude.Include.ALWAYS);
		objectMapper = Jackson2ObjectMapperBuilder.json().serializationInclusion(JsonInclude.Include.NON_DEFAULT).build();
		assertThat(objectMapper.getSerializationConfig().getSerializationInclusion()).isSameAs(JsonInclude.Include.NON_DEFAULT);
	}

	@Test
	void setNotEmptySerializationInclusion() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
		assertThat(objectMapper.getSerializationConfig().getSerializationInclusion()).isSameAs(JsonInclude.Include.ALWAYS);
		objectMapper = Jackson2ObjectMapperBuilder.json().serializationInclusion(JsonInclude.Include.NON_EMPTY).build();
		assertThat(objectMapper.getSerializationConfig().getSerializationInclusion()).isSameAs(JsonInclude.Include.NON_EMPTY);
	}

	@Test
	void dateTimeFormatSetter() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().dateFormat(dateFormat).build();
		assertThat(objectMapper.getSerializationConfig().getDateFormat()).isEqualTo(dateFormat);
		assertThat(objectMapper.getDeserializationConfig().getDateFormat()).isEqualTo(dateFormat);
	}

	@Test
	void simpleDateFormatStringSetter() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().simpleDateFormat(DATE_FORMAT).build();
		assertThat(objectMapper.getSerializationConfig().getDateFormat()).isEqualTo(dateFormat);
		assertThat(objectMapper.getDeserializationConfig().getDateFormat()).isEqualTo(dateFormat);
	}

	@Test
	void localeSetter() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().locale(Locale.FRENCH).build();
		assertThat(objectMapper.getSerializationConfig().getLocale()).isEqualTo(Locale.FRENCH);
		assertThat(objectMapper.getDeserializationConfig().getLocale()).isEqualTo(Locale.FRENCH);
	}

	@Test
	void timeZoneSetter() {
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().timeZone(timeZone).build();
		assertThat(objectMapper.getSerializationConfig().getTimeZone()).isEqualTo(timeZone);
		assertThat(objectMapper.getDeserializationConfig().getTimeZone()).isEqualTo(timeZone);
	}

	@Test
	void timeZoneStringSetter() {
		String zoneId = "Europe/Paris";
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().timeZone(zoneId).build();
		TimeZone timeZone = TimeZone.getTimeZone(zoneId);
		assertThat(objectMapper.getSerializationConfig().getTimeZone()).isEqualTo(timeZone);
		assertThat(objectMapper.getDeserializationConfig().getTimeZone()).isEqualTo(timeZone);
	}

	@Test
	void wrongTimeZoneStringSetter() {
		String zoneId = "foo";
		assertThatIllegalArgumentException().isThrownBy(() ->
				Jackson2ObjectMapperBuilder.json().timeZone(zoneId).build());
	}

	@Test
	void modulesToInstallByClass() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.modulesToInstall(CustomIntegerModule.class)
				.build();
		Serializers serializers = getSerializerFactoryConfig(objectMapper).serializers().iterator().next();
		assertThat(serializers.findSerializer(null, SimpleType.construct(Integer.class), null).getClass()).isSameAs(CustomIntegerSerializer.class);
	}

	@Test
	void modulesToInstallByInstance() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.modulesToInstall(new CustomIntegerModule())
				.build();
		Serializers serializers = getSerializerFactoryConfig(objectMapper).serializers().iterator().next();
		assertThat(serializers.findSerializer(null, SimpleType.construct(Integer.class), null).getClass()).isSameAs(CustomIntegerSerializer.class);
	}

	@Test
	void modulesToInstallWithConsumer() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.modulesToInstall(list -> list.add(new CustomIntegerModule()))
				.build();
		Serializers serializers = getSerializerFactoryConfig(objectMapper).serializers().iterator().next();
		assertThat(serializers.findSerializer(null, SimpleType.construct(Integer.class), null).getClass()).isSameAs(CustomIntegerSerializer.class);
	}

	static class ParameterModuleDto {

		int x;
		int y;

		ParameterModuleDto(int x, int y) {
			this.x = x;
			this.y = y;
		}

		int getX() {
			return x;
		}

		int getY() {
			return y;
		}
	}

	@Test
	void wellKnownModules() throws JsonProcessingException {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

		Path file = Paths.get("foo");
		assertThat(new String(objectMapper.writeValueAsBytes(file), StandardCharsets.UTF_8))
				.endsWith("foo\"");

		Optional<String> optional = Optional.of("test");
		assertThat(new String(objectMapper.writeValueAsBytes(optional), StandardCharsets.UTF_8))
				.isEqualTo("\"test\"");


		assertThatCode(() -> objectMapper.readValue("{\"x\":1,\"y\":2}", ParameterModuleDto.class))
				.doesNotThrowAnyException();

		// Kotlin module
		IntRange range = new IntRange(1, 3);
		assertThat(new String(objectMapper.writeValueAsBytes(range), StandardCharsets.UTF_8))
				.isEqualTo("{\"start\":1,\"end\":3}");
	}

	@Test  // gh-22576
	void overrideWellKnownModuleWithModule() throws IOException {
		Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
		JavaTimeModule javaTimeModule = new JavaTimeModule();
		javaTimeModule.addDeserializer(OffsetDateTime.class, new OffsetDateTimeDeserializer());
		builder.modulesToInstall(javaTimeModule);
		builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		ObjectMapper objectMapper = builder.build();
		DemoPojo demoPojo = objectMapper.readValue(DATA, DemoPojo.class);
		assertThat(demoPojo.getOffsetDateTime()).isNotNull();
	}

	@Test  // gh-22740
	void registerMultipleModulesWithNullTypeId() {
		Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
		SimpleModule fooModule = new SimpleModule();
		fooModule.addSerializer(new FooSerializer());
		SimpleModule barModule = new SimpleModule();
		barModule.addSerializer(new BarSerializer());
		builder.modulesToInstall(fooModule, barModule);
		ObjectMapper objectMapper = builder.build();

		assertThat(StreamSupport
				.stream(getSerializerFactoryConfig(objectMapper).serializers().spliterator(), false)
				.filter(s -> s.findSerializer(null, SimpleType.construct(Foo.class), null) != null)
				.count()).isEqualTo(1);
		assertThat(StreamSupport
				.stream(getSerializerFactoryConfig(objectMapper).serializers().spliterator(), false)
				.filter(s -> s.findSerializer(null, SimpleType.construct(Bar.class), null) != null)
				.count()).isEqualTo(1);
	}

	private static SerializerFactoryConfig getSerializerFactoryConfig(ObjectMapper objectMapper) {
		return ((BasicSerializerFactory) objectMapper.getSerializerFactory()).getFactoryConfig();
	}

	private static DeserializerFactoryConfig getDeserializerFactoryConfig(ObjectMapper objectMapper) {
		return ((BasicDeserializerFactory) objectMapper.getDeserializationContext().getFactory()).getFactoryConfig();
	}

	@Test
	void modules() {
		NumberSerializer serializer1 = new NumberSerializer(Integer.class);
		SimpleModule module = new SimpleModule();
		module.addSerializer(Integer.class, serializer1);
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().modules(module).build();
		Serializers serializers = getSerializerFactoryConfig(objectMapper).serializers().iterator().next();
		assertThat(serializers.findSerializer(null, SimpleType.construct(Integer.class), null)).isSameAs(serializer1);
	}

	@Test
	void modulesWithConsumer() {
		NumberSerializer serializer1 = new NumberSerializer(Integer.class);
		SimpleModule module = new SimpleModule();
		module.addSerializer(Integer.class, serializer1);
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().modules(list -> list.add(module) ).build();
		Serializers serializers = getSerializerFactoryConfig(objectMapper).serializers().iterator().next();
		assertThat(serializers.findSerializer(null, SimpleType.construct(Integer.class), null)).isSameAs(serializer1);
	}

	@Test
	void modulesWithConsumerAfterModulesToInstall() {
		NumberSerializer serializer1 = new NumberSerializer(Integer.class);
		SimpleModule module = new SimpleModule();
		module.addSerializer(Integer.class, serializer1);
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.modulesToInstall(new JavaTimeModule())
				.modules(list -> list.add(module) ).build();
		Serializers serializers = getSerializerFactoryConfig(objectMapper).serializers().iterator().next();
		assertThat(serializers.findSerializer(null, SimpleType.construct(Integer.class), null)).isSameAs(serializer1);
	}

	@Test
	void propertyNamingStrategy() {
		PropertyNamingStrategy strategy = new PropertyNamingStrategy.SnakeCaseStrategy();
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().propertyNamingStrategy(strategy).build();
		assertThat(objectMapper.getSerializationConfig().getPropertyNamingStrategy()).isSameAs(strategy);
		assertThat(objectMapper.getDeserializationConfig().getPropertyNamingStrategy()).isSameAs(strategy);
	}

	@Test
	void serializerByType() {
		JsonSerializer<Number> serializer = new NumberSerializer(Integer.class);
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.modules(new ArrayList<>())  // disable well-known modules detection
				.serializerByType(Boolean.class, serializer)
				.build();
		assertThat(getSerializerFactoryConfig(objectMapper).hasSerializers()).isTrue();
		Serializers serializers = getSerializerFactoryConfig(objectMapper).serializers().iterator().next();
		assertThat(serializers.findSerializer(null, SimpleType.construct(Boolean.class), null)).isSameAs(serializer);
	}

	@Test
	void deserializerByType() throws JsonMappingException {
		JsonDeserializer<Date> deserializer = new DateDeserializers.DateDeserializer();
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.modules(new ArrayList<>())  // disable well-known modules detection
				.deserializerByType(Date.class, deserializer)
				.build();
		assertThat(getDeserializerFactoryConfig(objectMapper).hasDeserializers()).isTrue();
		Deserializers deserializers = getDeserializerFactoryConfig(objectMapper).deserializers().iterator().next();
		assertThat(deserializers.findBeanDeserializer(SimpleType.construct(Date.class), null, null)).isSameAs(deserializer);
	}

	@Test
	void mixIn() {
		Class<?> target = String.class;
		Class<?> mixInSource = Object.class;

		ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
				.modules().mixIn(target, mixInSource)
				.build();

		assertThat(mapper.mixInCount()).isEqualTo(2);
		assertThat(mapper.findMixInClassFor(ProblemDetail.class)).isAssignableFrom(ProblemDetailJacksonXmlMixin.class);
		assertThat(mapper.findMixInClassFor(target)).isSameAs(mixInSource);
	}

	@Test
	void mixIns() {
		Class<?> target = String.class;
		Class<?> mixInSource = Object.class;
		Map<Class<?>, Class<?>> mixIns = new HashMap<>();
		mixIns.put(target, mixInSource);

		ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
				.modules().mixIns(mixIns)
				.build();

		assertThat(mapper.mixInCount()).isEqualTo(2);
		assertThat(mapper.findMixInClassFor(ProblemDetail.class)).isAssignableFrom(ProblemDetailJacksonXmlMixin.class);
		assertThat(mapper.findMixInClassFor(target)).isSameAs(mixInSource);
	}

	@Test
	void filters() throws JsonProcessingException {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.filters(new SimpleFilterProvider().setFailOnUnknownId(false)).build();
		JacksonFilteredBean bean = new JacksonFilteredBean("value1", "value2");
		String output = objectMapper.writeValueAsString(bean);
		assertThat(output).contains("value1");
		assertThat(output).contains("value2");

		SimpleFilterProvider provider = new SimpleFilterProvider()
				.setFailOnUnknownId(false)
				.setDefaultFilter(SimpleBeanPropertyFilter.serializeAllExcept("property2"));
		objectMapper = Jackson2ObjectMapperBuilder.json().filters(provider).build();
		output = objectMapper.writeValueAsString(bean);
		assertThat(output).contains("value1");
		assertThat(output).doesNotContain("value2");
	}

	@Test  // gh-23017
	void postConfigurer() {
		JacksonAnnotationIntrospector introspector1 = new JacksonAnnotationIntrospector();
		JacksonAnnotationIntrospector introspector2 = new JacksonAnnotationIntrospector();

		ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
				.postConfigurer(m -> m.setAnnotationIntrospectors(introspector1, introspector2))
				.build();

		assertThat(mapper.getSerializationConfig().getAnnotationIntrospector()).isSameAs(introspector1);
		assertThat(mapper.getDeserializationConfig().getAnnotationIntrospector()).isSameAs(introspector2);
	}

	@Test
	void completeSetup() throws JsonMappingException {
		NopAnnotationIntrospector introspector = NopAnnotationIntrospector.instance;

		Map<Class<?>, JsonDeserializer<?>> deserializerMap = new HashMap<>();
		JsonDeserializer<Date> deserializer = new DateDeserializers.DateDeserializer();
		deserializerMap.put(Date.class, deserializer);

		JsonSerializer<Class<?>> serializer1 = new ClassSerializer();
		JsonSerializer<Number> serializer2 = new NumberSerializer(Integer.class);

		Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json()
				.modules(new ArrayList<>())  // disable well-known modules detection
				.serializers(serializer1)
				.serializersByType(Collections.singletonMap(Boolean.class, serializer2))
				.deserializersByType(deserializerMap)
				.annotationIntrospector(introspector)
				.annotationIntrospector(current -> AnnotationIntrospector.pair(current, introspector))
				.featuresToEnable(SerializationFeature.FAIL_ON_EMPTY_BEANS,
						DeserializationFeature.UNWRAP_ROOT_VALUE,
						EnumFeature.WRITE_ENUMS_TO_LOWERCASE,
						JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER,
						JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS)
				.featuresToDisable(MapperFeature.AUTO_DETECT_GETTERS,
						MapperFeature.AUTO_DETECT_FIELDS,
						JsonNodeFeature.READ_NULL_PROPERTIES,
						JsonParser.Feature.AUTO_CLOSE_SOURCE,
						JsonGenerator.Feature.QUOTE_FIELD_NAMES)
						.serializationInclusion(JsonInclude.Include.NON_NULL);

		ObjectMapper mapper = new ObjectMapper();
		builder.configure(mapper);

		assertThat(getSerializerFactoryConfig(mapper).hasSerializers()).isTrue();
		assertThat(getDeserializerFactoryConfig(mapper).hasDeserializers()).isTrue();

		Serializers serializers = getSerializerFactoryConfig(mapper).serializers().iterator().next();
		assertThat(serializers.findSerializer(null, SimpleType.construct(Class.class), null)).isSameAs(serializer1);
		assertThat(serializers.findSerializer(null, SimpleType.construct(Boolean.class), null)).isSameAs(serializer2);
		assertThat(serializers.findSerializer(null, SimpleType.construct(Number.class), null)).isNull();

		Deserializers deserializers = getDeserializerFactoryConfig(mapper).deserializers().iterator().next();
		assertThat(deserializers.findBeanDeserializer(SimpleType.construct(Date.class), null, null)).isSameAs(deserializer);

		AnnotationIntrospectorPair pair1 =
				(AnnotationIntrospectorPair) mapper.getSerializationConfig().getAnnotationIntrospector();
		AnnotationIntrospectorPair pair2 =
				(AnnotationIntrospectorPair) mapper.getDeserializationConfig().getAnnotationIntrospector();

		assertThat(pair1.allIntrospectors()).containsExactly(introspector, introspector);
		assertThat(pair2.allIntrospectors()).containsExactly(introspector, introspector);

		assertThat(mapper.getSerializationConfig().isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS)).isTrue();
		assertThat(mapper.getDeserializationConfig().isEnabled(DeserializationFeature.UNWRAP_ROOT_VALUE)).isTrue();
		assertThat(mapper.getSerializationConfig().isEnabled(EnumFeature.WRITE_ENUMS_TO_LOWERCASE)).isTrue();
		assertThat(mapper.getFactory().isEnabled(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)).isTrue();
		assertThat(mapper.getFactory().isEnabled(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS)).isTrue();

		assertThat(mapper.getSerializationConfig().isEnabled(MapperFeature.AUTO_DETECT_GETTERS)).isFalse();
		assertThat(mapper.getDeserializationConfig().isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION)).isFalse();
		assertThat(mapper.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
		assertThat(mapper.getDeserializationConfig().isEnabled(MapperFeature.AUTO_DETECT_FIELDS)).isFalse();
		assertThat(mapper.getDeserializationConfig().isEnabled(JsonNodeFeature.READ_NULL_PROPERTIES)).isFalse();
		assertThat(mapper.getFactory().isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE)).isFalse();
		assertThat(mapper.getFactory().isEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES)).isFalse();
		assertThat(mapper.getSerializationConfig().getSerializationInclusion()).isSameAs(JsonInclude.Include.NON_NULL);
	}

	@Test
	void xmlMapper() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.xml().build();
		assertThat(objectMapper).isNotNull();
		assertThat(objectMapper.getClass()).isEqualTo(XmlMapper.class);
	}

	@Test  // gh-22428
	void xmlMapperAndCustomFactory() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.xml().factory(new MyXmlFactory()).build();
		assertThat(objectMapper).isNotNull();
		assertThat(objectMapper.getClass()).isEqualTo(XmlMapper.class);
		assertThat(objectMapper.getFactory().getClass()).isEqualTo(MyXmlFactory.class);
	}

	@Test
	void createXmlMapper() {
		Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json().indentOutput(true);
		ObjectMapper jsonObjectMapper = builder.build();
		ObjectMapper xmlObjectMapper = builder.createXmlMapper(true).build();
		assertThat(jsonObjectMapper.isEnabled(SerializationFeature.INDENT_OUTPUT)).isTrue();
		assertThat(xmlObjectMapper.isEnabled(SerializationFeature.INDENT_OUTPUT)).isTrue();
		assertThat(xmlObjectMapper.getClass().isAssignableFrom(XmlMapper.class)).isTrue();
	}

	@Test  // SPR-13975
	void defaultUseWrapper() throws JsonProcessingException {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.xml().defaultUseWrapper(false).build();
		assertThat(objectMapper).isNotNull();
		assertThat(objectMapper.getClass()).isEqualTo(XmlMapper.class);
		ListContainer<String> container = new ListContainer<>(Arrays.asList("foo", "bar"));
		String output = objectMapper.writeValueAsString(container);
		assertThat(output).contains("<list>foo</list><list>bar</list></ListContainer>");
	}

	@Test  // SPR-14435
	void smile() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.smile().build();
		assertThat(objectMapper).isNotNull();
		assertThat(objectMapper.getFactory().getClass()).isEqualTo(SmileFactory.class);
	}

	@Test  // SPR-14435
	void cbor() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.cbor().build();
		assertThat(objectMapper).isNotNull();
		assertThat(objectMapper.getFactory().getClass()).isEqualTo(CBORFactory.class);
	}

	@Test  // SPR-14435
	void factory() {
		ObjectMapper objectMapper = new Jackson2ObjectMapperBuilder().factory(new SmileFactory()).build();
		assertThat(objectMapper).isNotNull();
		assertThat(objectMapper.getFactory().getClass()).isEqualTo(SmileFactory.class);
	}

	@Test
	void yaml() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.yaml().build();
		assertThat(objectMapper).isNotNull();
		assertThat(objectMapper.getFactory().getClass()).isEqualTo(YAMLFactory.class);
	}

	@Test
	void visibility() throws JsonProcessingException {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.visibility(PropertyAccessor.GETTER, Visibility.NONE)
				.visibility(PropertyAccessor.FIELD, Visibility.ANY)
				.build();

		String json = objectMapper.writeValueAsString(new JacksonVisibilityBean());
		assertThat(json).contains("property1");
		assertThat(json).contains("property2");
		assertThat(json).doesNotContain("property3");
	}


	static class CustomIntegerModule extends Module {

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


	static class CustomIntegerSerializer extends JsonSerializer<Integer> {

		@Override
		public void serialize(Integer value, JsonGenerator gen, SerializerProvider serializers)
				throws IOException {

			gen.writeStartObject();
			gen.writeNumberField("customid", value);
			gen.writeEndObject();
		}
	}


	@JsonFilter("myJacksonFilter")
	static class JacksonFilteredBean {

		public JacksonFilteredBean() {
		}

		public JacksonFilteredBean(String property1, String property2) {
			this.property1 = property1;
			this.property2 = property2;
		}

		private String property1;
		private String property2;

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


	static class ListContainer<T> {

		private List<T> list;

		public ListContainer() {
		}

		public ListContainer(List<T> list) {
			this.list = list;
		}

		public List<T> getList() {
			return list;
		}

		public void setList(List<T> list) {
			this.list = list;
		}
	}


	static class JacksonVisibilityBean {

		@SuppressWarnings("unused")
		private String property1;

		public String property2;

		public String getProperty3() {
			return null;
		}
	}


	static class OffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

		private static final String CURRENT_ZONE_OFFSET = OffsetDateTime.now().getOffset().toString();

		@Override
		public OffsetDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
			String value = jsonParser.getValueAsString();
			if (!StringUtils.hasLength(value)) {
				return null;
			}
			try {
				return OffsetDateTime.parse(value);
			}
			catch (DateTimeParseException exception) {
				return OffsetDateTime.parse(value + CURRENT_ZONE_OFFSET);
			}
		}
	}


	@JsonDeserialize
	static class DemoPojo {

		private OffsetDateTime offsetDateTime;

		public OffsetDateTime getOffsetDateTime() {
			return offsetDateTime;
		}

		public void setOffsetDateTime(OffsetDateTime offsetDateTime) {
			this.offsetDateTime = offsetDateTime;
		}
	}


	@SuppressWarnings("serial")
	public static class MyXmlFactory extends XmlFactory {
	}


	static class Foo {}

	static class Bar {}

	static class FooSerializer extends JsonSerializer<Foo> {
		@Override
		public void serialize(Foo value, JsonGenerator gen, SerializerProvider serializers) {
		}

		@Override
		public Class<Foo> handledType() {
			return Foo.class;
		}
	}

	static class BarSerializer extends JsonSerializer<Bar> {
		@Override
		public void serialize(Bar value, JsonGenerator gen, SerializerProvider serializers) {
		}
		@Override
		public Class<Bar> handledType() {
			return Bar.class;
		}
	}

}
