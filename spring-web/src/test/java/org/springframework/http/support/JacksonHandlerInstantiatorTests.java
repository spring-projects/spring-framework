/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http.support;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.annotation.JsonTypeIdResolver;
import tools.jackson.databind.annotation.JsonTypeResolver;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.jsontype.TypeIdResolver;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.jsontype.impl.StdTypeResolverBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for {@link JacksonHandlerInstantiator}.
 *
 * @author Sebastien Deleuze
 */
class JacksonHandlerInstantiatorTests {

	private JacksonHandlerInstantiator instantiator;

	private ObjectMapper objectMapper;


	@BeforeEach
	void setup() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("capitalizer", new RootBeanDefinition(Capitalizer.class));
		instantiator = new JacksonHandlerInstantiator(bf);
		objectMapper = JsonMapper.builder().handlerInstantiator(instantiator).build();
	}


	@Test
	void autowiredSerializer() {
		User user = new User("bob");
		String json = this.objectMapper.writeValueAsString(user);
		assertThat(json).isEqualTo("{\"username\":\"BOB\"}");
	}

	@Test
	void autowiredDeserializer() {
		String json = "{\"username\":\"bob\"}";
		User user = this.objectMapper.readValue(json, User.class);
		assertThat(user.getUsername()).isEqualTo("BOB");
	}

	@Test
	void autowiredKeyDeserializer() {
		String json = "{\"credentials\":{\"bob\":\"admin\"}}";
		SecurityRegistry registry = this.objectMapper.readValue(json, SecurityRegistry.class);
		assertThat(registry.getCredentials()).containsKey("BOB");
		assertThat(registry.getCredentials()).doesNotContainKey("bob");
	}

	@Test
	void applicationContextAwaretypeResolverBuilder() {
		this.objectMapper.writeValueAsString(new Group());
		assertThat(CustomTypeResolverBuilder.isAutowiredFiledInitialized).isTrue();
	}

	@Test
	void applicationContextAwareTypeIdResolver() {
		this.objectMapper.writeValueAsString(new Group());
		assertThat(CustomTypeIdResolver.isAutowiredFiledInitialized).isTrue();
	}


	public static class UserDeserializer extends ValueDeserializer<User> {

		@Autowired
		private Capitalizer capitalizer;

		@Override
		public User deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
			JsonNode node = jsonParser.readValueAsTree();
			return new User(this.capitalizer.capitalize(node.get("username").asString()));
		}
	}


	public static class UserSerializer extends ValueSerializer<User> {

		@Autowired
		private Capitalizer capitalizer;

		@Override
		public void serialize(User user, JsonGenerator jsonGenerator, SerializationContext serializationContext) {

			jsonGenerator.writeStartObject();
			jsonGenerator.writeStringProperty("username", this.capitalizer.capitalize(user.getUsername()));
			jsonGenerator.writeEndObject();
		}
	}


	public static class UpperCaseKeyDeserializer extends KeyDeserializer {

		@Autowired
		private Capitalizer capitalizer;

		@Override
		public Object deserializeKey(String key, DeserializationContext context) {
			return this.capitalizer.capitalize(key);
		}
	}


	public static class CustomTypeResolverBuilder extends StdTypeResolverBuilder {

		@Autowired
		private Capitalizer capitalizer;

		public static boolean isAutowiredFiledInitialized = false;

		@Override
		public TypeSerializer buildTypeSerializer(SerializationContext serializationContext, JavaType baseType,
				Collection<NamedType> subtypes) {

			isAutowiredFiledInitialized = (this.capitalizer != null);
			return super.buildTypeSerializer(serializationContext, baseType, subtypes);
		}

		@Override
		public TypeDeserializer buildTypeDeserializer(DeserializationContext deserializationContext,
				JavaType baseType, Collection<NamedType> subtypes) {

			return super.buildTypeDeserializer(deserializationContext, baseType, subtypes);
		}
	}


	public static class CustomTypeIdResolver implements TypeIdResolver {

		@Autowired
		private Capitalizer capitalizer;

		public static boolean isAutowiredFiledInitialized = false;

		public CustomTypeIdResolver() {
		}

		@Override
		public String idFromValueAndType(DatabindContext ctxt, Object o, Class<?> type) {
			return type.getClass().getName();
		}

		@Override
		public JsonTypeInfo.Id getMechanism() {
			return JsonTypeInfo.Id.CUSTOM;
		}

		@Override
		public String idFromValue(DatabindContext databindContext, Object value) {
			isAutowiredFiledInitialized = (this.capitalizer != null);
			return value.getClass().getName();
		}

		@Override
		public void init(JavaType type) {
		}

		@Override
		public String idFromBaseType(DatabindContext ctxt) {
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
