/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.annotation.JsonTypeResolver;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for {@link SpringHandlerInstantiatorTests}.
 *
 * @author Sebastien Deleuze
 */
public class SpringHandlerInstantiatorTests {

	private SpringHandlerInstantiator instantiator;

	private ObjectMapper objectMapper;


	@BeforeEach
	public void setup() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("capitalizer", new RootBeanDefinition(Capitalizer.class));
		instantiator = new SpringHandlerInstantiator(bf);
		objectMapper = Jackson2ObjectMapperBuilder.json().handlerInstantiator(instantiator).build();
	}


	@Test
	public void autowiredSerializer() throws JsonProcessingException {
		User user = new User("bob");
		String json = this.objectMapper.writeValueAsString(user);
		assertThat(json).isEqualTo("{\"username\":\"BOB\"}");
	}

	@Test
	public void autowiredDeserializer() throws IOException {
		String json = "{\"username\":\"bob\"}";
		User user = this.objectMapper.readValue(json, User.class);
		assertThat(user.getUsername()).isEqualTo("BOB");
	}

	@Test
	public void autowiredKeyDeserializer() throws IOException {
		String json = "{\"credentials\":{\"bob\":\"admin\"}}";
		SecurityRegistry registry = this.objectMapper.readValue(json, SecurityRegistry.class);
		assertThat(registry.getCredentials().keySet().contains("BOB")).isTrue();
		assertThat(registry.getCredentials().keySet().contains("bob")).isFalse();
	}

	@Test
	public void applicationContextAwaretypeResolverBuilder() throws JsonProcessingException {
		this.objectMapper.writeValueAsString(new Group());
		assertThat(CustomTypeResolverBuilder.isAutowiredFiledInitialized).isTrue();
	}

	@Test
	public void applicationContextAwareTypeIdResolver() throws JsonProcessingException {
		this.objectMapper.writeValueAsString(new Group());
		assertThat(CustomTypeIdResolver.isAutowiredFiledInitialized).isTrue();
	}


	public static class UserDeserializer extends JsonDeserializer<User> {

		@Autowired
		private Capitalizer capitalizer;

		@Override
		public User deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws  IOException {
			ObjectCodec oc = jsonParser.getCodec();
			JsonNode node = oc.readTree(jsonParser);
			return new User(this.capitalizer.capitalize(node.get("username").asText()));
		}
	}


	public static class UserSerializer extends JsonSerializer<User> {

		@Autowired
		private Capitalizer capitalizer;

		@Override
		public void serialize(User user, JsonGenerator jsonGenerator,
				SerializerProvider serializerProvider) throws IOException {

			jsonGenerator.writeStartObject();
			jsonGenerator.writeStringField("username", this.capitalizer.capitalize(user.getUsername()));
			jsonGenerator.writeEndObject();
		}
	}


	public static class UpperCaseKeyDeserializer extends KeyDeserializer {

		@Autowired
		private Capitalizer capitalizer;

		@Override
		public Object deserializeKey(String key, DeserializationContext context) throws IOException {
			return this.capitalizer.capitalize(key);
		}
	}


	public static class CustomTypeResolverBuilder extends StdTypeResolverBuilder {

		@Autowired
		private Capitalizer capitalizer;

		public static boolean isAutowiredFiledInitialized = false;

		@Override
		public TypeSerializer buildTypeSerializer(SerializationConfig config, JavaType baseType,
				Collection<NamedType> subtypes) {

			isAutowiredFiledInitialized = (this.capitalizer != null);
			return super.buildTypeSerializer(config, baseType, subtypes);
		}

		@Override
		public TypeDeserializer buildTypeDeserializer(DeserializationConfig config,
				JavaType baseType, Collection<NamedType> subtypes) {

			return super.buildTypeDeserializer(config, baseType, subtypes);
		}
	}


	public static class CustomTypeIdResolver implements TypeIdResolver {

		@Autowired
		private Capitalizer capitalizer;

		public static boolean isAutowiredFiledInitialized = false;

		public CustomTypeIdResolver() {
		}

		@Override
		public String idFromValueAndType(Object o, Class<?> type) {
			return type.getClass().getName();
		}

		@Override
		public JsonTypeInfo.Id getMechanism() {
			return JsonTypeInfo.Id.CUSTOM;
		}

		@Override
		public String idFromValue(Object value) {
			isAutowiredFiledInitialized = (this.capitalizer != null);
			return value.getClass().getName();
		}

		@Override
		public void init(JavaType type) {
		}

		@Override
		public String idFromBaseType() {
			return null;
		}

		@Override
		public JavaType typeFromId(DatabindContext context, String id) {
			return null;
		}

		@Override
		public String getDescForKnownTypeIds() {
			return null;
		}
	}


	@JsonDeserialize(using = UserDeserializer.class)
	@JsonSerialize(using = UserSerializer.class)
	public static class User {

		private String username;

		public User() {
		}

		public User(String username) {
			this.username = username;
		}

		public String getUsername() { return this.username; }
	}


	public static class SecurityRegistry {

		@JsonDeserialize(keyUsing = UpperCaseKeyDeserializer.class)
		private Map<String, String> credentials = new HashMap<>();

		public void addCredential(String username, String credential) {
			this.credentials.put(username, credential);
		}

		public Map<String, String> getCredentials() {
			return credentials;
		}
	}


	@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
	@JsonTypeResolver(CustomTypeResolverBuilder.class)
	@JsonTypeIdResolver(CustomTypeIdResolver.class)
	public static class Group {

		public String getType() {
			return Group.class.getName();
		}
	}


	public static class Capitalizer {

		public String capitalize(String text) {
			return text.toUpperCase();
		}
	}

}
