/*
 * Copyright 2002-2019 the original author or authors.
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
import java.io.UnsupportedEncodingException;
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
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.deser.BasicDeserializerFactory;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kotlin.ranges.IntRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import org.springframework.beans.FatalBeanException;
import org.springframework.util.StringUtils;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Test class for {@link Jackson2ObjectMapperBuilder}.
 *
 * @author Sebastien Deleuze
 * @author Eddú Meléndez
 */
@SuppressWarnings("deprecation")
public class Jackson2ObjectMapperBuilderTests {

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private static final String DATA = "{\"offsetDateTime\": \"2020-01-01T00:00:00\"}";


	@Test(expected = FatalBeanException.class)
	public void unknownFeature() {
		Jackson2ObjectMapperBuilder.json().featuresToEnable(Boolean.TRUE).build();
	}

	@Test
	public void defaultProperties() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
		assertNotNull(objectMapper);
		assertFalse(objectMapper.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION));
		assertFalse(objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
		assertTrue(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_FIELDS));
		assertTrue(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_GETTERS));
		assertTrue(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_IS_GETTERS));
		assertTrue(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_SETTERS));
		assertFalse(objectMapper.isEnabled(SerializationFeature.INDENT_OUTPUT));
		assertTrue(objectMapper.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
	}

	@Test
	public void propertiesShortcut() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().autoDetectFields(false)
				.defaultViewInclusion(true).failOnUnknownProperties(true).failOnEmptyBeans(false)
				.autoDetectGettersSetters(false).indentOutput(true).build();
		assertNotNull(objectMapper);
		assertTrue(objectMapper.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION));
		assertTrue(objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
		assertFalse(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_FIELDS));
		assertFalse(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_GETTERS));
		assertFalse(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_IS_GETTERS));
		assertFalse(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_SETTERS));
		assertTrue(objectMapper.isEnabled(SerializationFeature.INDENT_OUTPUT));
		assertFalse(objectMapper.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
	}

	@Test
	public void booleanSetters() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.featuresToEnable(MapperFeature.DEFAULT_VIEW_INCLUSION,
						DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
						SerializationFeature.INDENT_OUTPUT)
				.featuresToDisable(MapperFeature.AUTO_DETECT_FIELDS,
						MapperFeature.AUTO_DETECT_GETTERS,
						MapperFeature.AUTO_DETECT_SETTERS,
						SerializationFeature.FAIL_ON_EMPTY_BEANS).build();
		assertNotNull(objectMapper);
		assertTrue(objectMapper.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION));
		assertTrue(objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
		assertFalse(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_FIELDS));
		assertFalse(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_GETTERS));
		assertFalse(objectMapper.isEnabled(MapperFeature.AUTO_DETECT_SETTERS));
		assertTrue(objectMapper.isEnabled(SerializationFeature.INDENT_OUTPUT));
		assertFalse(objectMapper.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
	}

	@Test
	public void setNotNullSerializationInclusion() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
		assertSame(JsonInclude.Include.ALWAYS, objectMapper.getSerializationConfig().getSerializationInclusion());
		objectMapper = Jackson2ObjectMapperBuilder.json().serializationInclusion(JsonInclude.Include.NON_NULL).build();
		assertSame(JsonInclude.Include.NON_NULL, objectMapper.getSerializationConfig().getSerializationInclusion());
	}

	@Test
	public void setNotDefaultSerializationInclusion() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
		assertSame(JsonInclude.Include.ALWAYS, objectMapper.getSerializationConfig().getSerializationInclusion());
		objectMapper = Jackson2ObjectMapperBuilder.json().serializationInclusion(JsonInclude.Include.NON_DEFAULT).build();
		assertSame(JsonInclude.Include.NON_DEFAULT, objectMapper.getSerializationConfig().getSerializationInclusion());
	}

	@Test
	public void setNotEmptySerializationInclusion() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
		assertSame(JsonInclude.Include.ALWAYS, objectMapper.getSerializationConfig().getSerializationInclusion());
		objectMapper = Jackson2ObjectMapperBuilder.json().serializationInclusion(JsonInclude.Include.NON_EMPTY).build();
		assertSame(JsonInclude.Include.NON_EMPTY, objectMapper.getSerializationConfig().getSerializationInclusion());
	}

	@Test
	public void dateTimeFormatSetter() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().dateFormat(dateFormat).build();
		assertEquals(dateFormat, objectMapper.getSerializationConfig().getDateFormat());
		assertEquals(dateFormat, objectMapper.getDeserializationConfig().getDateFormat());
	}

	@Test
	public void simpleDateFormatStringSetter() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().simpleDateFormat(DATE_FORMAT).build();
		assertEquals(dateFormat, objectMapper.getSerializationConfig().getDateFormat());
		assertEquals(dateFormat, objectMapper.getDeserializationConfig().getDateFormat());
	}

	@Test
	public void localeSetter() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().locale(Locale.FRENCH).build();
		assertEquals(Locale.FRENCH, objectMapper.getSerializationConfig().getLocale());
		assertEquals(Locale.FRENCH, objectMapper.getDeserializationConfig().getLocale());
	}

	@Test
	public void timeZoneSetter() {
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().timeZone(timeZone).build();
		assertEquals(timeZone, objectMapper.getSerializationConfig().getTimeZone());
		assertEquals(timeZone, objectMapper.getDeserializationConfig().getTimeZone());
	}

	@Test
	public void timeZoneStringSetter() {
		String zoneId = "Europe/Paris";
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().timeZone(zoneId).build();
		TimeZone timeZone = TimeZone.getTimeZone(zoneId);
		assertEquals(timeZone, objectMapper.getSerializationConfig().getTimeZone());
		assertEquals(timeZone, objectMapper.getDeserializationConfig().getTimeZone());
	}

	@Test(expected = IllegalArgumentException.class)
	public void wrongTimeZoneStringSetter() {
		String zoneId = "foo";
		Jackson2ObjectMapperBuilder.json().timeZone(zoneId).build();
	}

	@Test
	public void modules() {
		NumberSerializer serializer1 = new NumberSerializer(Integer.class);
		SimpleModule module = new SimpleModule();
		module.addSerializer(Integer.class, serializer1);
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().modules(module).build();
		Serializers serializers = getSerializerFactoryConfig(objectMapper).serializers().iterator().next();
		assertSame(serializer1, serializers.findSerializer(null, SimpleType.construct(Integer.class), null));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void modulesToInstallByClass() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.modulesToInstall(CustomIntegerModule.class)
				.build();
		Serializers serializers = getSerializerFactoryConfig(objectMapper).serializers().iterator().next();
		assertSame(CustomIntegerSerializer.class,
				serializers.findSerializer(null, SimpleType.construct(Integer.class), null).getClass());
	}

	@Test
	public void modulesToInstallByInstance() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.modulesToInstall(new CustomIntegerModule())
				.build();
		Serializers serializers = getSerializerFactoryConfig(objectMapper).serializers().iterator().next();
		assertSame(CustomIntegerSerializer.class,
				serializers.findSerializer(null, SimpleType.construct(Integer.class), null).getClass());
	}

	@Test
	public void wellKnownModules() throws JsonProcessingException, UnsupportedEncodingException {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

		Long timestamp = 1322903730000L;
		DateTime dateTime = new DateTime(timestamp, DateTimeZone.UTC);
		assertEquals(timestamp.toString(), new String(objectMapper.writeValueAsBytes(dateTime), "UTF-8"));

		Path file = Paths.get("foo");
		assertTrue(new String(objectMapper.writeValueAsBytes(file), "UTF-8").endsWith("foo\""));

		Optional<String> optional = Optional.of("test");
		assertEquals("\"test\"", new String(objectMapper.writeValueAsBytes(optional), "UTF-8"));

		// Kotlin module
		IntRange range = new IntRange(1, 3);
		assertEquals("{\"start\":1,\"end\":3}", new String(objectMapper.writeValueAsBytes(range), "UTF-8"));
	}

	@Test  // SPR-12634
	public void customizeWellKnownModulesWithModule()
			throws JsonProcessingException, UnsupportedEncodingException {

		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.modulesToInstall(new CustomIntegerModule())
				.build();
		DateTime dateTime = new DateTime(1322903730000L, DateTimeZone.UTC);
		assertEquals("1322903730000", new String(objectMapper.writeValueAsBytes(dateTime), "UTF-8"));
		assertThat(new String(objectMapper.writeValueAsBytes(new Integer(4)), "UTF-8"), containsString("customid"));
	}

	@Test  // SPR-12634
	@SuppressWarnings("unchecked")
	public void customizeWellKnownModulesWithModuleClass()
			throws JsonProcessingException, UnsupportedEncodingException {

		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.modulesToInstall(CustomIntegerModule.class)
				.build();
		DateTime dateTime = new DateTime(1322903730000L, DateTimeZone.UTC);
		assertEquals("1322903730000", new String(objectMapper.writeValueAsBytes(dateTime), "UTF-8"));
		assertThat(new String(objectMapper.writeValueAsBytes(new Integer(4)), "UTF-8"), containsString("customid"));
	}

	@Test  // SPR-12634
	public void customizeWellKnownModulesWithSerializer()
			throws JsonProcessingException, UnsupportedEncodingException {

		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.serializerByType(Integer.class, new CustomIntegerSerializer()).build();
		DateTime dateTime = new DateTime(1322903730000L, DateTimeZone.UTC);
		assertEquals("1322903730000", new String(objectMapper.writeValueAsBytes(dateTime), "UTF-8"));
		assertThat(new String(objectMapper.writeValueAsBytes(new Integer(4)), "UTF-8"), containsString("customid"));
	}

	@Test  // gh-22576
	public void overrideWellKnownModuleWithModule() throws IOException {
		Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
		JavaTimeModule javaTimeModule = new JavaTimeModule();
		javaTimeModule.addDeserializer(OffsetDateTime.class, new OffsetDateTimeDeserializer());
		builder.modulesToInstall(javaTimeModule);
		builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		ObjectMapper objectMapper =  builder.build();
		DemoPojo demoPojo = objectMapper.readValue(DATA, DemoPojo.class);
		assertNotNull(demoPojo.getOffsetDateTime());
	}

	@Test  // gh-22740
	public void registerMultipleModulesWithNullTypeId() {
		Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
		SimpleModule fooModule = new SimpleModule();
		fooModule.addSerializer(new FooSerializer());
		SimpleModule barModule = new SimpleModule();
		barModule.addSerializer(new BarSerializer());
		builder.modulesToInstall(fooModule, barModule);
		ObjectMapper objectMapper = builder.build();
		assertEquals(1, StreamSupport
				.stream(getSerializerFactoryConfig(objectMapper).serializers().spliterator(), false)
				.filter(s -> s.findSerializer(null, SimpleType.construct(Foo.class), null) != null)
				.count());
		assertEquals(1, StreamSupport
				.stream(getSerializerFactoryConfig(objectMapper).serializers().spliterator(), false)
				.filter(s -> s.findSerializer(null, SimpleType.construct(Bar.class), null) != null)
				.count());
	}

	private static SerializerFactoryConfig getSerializerFactoryConfig(ObjectMapper objectMapper) {
		return ((BasicSerializerFactory) objectMapper.getSerializerFactory()).getFactoryConfig();
	}

	private static DeserializerFactoryConfig getDeserializerFactoryConfig(ObjectMapper objectMapper) {
		return ((BasicDeserializerFactory) objectMapper.getDeserializationContext().getFactory()).getFactoryConfig();
	}

	@Test
	public void propertyNamingStrategy() {
		PropertyNamingStrategy strategy = new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy();
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().propertyNamingStrategy(strategy).build();
		assertSame(strategy, objectMapper.getSerializationConfig().getPropertyNamingStrategy());
		assertSame(strategy, objectMapper.getDeserializationConfig().getPropertyNamingStrategy());
	}

	@Test
	public void serializerByType() {
		JsonSerializer<Number> serializer = new NumberSerializer(Integer.class);
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.modules(new ArrayList<>())  // Disable well-known modules detection
				.serializerByType(Boolean.class, serializer)
				.build();
		assertTrue(getSerializerFactoryConfig(objectMapper).hasSerializers());
		Serializers serializers = getSerializerFactoryConfig(objectMapper).serializers().iterator().next();
		assertSame(serializer, serializers.findSerializer(null, SimpleType.construct(Boolean.class), null));
	}

	@Test
	public void deserializerByType() throws JsonMappingException {
		JsonDeserializer<Date> deserializer = new DateDeserializers.DateDeserializer();
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.modules(new ArrayList<>())  // Disable well-known modules detection
				.deserializerByType(Date.class, deserializer)
				.build();
		assertTrue(getDeserializerFactoryConfig(objectMapper).hasDeserializers());
		Deserializers deserializers = getDeserializerFactoryConfig(objectMapper).deserializers().iterator().next();
		assertSame(deserializer, deserializers.findBeanDeserializer(SimpleType.construct(Date.class), null, null));
	}

	@Test
	public void mixIn() {
		Class<?> target = String.class;
		Class<?> mixInSource = Object.class;

		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.modules().mixIn(target, mixInSource)
				.build();

		assertEquals(1, objectMapper.mixInCount());
		assertSame(mixInSource, objectMapper.findMixInClassFor(target));
	}

	@Test
	public void mixIns() {
		Class<?> target = String.class;
		Class<?> mixInSource = Object.class;
		Map<Class<?>, Class<?>> mixIns = new HashMap<>();
		mixIns.put(target, mixInSource);

		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.modules().mixIns(mixIns)
				.build();

		assertEquals(1, objectMapper.mixInCount());
		assertSame(mixInSource, objectMapper.findMixInClassFor(target));
	}

	@Test
	public void filters() throws JsonProcessingException {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.filters(new SimpleFilterProvider().setFailOnUnknownId(false)).build();
		JacksonFilteredBean bean = new JacksonFilteredBean("value1", "value2");
		String output = objectMapper.writeValueAsString(bean);
		assertThat(output, containsString("value1"));
		assertThat(output, containsString("value2"));

		SimpleFilterProvider provider = new SimpleFilterProvider()
				.setFailOnUnknownId(false)
				.setDefaultFilter(SimpleBeanPropertyFilter.serializeAllExcept("property2"));
		objectMapper = Jackson2ObjectMapperBuilder.json().filters(provider).build();
		output = objectMapper.writeValueAsString(bean);
		assertThat(output, containsString("value1"));
		assertThat(output, not(containsString("value2")));
	}

	@Test
	public void completeSetup() throws JsonMappingException {
		NopAnnotationIntrospector annotationIntrospector = NopAnnotationIntrospector.instance;

		Map<Class<?>, JsonDeserializer<?>> deserializerMap = new HashMap<>();
		JsonDeserializer<Date> deserializer = new DateDeserializers.DateDeserializer();
		deserializerMap.put(Date.class, deserializer);

		JsonSerializer<Class<?>> serializer1 = new ClassSerializer();
		JsonSerializer<Number> serializer2 = new NumberSerializer(Integer.class);

		Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json()
				.modules(new ArrayList<>()) // Disable well-known modules detection
				.serializers(serializer1)
				.serializersByType(Collections.singletonMap(Boolean.class, serializer2))
				.deserializersByType(deserializerMap)
				.annotationIntrospector(annotationIntrospector)
				.featuresToEnable(SerializationFeature.FAIL_ON_EMPTY_BEANS,
						DeserializationFeature.UNWRAP_ROOT_VALUE,
						JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER,
						JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS)
				.featuresToDisable(MapperFeature.AUTO_DETECT_GETTERS,
						MapperFeature.AUTO_DETECT_FIELDS,
						JsonParser.Feature.AUTO_CLOSE_SOURCE,
						JsonGenerator.Feature.QUOTE_FIELD_NAMES)
						.serializationInclusion(JsonInclude.Include.NON_NULL);

		ObjectMapper mapper = new ObjectMapper();
		builder.configure(mapper);

		assertTrue(getSerializerFactoryConfig(mapper).hasSerializers());
		assertTrue(getDeserializerFactoryConfig(mapper).hasDeserializers());

		Serializers serializers = getSerializerFactoryConfig(mapper).serializers().iterator().next();
		assertSame(serializer1, serializers.findSerializer(null, SimpleType.construct(Class.class), null));
		assertSame(serializer2, serializers.findSerializer(null, SimpleType.construct(Boolean.class), null));
		assertNull(serializers.findSerializer(null, SimpleType.construct(Number.class), null));

		Deserializers deserializers = getDeserializerFactoryConfig(mapper).deserializers().iterator().next();
		assertSame(deserializer, deserializers.findBeanDeserializer(SimpleType.construct(Date.class), null, null));

		assertSame(annotationIntrospector, mapper.getSerializationConfig().getAnnotationIntrospector());
		assertSame(annotationIntrospector, mapper.getDeserializationConfig().getAnnotationIntrospector());

		assertTrue(mapper.getSerializationConfig().isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
		assertTrue(mapper.getDeserializationConfig().isEnabled(DeserializationFeature.UNWRAP_ROOT_VALUE));
		assertTrue(mapper.getFactory().isEnabled(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER));
		assertTrue(mapper.getFactory().isEnabled(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS));

		assertFalse(mapper.getSerializationConfig().isEnabled(MapperFeature.AUTO_DETECT_GETTERS));
		assertFalse(mapper.getDeserializationConfig().isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION));
		assertFalse(mapper.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
		assertFalse(mapper.getDeserializationConfig().isEnabled(MapperFeature.AUTO_DETECT_FIELDS));
		assertFalse(mapper.getFactory().isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
		assertFalse(mapper.getFactory().isEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
		assertSame(JsonInclude.Include.NON_NULL, mapper.getSerializationConfig().getSerializationInclusion());
	}

	@Test
	public void xmlMapper() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.xml().build();
		assertNotNull(objectMapper);
		assertEquals(XmlMapper.class, objectMapper.getClass());
	}

	@Test  // gh-22428
	public void xmlMapperAndCustomFactory() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.xml().factory(new MyXmlFactory()).build();
		assertNotNull(objectMapper);
		assertEquals(XmlMapper.class, objectMapper.getClass());
		assertEquals(MyXmlFactory.class, objectMapper.getFactory().getClass());
	}

	@Test
	public void createXmlMapper() {
		Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json().indentOutput(true);
		ObjectMapper jsonObjectMapper = builder.build();
		ObjectMapper xmlObjectMapper = builder.createXmlMapper(true).build();
		assertTrue(jsonObjectMapper.isEnabled(SerializationFeature.INDENT_OUTPUT));
		assertTrue(xmlObjectMapper.isEnabled(SerializationFeature.INDENT_OUTPUT));
		assertTrue(xmlObjectMapper.getClass().isAssignableFrom(XmlMapper.class));
	}

	@Test  // SPR-13975
	public void defaultUseWrapper() throws JsonProcessingException {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.xml().defaultUseWrapper(false).build();
		assertNotNull(objectMapper);
		assertEquals(XmlMapper.class, objectMapper.getClass());
		ListContainer<String> container = new ListContainer<>(Arrays.asList("foo", "bar"));
		String output = objectMapper.writeValueAsString(container);
		assertThat(output, containsString("<list>foo</list><list>bar</list></ListContainer>"));
	}

	@Test  // SPR-14435
	public void smile() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.smile().build();
		assertNotNull(objectMapper);
		assertEquals(SmileFactory.class, objectMapper.getFactory().getClass());
	}

	@Test  // SPR-14435
	public void cbor() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.cbor().build();
		assertNotNull(objectMapper);
		assertEquals(CBORFactory.class, objectMapper.getFactory().getClass());
	}

	@Test  // SPR-14435
	public void factory() {
		ObjectMapper objectMapper = new Jackson2ObjectMapperBuilder().factory(new SmileFactory()).build();
		assertNotNull(objectMapper);
		assertEquals(SmileFactory.class, objectMapper.getFactory().getClass());
	}


	@Test
	public void visibility() throws JsonProcessingException {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
				.visibility(PropertyAccessor.GETTER, Visibility.NONE)
				.visibility(PropertyAccessor.FIELD, Visibility.ANY)
				.build();

		String json = objectMapper.writeValueAsString(new JacksonVisibilityBean());
		assertThat(json, containsString("property1"));
		assertThat(json, containsString("property2"));
		assertThat(json, not(containsString("property3")));
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
		public void serialize(Integer value, JsonGenerator gen, SerializerProvider serializers)
				throws IOException {

			gen.writeStartObject();
			gen.writeNumberField("customid", value);
			gen.writeEndObject();
		}
	}


	@JsonFilter("myJacksonFilter")
	public static class JacksonFilteredBean {

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


	public static class ListContainer<T> {

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

	public static class JacksonVisibilityBean {

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
			final String value = jsonParser.getValueAsString();
			if (StringUtils.isEmpty(value)) {
				return null;
			}
			try {
				return OffsetDateTime.parse(value);

			} catch (DateTimeParseException exception) {
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
		public void serialize(Foo value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		}

		@Override
		public Class<Foo> handledType() {
			return Foo.class;
		}
	}

	static class BarSerializer extends JsonSerializer<Bar> {
		@Override
		public void serialize(Bar value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		}
		@Override
		public Class<Bar> handledType() {
			return Bar.class;
		}
	}

}
