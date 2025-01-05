/*
 * Copyright 2002-2025 the original author or authors.
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.tomcat.util.http.MimeHeaders;
import org.eclipse.jetty.http.HttpFields;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.support.HttpComponentsHeadersAdapter;
import org.springframework.http.support.JettyHeadersAdapter;
import org.springframework.http.support.Netty4HeadersAdapter;
import org.springframework.http.support.Netty5HeadersAdapter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

/**
 * Tests for {@code HeadersAdapters} {@code MultiValueMap} implementations.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 * @author Simon Baslé
 */
class HeadersAdaptersTests {

	@ParameterizedPopulatedHeadersTest
	void toSingleValueMapIsCaseInsensitive(MultiValueMap<String, String> headers) {
		assertThat(headers.toSingleValueMap()).as("toSingleValueMap")
				.containsEntry("TestHeader", "first")
				.containsEntry("SecondHeader", "value")
				.hasSize(2);
	}

	@ParameterizedHeadersTest
	void shouldRemoveCaseInsensitiveFromKeySet(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.add("TestHEADER", "second");
		headers.add("TestHeader", "third");

		Iterator<String> iterator = headers.keySet().iterator();
		iterator.next();
		iterator.remove();

		assertThat(headers)
				.doesNotContainKey("TestHeader")
				.doesNotContainKey("TestHEADER")
				.doesNotContainKey("testheader")
				.hasSize(0);
	}

	@ParameterizedPopulatedHeadersTest
	void toString(MultiValueMap<String, String> headers) {
		String expectedFirstHeader = "TestHeader:\"first\", \"second\", \"third\"";
		String expectedSecondHeader = "SecondHeader:\"value\"";
		int minimumLength = expectedFirstHeader.length() + expectedSecondHeader.length() + 4;

		String result = headers.toString();
		assertThat(result)
				.startsWith("[").endsWith("]")
				// Using contains here because some native headers iterate over names in reverse insertion order
				.contains(expectedFirstHeader)
				.contains(expectedSecondHeader)
				.hasSizeGreaterThanOrEqualTo(minimumLength);

		if (result.length() > minimumLength) {
			String expectedEnd = " with native header names " + headers.keySet();
			assertThat(result).as("toString() with a dump of native duplicate headers")
					.endsWith(expectedEnd)
					.hasSize(minimumLength + expectedEnd.length());
		}
	}

	@ParameterizedPopulatedHeadersTest
	void copyUsingHeaderSetAddAllIsCaseInsensitive(MultiValueMap<String, String> headers) {
		HttpHeaders headers2 = new HttpHeaders();
		for (Map.Entry<String, List<String>> entry : new HttpHeaders(headers).headerSet()) {
			headers2.addAll(entry.getKey(), entry.getValue());
		}

		assertThat(headers2.get("TestHeader")).as("TestHeader")
				.containsExactly("first", "second", "third");
		// Using the headerSet approach, we keep the first encountered casing of any given key
		assertThat(headers2.headerNames()).as("first casing variant").containsExactlyInAnyOrder("TestHeader", "SecondHeader");
		assertThat(headers2.toString()).as("similar toString, no 'with native headers' dump")
				.isEqualTo(headers.toString().substring(0, headers.toString().indexOf(']') + 1));
	}

	@ParameterizedPopulatedHeadersTest
	@SuppressWarnings("deprecation")
	void copyUsingEntrySetPutRemovesDuplicates(MultiValueMap<String, String> headers) {
		HttpHeaders headers2 = new HttpHeaders();
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			headers2.put(entry.getKey(), entry.getValue());
		}

		assertThat(headers2.get("TestHeader")).as("TestHeader")
				.containsExactly("first", "second", "third");
		// Ordering and casing are not guaranteed using the entrySet+put approach
		assertThat(headers2.asMultiValueMap()).as("two keys")
				.containsKey("testheader")
				.containsKey("secondheader")
				.hasSize(2);
		assertThat(headers2.toString()).as("no 'with native headers' dump")
				.doesNotContain("with native headers");
	}

	@ParameterizedPopulatedHeadersTest
	@SuppressWarnings("deprecation")
	void copyUsingPutAllRemovesDuplicates(MultiValueMap<String, String> headers) {
		HttpHeaders headers2 = new HttpHeaders();
		headers2.putAll(headers);

		assertThat(headers2.get("TestHeader")).as("TestHeader")
				.containsExactly("first", "second", "third");
		// Ordering and casing are not guaranteed using the putAll approach
		assertThat(headers2.asMultiValueMap()).as("two keys")
				.containsKey("testheader")
				.containsKey("secondheader")
				.hasSize(2);
		assertThat(headers2.toString()).as("similar toString, no 'with native headers' dump")
				.isEqualToIgnoringCase(headers.toString().substring(0, headers.toString().indexOf(']') + 1));
	}

	@ParameterizedPopulatedHeadersTest
	void getWithUnknownHeaderShouldReturnNull(MultiValueMap<String, String> headers) {
		assertThat(headers.get("Unknown")).isNull();
	}

	@ParameterizedPopulatedHeadersTest
	void getFirstWithUnknownHeaderShouldReturnNull(MultiValueMap<String, String> headers) {
		assertThat(headers.getFirst("Unknown")).isNull();
	}

	@ParameterizedHeadersTest
	void sizeWithMultipleValuesForHeaderShouldCountHeaders(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.add("TestHeader", "second");
		assertThat(headers).hasSize(1);
	}

	@ParameterizedHeadersTest
	void keySetShouldNotDuplicateHeaderNames(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.add("OtherHeader", "test");
		headers.add("TestHeader", "second");
		assertThat(headers.keySet()).hasSize(2);
	}

	@ParameterizedPopulatedHeadersTest
	void containsKeyShouldBeCaseInsensitive(MultiValueMap<String, String> headers) {
		assertThat(headers.containsKey("testheader")).isTrue();
	}

	@ParameterizedHeadersTest
	void addShouldKeepOrdering(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.add("TestHeader", "second");
		assertThat(headers.getFirst("TestHeader")).isEqualTo("first");
		assertThat(headers.get("TestHeader")).first().isEqualTo("first");
	}

	@ParameterizedHeadersTest
	void putShouldOverrideExisting(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.put("TestHeader", List.of("override"));
		assertThat(headers.getFirst("TestHeader")).isEqualTo("override");
		assertThat(headers.get("TestHeader")).hasSize(1);
	}

	@ParameterizedHeadersTest
	void nullValuesShouldNotFail(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", null);
		assertThat(headers.getFirst("TestHeader")).isNull();
		headers.set("TestHeader", null);
		assertThat(headers.getFirst("TestHeader")).isNull();
	}

	@ParameterizedHeadersTest
	void shouldReflectChangesOnKeyset(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		assertThat(headers.keySet()).hasSize(1);
		headers.keySet().removeIf("TestHeader"::equals);
		assertThat(headers.keySet()).isEmpty();
	}

	@ParameterizedHeadersTest
	void shouldFailIfHeaderRemovedFromKeyset(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		assertThat(headers.keySet()).hasSize(1);
		Iterator<String> names = headers.keySet().iterator();
		assertThat(names.hasNext()).isTrue();
		assertThat(names.next()).isEqualTo("TestHeader");
		names.remove();
		assertThatThrownBy(names::remove).isInstanceOf(IllegalStateException.class);
	}

	@ParameterizedPopulatedHeadersTest
	void headerSetEntryCanSetList(MultiValueMap<String, String> headers) {
		for (Map.Entry<String, List<String>> entry : new HttpHeaders(headers).headerSet()) {
			entry.setValue(List.of(entry.getKey()));
		}

		assertThat(headers).hasSize(2);
		assertThat(headers.get("TestHeader")).containsExactly("TestHeader");
		assertThat(headers.get("SecondHeader")).containsExactly("SecondHeader");
	}

	@ParameterizedPopulatedHeadersTest
	void headerSetIteratorCanRemove(MultiValueMap<String, String> headers) {
		Iterator<Map.Entry<String, List<String>>> iterator = new HttpHeaders(headers).headerSet().iterator();
		while (iterator.hasNext()) {
			iterator.next();
			iterator.remove();
		}

		assertThat(headers).isEmpty();
	}


	static <T> T withHeaders(T nativeHeader, Function<T, BiConsumer<String, String>> addMethod) {
		BiConsumer<String, String> add = addMethod.apply(nativeHeader);
		add.accept("TestHeader", "first");
		add.accept("TestHEADER", "second");
		add.accept("SecondHeader", "value");
		add.accept("TestHeader", "third");

		return nativeHeader;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest
	@MethodSource("headers")
	@interface ParameterizedHeadersTest {
	}

	static Stream<Arguments> headers() {
		return Stream.of(
				argumentSet("Map", CollectionUtils.toMultiValueMap(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH))),
				argumentSet("Netty", new Netty4HeadersAdapter(new DefaultHttpHeaders())),
				argumentSet("Netty5", new Netty5HeadersAdapter(io.netty5.handler.codec.http.headers.HttpHeaders.newHeaders())),
				argumentSet("Tomcat", new TomcatHeadersAdapter(new MimeHeaders())),
				argumentSet("Undertow", new UndertowHeadersAdapter(new HeaderMap())),
				argumentSet("Jetty", new JettyHeadersAdapter(HttpFields.build())),
				argumentSet("HttpComponents", new HttpComponentsHeadersAdapter(new HttpGet("https://example.com")))
		);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest
	@MethodSource("nativeHeadersWithCasedEntries")
	@interface ParameterizedPopulatedHeadersTest {
	}

	static Stream<Arguments> nativeHeadersWithCasedEntries() {
		return Stream.of(
				argumentSet("Netty", new Netty4HeadersAdapter(withHeaders(new DefaultHttpHeaders(), h -> h::add))),
				argumentSet("Netty5", new Netty5HeadersAdapter(withHeaders(io.netty5.handler.codec.http.headers.HttpHeaders.newHeaders(),
						h -> h::add))),
				argumentSet("Tomcat", new TomcatHeadersAdapter(withHeaders(new MimeHeaders(),
						h -> (k, v) -> h.addValue(k).setString(v)))),
				argumentSet("Undertow", new UndertowHeadersAdapter(withHeaders(new HeaderMap(),
						h -> (k, v) -> h.add(HttpString.tryFromString(k), v)))),
				argumentSet("Jetty", new JettyHeadersAdapter(withHeaders(HttpFields.build(), h -> h::add))),
				argumentSet("HttpComponents", new HttpComponentsHeadersAdapter(withHeaders(new HttpGet("https://example.com"),
						h -> h::addHeader)))
		);
	}

}
