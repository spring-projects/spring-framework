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

package org.springframework.http.server.reactive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.stream.Stream;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.undertow.util.HeaderMap;
import org.apache.tomcat.util.http.MimeHeaders;
import org.eclipse.jetty.http.HttpFields;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Unit tests for {@code HeadersAdapters} {@code MultiValueMap} implementations.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 */
class HeadersAdaptersTests {

	@ParameterizedHeadersTest
	void getWithUnknownHeaderShouldReturnNull(String displayName, MultiValueMap<String, String> headers) {
		assertThat(headers.get("Unknown")).isNull();
	}

	@ParameterizedHeadersTest
	void getFirstWithUnknownHeaderShouldReturnNull(String displayName, MultiValueMap<String, String> headers) {
		assertThat(headers.getFirst("Unknown")).isNull();
	}

	@ParameterizedHeadersTest
	void sizeWithMultipleValuesForHeaderShouldCountHeaders(String displayName, MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.add("TestHeader", "second");
		assertThat(headers.size()).isEqualTo(1);
	}

	@ParameterizedHeadersTest
	void keySetShouldNotDuplicateHeaderNames(String displayName, MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.add("OtherHeader", "test");
		headers.add("TestHeader", "second");
		assertThat(headers.keySet().size()).isEqualTo(2);
	}

	@ParameterizedHeadersTest
	void containsKeyShouldBeCaseInsensitive(String displayName, MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		assertThat(headers.containsKey("testheader")).isTrue();
	}

	@ParameterizedHeadersTest
	void addShouldKeepOrdering(String displayName, MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.add("TestHeader", "second");
		assertThat(headers.getFirst("TestHeader")).isEqualTo("first");
		assertThat(headers.get("TestHeader").get(0)).isEqualTo("first");
	}

	@ParameterizedHeadersTest
	void putShouldOverrideExisting(String displayName, MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.put("TestHeader", Arrays.asList("override"));
		assertThat(headers.getFirst("TestHeader")).isEqualTo("override");
		assertThat(headers.get("TestHeader").size()).isEqualTo(1);
	}

	@ParameterizedHeadersTest
	void nullValuesShouldNotFail(String displayName, MultiValueMap<String, String> headers) {
		headers.add("TestHeader", null);
		assertThat(headers.getFirst("TestHeader")).isNull();
		headers.set("TestHeader", null);
		assertThat(headers.getFirst("TestHeader")).isNull();
	}

	@ParameterizedHeadersTest
	void shouldReflectChangesOnKeyset(String displayName, MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		assertThat(headers.keySet()).hasSize(1);
		headers.keySet().removeIf("TestHeader"::equals);
		assertThat(headers.keySet()).hasSize(0);
	}

	@ParameterizedHeadersTest
	void shouldFailIfHeaderRemovedFromKeyset(String displayName, MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		assertThat(headers.keySet()).hasSize(1);
		Iterator<String> names = headers.keySet().iterator();
		assertThat(names.hasNext()).isTrue();
		assertThat(names.next()).isEqualTo("TestHeader");
		names.remove();
		assertThatThrownBy(names::remove).isInstanceOf(IllegalStateException.class);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("headers")
	@interface ParameterizedHeadersTest {
	}

	static Stream<Arguments> headers() {
		return Stream.of(
			arguments("Map", CollectionUtils.toMultiValueMap(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH))),
			arguments("Netty", new NettyHeadersAdapter(new DefaultHttpHeaders())),
			arguments("Tomcat", new TomcatHeadersAdapter(new MimeHeaders())),
			arguments("Undertow", new UndertowHeadersAdapter(new HeaderMap())),
			arguments("Jetty", new JettyHeadersAdapter(HttpFields.build()))
		);
	}

}
