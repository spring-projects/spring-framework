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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ProxyHints;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.SerializationHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.codec.StringDecoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FileNativeConfigurationWriter}.
 *
 * @author Sebastien Deleuze
 * @author Janne Valkealahti
 * @author Sam Brannen
 */
class FileNativeConfigurationWriterTests {

	@TempDir
	static Path tempDir;


	@Test
	void emptyConfig() {
		Path empty = tempDir.resolve("empty");
		FileNativeConfigurationWriter generator = new FileNativeConfigurationWriter(empty);
		generator.write(new RuntimeHints());
		assertThat(empty.toFile().listFiles()).isNull();
	}

	@Test
	void serializationConfig() throws IOException, JSONException {
		FileNativeConfigurationWriter generator = new FileNativeConfigurationWriter(tempDir);
		RuntimeHints hints = new RuntimeHints();
		SerializationHints serializationHints = hints.serialization();
		serializationHints.registerType(Integer.class);
		serializationHints.registerType(Long.class);
		generator.write(hints);
		assertEquals("""
				{
					"serialization": [
						{ "type": "java.lang.Integer" },
						{ "type": "java.lang.Long" }
					]
				}
				""");
	}

	@Test
	void proxyConfig() throws IOException, JSONException {
		FileNativeConfigurationWriter generator = new FileNativeConfigurationWriter(tempDir);
		RuntimeHints hints = new RuntimeHints();
		ProxyHints proxyHints = hints.proxies();
		proxyHints.registerJdkProxy(Function.class);
		proxyHints.registerJdkProxy(Function.class, Consumer.class);
		generator.write(hints);
		assertEquals("""
				{
					"reflection": [
						{ type: {"proxy": [ "java.util.function.Function" ] } },
						{ type: {"proxy": [ "java.util.function.Function", "java.util.function.Consumer" ] } }
					]
				}
				""");
	}

	@Test
	void reflectionConfig() throws IOException, JSONException {
		FileNativeConfigurationWriter generator = new FileNativeConfigurationWriter(tempDir);
		RuntimeHints hints = new RuntimeHints();
		ReflectionHints reflectionHints = hints.reflection();
		reflectionHints.registerType(StringDecoder.class, builder -> builder
				.onReachableType(String.class)
				.withMembers(MemberCategory.ACCESS_PUBLIC_FIELDS, MemberCategory.ACCESS_DECLARED_FIELDS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
						MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_METHODS)
				.withField("DEFAULT_CHARSET")
				.withField("defaultCharset")
				.withMethod("setDefaultCharset", TypeReference.listOf(Charset.class), ExecutableMode.INVOKE));
		generator.write(hints);
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
								{ "name": "DEFAULT_CHARSET" },
								{ "name": "defaultCharset" }
							],
							"methods": [
								{ "name": "setDefaultCharset", "parameterTypes": [ "java.nio.charset.Charset" ] }
							]
						}
					]
				}
				""");
	}

	@Test
	void jniConfig() throws IOException, JSONException {
		// same format as reflection so just test basic file generation
		FileNativeConfigurationWriter generator = new FileNativeConfigurationWriter(tempDir);
		RuntimeHints hints = new RuntimeHints();
		ReflectionHints jniHints = hints.jni();
		jniHints.registerType(StringDecoder.class, builder -> builder.onReachableType(String.class));
		generator.write(hints);
		assertEquals("""
				{
					"jni": [
						{
							"type": "org.springframework.core.codec.StringDecoder",
							"condition": { "typeReached": "java.lang.String" }
						}
					]
				}""");
	}

	@Test
	void resourceConfig() throws IOException, JSONException {
		FileNativeConfigurationWriter generator = new FileNativeConfigurationWriter(tempDir);
		RuntimeHints hints = new RuntimeHints();
		ResourceHints resourceHints = hints.resources();
		resourceHints.registerPattern("com/example/test.properties");
		resourceHints.registerPattern("com/example/another.properties");
		generator.write(hints);
		assertEquals("""
				{
					"resources": [
							{"glob": "com/example/test.properties"},
							{"glob": "/"},
							{"glob": "com"},
							{"glob": "com/example"},
							{"glob": "com/example/another.properties"}
					]
				}""");
	}

	@Test
	void namespace() {
		String groupId = "foo.bar";
		String artifactId = "baz";
		String filename = "reachability-metadata.json";
		FileNativeConfigurationWriter generator = new FileNativeConfigurationWriter(tempDir, groupId, artifactId);
		RuntimeHints hints = new RuntimeHints();
		ResourceHints resourceHints = hints.resources();
		resourceHints.registerPattern("com/example/test.properties");
		generator.write(hints);
		Path jsonFile = tempDir.resolve("META-INF").resolve("native-image").resolve(groupId).resolve(artifactId).resolve(filename);
		assertThat(jsonFile.toFile()).exists();
	}

	private void assertEquals(String expectedString) throws IOException, JSONException {
		Path jsonFile = tempDir.resolve("META-INF").resolve("native-image").resolve("reachability-metadata.json");
		String content = Files.readString(jsonFile);
		JSONAssert.assertEquals(expectedString, content, JSONCompareMode.NON_EXTENSIBLE);
	}

}
