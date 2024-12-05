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

package org.springframework.aot.nativex;

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RuntimeHintsWriter}.
 *
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 */
class RuntimeHintsWriterTests {

	private static JsonSchema JSON_SCHEMA;

	@BeforeAll
	static void setupSchemaValidator() {
		JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909, builder ->
				builder.schemaMappers(schemaMappers -> schemaMappers.mapPrefix("https://www.graalvm.org/", "classpath:org/springframework/aot/nativex/"))
		);
		SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
		JSON_SCHEMA = jsonSchemaFactory.getSchema(SchemaLocation.of("https://www.graalvm.org/reachability-metadata-schema-v1.0.0.json"), config);
	}

	@Nested
	class ReflectionHintsTests {

		@Test
		void empty() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			assertEquals("{}", hints);
		}

		@Test
		void one() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.reflection().registerType(StringDecoder.class, builder -> builder
					.onReachableType(String.class)
					.withMembers(MemberCategory.INVOKE_PUBLIC_FIELDS, MemberCategory.INVOKE_DECLARED_FIELDS,
							MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_METHODS)
					.withField("DEFAULT_CHARSET")
					.withField("defaultCharset")
					.withField("aScore")
					.withMethod("setDefaultCharset", List.of(TypeReference.of(Charset.class)), ExecutableMode.INVOKE));
			assertEquals("""
				{
					"reflection": [
						{
							"type": "org.springframework.core.codec.StringDecoder",
							"condition": { "typeReached": "java.lang.String" },
							"allPublicFields": true,
							"allDeclaredFields": true,
							"allPublicConstructors": true,
							"allDeclaredConstructors": true,
							"allPublicMethods": true,
							"allDeclaredMethods": true,
							"fields": [
								{ "name": "aScore" },
								{ "name": "DEFAULT_CHARSET" },
								{ "name": "defaultCharset" }
							],
							"methods": [
								{ "name": "setDefaultCharset", "parameterTypes": [ "java.nio.charset.Charset" ] }
							]
						}
					]
				}
				""", hints);
		}

		@Test
		void two() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.reflection().registerType(Integer.class, builder -> {
			});
			hints.reflection().registerType(Long.class, builder -> {
			});

			assertEquals("""
				{
					"reflection": [
						{ "type": "java.lang.Integer" },
						{ "type": "java.lang.Long" }
					]
				}
				""", hints);
		}

		@Test
		void methods() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.reflection().registerType(Integer.class, builder -> builder.withMethod("parseInt",
					TypeReference.listOf(String.class), ExecutableMode.INVOKE));

			assertEquals("""
				{
					"reflection": [
						{
							"type": "java.lang.Integer",
							"methods": [
								{
									"name": "parseInt",
									"parameterTypes": ["java.lang.String"]
								}
							]
						}
					]
				}
				""", hints);
		}

		@Test
		void methodWithInnerClassParameter() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.reflection().registerType(Integer.class, builder -> builder.withMethod("test",
					TypeReference.listOf(InnerClass.class), ExecutableMode.INVOKE));

			assertEquals("""
				{
					"reflection": [
						{
							"type": "java.lang.Integer",
							"methods": [
								{
									"name": "test",
									"parameterTypes": ["org.springframework.aot.nativex.RuntimeHintsWriterTests$InnerClass"]
								}
							]
						}
					]
				}
				""", hints);
		}

		@Test
		void methodAndQueriedMethods() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.reflection().registerType(Integer.class, builder -> builder.withMethod("parseInt",
					TypeReference.listOf(String.class), ExecutableMode.INVOKE));

			assertEquals("""
				{
					"reflection": [
						{
							"type": "java.lang.Integer",
							"methods": [
								{
									"name": "parseInt",
									"parameterTypes": ["java.lang.String"]
								}
							]
						}
					]
				}
				""", hints);
		}

		@Test
		void ignoreLambda() throws JSONException {
			Runnable anonymousRunnable = () -> {};
			RuntimeHints hints = new RuntimeHints();
			hints.reflection().registerType(anonymousRunnable.getClass());
			assertEquals("{}", hints);
		}

		@Test
		void sortTypeHints() {
			RuntimeHints hints = new RuntimeHints();
			hints.reflection().registerType(Integer.class, builder -> {});
			hints.reflection().registerType(Long.class, builder -> {});

			RuntimeHints hints2 = new RuntimeHints();
			hints2.reflection().registerType(Long.class, builder -> {});
			hints2.reflection().registerType(Integer.class, builder -> {});

			assertThat(writeJson(hints)).isEqualTo(writeJson(hints2));
		}

		@Test
		void sortFieldHints() {
			RuntimeHints hints = new RuntimeHints();
			hints.reflection().registerType(Integer.class, builder -> {
				builder.withField("first");
				builder.withField("second");
			});
			RuntimeHints hints2 = new RuntimeHints();
			hints2.reflection().registerType(Integer.class, builder -> {
				builder.withField("second");
				builder.withField("first");
			});
			assertThat(writeJson(hints)).isEqualTo(writeJson(hints2));
		}

		@Test
		void sortConstructorHints() {
			RuntimeHints hints = new RuntimeHints();
			hints.reflection().registerType(Integer.class, builder -> {
				builder.withConstructor(List.of(TypeReference.of(String.class)), ExecutableMode.INVOKE);
				builder.withConstructor(List.of(TypeReference.of(String.class),
						TypeReference.of(Integer.class)), ExecutableMode.INVOKE);
			});

			RuntimeHints hints2 = new RuntimeHints();
			hints2.reflection().registerType(Integer.class, builder -> {
				builder.withConstructor(List.of(TypeReference.of(String.class),
						TypeReference.of(Integer.class)), ExecutableMode.INVOKE);
				builder.withConstructor(List.of(TypeReference.of(String.class)), ExecutableMode.INVOKE);
			});
			assertThat(writeJson(hints)).isEqualTo(writeJson(hints2));
		}

		@Test
		void sortMethodHints() {
			RuntimeHints hints = new RuntimeHints();
			hints.reflection().registerType(Integer.class, builder -> {
				builder.withMethod("test", Collections.emptyList(), ExecutableMode.INVOKE);
				builder.withMethod("another", Collections.emptyList(), ExecutableMode.INVOKE);
			});

			RuntimeHints hints2 = new RuntimeHints();
			hints2.reflection().registerType(Integer.class, builder -> {
				builder.withMethod("another", Collections.emptyList(), ExecutableMode.INVOKE);
				builder.withMethod("test", Collections.emptyList(), ExecutableMode.INVOKE);
			});
			assertThat(writeJson(hints)).isEqualTo(writeJson(hints2));
		}

	}


	@Nested
	class JniHints {

		// TODO

	}


	@Nested
	class ResourceHintsTests {

		@Test
		void empty() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			assertEquals("{}", hints);
		}

		@Test
		void registerExactMatch() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.resources().registerPattern("com/example/test.properties");
			hints.resources().registerPattern("com/example/another.properties");
			assertEquals("""
				{
					"resources": [
						{ "glob": "/" },
						{ "glob": "com"},
						{ "glob": "com/example"},
						{ "glob": "com/example/another.properties"},
						{ "glob": "com/example/test.properties"}
					]
				}""", hints);
		}

		@Test
		void registerWildcardAtTheBeginningPattern() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.resources().registerPattern("*.properties");
			assertEquals("""
				{
					"resources": [
						{ "glob": "*.properties"},
						{ "glob": "/"}
					]
				}""", hints);
		}

		@Test
		void registerWildcardInTheMiddlePattern() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.resources().registerPattern("com/example/*.properties");
			assertEquals("""
				{
					"resources": [
						{ "glob": "/" },
						{ "glob": "com"},
						{ "glob": "com/example"},
						{ "glob": "com/example/*.properties"}
					]
				}""", hints);
		}

		@Test
		void registerWildcardAtTheEndPattern() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.resources().registerPattern("static/*");
			assertEquals("""
				{
					"resources": [
						{ "glob": "/" },
						{ "glob": "static"},
						{ "glob": "static/*"}
					]
				}""", hints);
		}

		@Test
		void registerPatternWithIncludesAndExcludes() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.resources().registerPattern(hint -> hint.includes("com/example/*.properties"));
			hints.resources().registerPattern(hint -> hint.includes("org/other/*.properties"));
			assertEquals("""
				{
					"resources": [
						{ "glob": "/"},
						{ "glob": "com"},
						{ "glob": "com/example"},
						{ "glob": "com/example/*.properties"},
						{ "glob": "org"},
						{ "glob": "org/other"},
						{ "glob": "org/other/*.properties"}
					]
				}""", hints);
		}

		@Test
		void registerWithReachableTypeCondition() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.resources().registerPattern(builder -> builder.includes(TypeReference.of("com.example.Test"), "com/example/test.properties"));
			assertEquals("""
				{
					"resources": [
						{ "condition": { "typeReached": "com.example.Test"}, "glob": "/"},
						{ "condition": { "typeReached": "com.example.Test"}, "glob": "com"},
						{ "condition": { "typeReached": "com.example.Test"}, "glob": "com/example"},
						{ "condition": { "typeReached": "com.example.Test"}, "glob": "com/example/test.properties"}
					]
				}""", hints);
		}

		@Test
		void registerType() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.resources().registerType(String.class);
			assertEquals("""
				{
					"resources": [
						{ "glob": "/" },
						{ "glob": "java" },
						{ "glob": "java/lang" },
						{ "glob": "java/lang/String.class" }
					]
				}""", hints);
		}

		@Test
		void registerResourceBundle() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.resources().registerResourceBundle("com.example.message2");
			hints.resources().registerResourceBundle("com.example.message");
			assertEquals("""
				{
					"bundles": [
						{ "name": "com.example.message"},
						{ "name": "com.example.message2"}
					]
				}""", hints);
		}
	}

	@Nested
	class SerializationHintsTests {

		@Test
		void shouldWriteEmptyHint() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			assertEquals("{}", hints);
		}

		@Test
		void shouldWriteSingleHint() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.serialization().registerType(TypeReference.of(String.class));
			assertEquals("""
				{
					"serialization": [
						{ "type": "java.lang.String" }
					]
				}
				""", hints);
		}

		@Test
		void shouldWriteMultipleHints() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.serialization()
					.registerType(TypeReference.of(Environment.class))
					.registerType(TypeReference.of(String.class));
			assertEquals("""
				{
					"serialization": [
						{ "type": "java.lang.String" },
						{ "type": "org.springframework.core.env.Environment" }
					]
				}
				""", hints);
		}

		@Test
		void shouldWriteSingleHintWithCondition() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.serialization().registerType(TypeReference.of(String.class),
					builder -> builder.onReachableType(TypeReference.of("org.example.Test")));
			assertEquals("""
				{
					"serialization": [
						{ "condition": { "typeReached": "org.example.Test" }, "type": "java.lang.String" }
					]
				}
				""", hints);
		}

	}

	@Nested
	class ProxyHintsTests {

		@Test
		void empty() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			assertEquals("{}", hints);
		}

		@Test
		void shouldWriteOneEntry() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.proxies().registerJdkProxy(Function.class);
			assertEquals("""
				{
					"reflection": [
						{
							"type": {
								"proxy": ["java.util.function.Function"]
							}
						}
					]
				}
				""", hints);
		}

		@Test
		void shouldWriteMultipleEntries() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.proxies().registerJdkProxy(Function.class)
					.registerJdkProxy(Function.class, Consumer.class);
			assertEquals("""
				{
					"reflection": [
						{
							"type": { "proxy": ["java.util.function.Function"] }
						},
						{
							"type": { "proxy": ["java.util.function.Function", "java.util.function.Consumer"] }
						}
					]
				}
				""", hints);
		}

		@Test
		void shouldWriteEntriesInNaturalOrder() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.proxies().registerJdkProxy(Supplier.class)
					.registerJdkProxy(Function.class);
			assertEquals("""
				{
					"reflection": [
						{
							"type": { "proxy": ["java.util.function.Function"] }
						},
						{
							"type": { "proxy": ["java.util.function.Supplier"] }
						}
					]
				}
				""", hints);
		}

		@Test
		void shouldWriteInnerClass() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.proxies().registerJdkProxy(InnerInterface.class);
			assertEquals("""
				{
					"reflection": [
						{
							"type": { "proxy": ["org.springframework.aot.nativex.RuntimeHintsWriterTests$InnerInterface"] }
						}
					]
				}
				""", hints);
		}

		@Test
		void shouldWriteCondition() throws JSONException {
			RuntimeHints hints = new RuntimeHints();
			hints.proxies().registerJdkProxy(builder -> builder.proxiedInterfaces(Function.class)
					.onReachableType(TypeReference.of("org.example.Test")));
			assertEquals("""
				{
					"reflection": [
						{
							"type": { "proxy": ["java.util.function.Function"] },
							"condition": { "typeReached": "org.example.Test" }
						}
					]
				}
				""", hints);
		}

	}

	private void assertEquals(String expectedString, RuntimeHints hints) throws JSONException {
		String json = writeJson(hints);
		JSONAssert.assertEquals(expectedString, json, JSONCompareMode.LENIENT);
		Set<ValidationMessage> validationMessages = JSON_SCHEMA.validate(json, InputFormat.JSON, executionContext ->
				executionContext.getExecutionConfig().setFormatAssertionsEnabled(true));
		assertThat(validationMessages).isEmpty();
	}

	private String writeJson(RuntimeHints hints) {
		StringWriter out = new StringWriter();
		BasicJsonWriter writer = new BasicJsonWriter(out, "\t");
		new RuntimeHintsWriter().write(writer, hints);
		return out.toString();
	}


	static class InnerClass {

	}

	interface InnerInterface {

	}

}
