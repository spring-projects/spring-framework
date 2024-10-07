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

package org.springframework.web.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.springframework.web.util.UriComponentsBuilder.ParserType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

/**
 * Tests for {@link UriComponents}.
 *
 * @author Arjen Poutsma
 * @author Phillip Webb
 * @author Rossen Stoyanchev
 */
class UriComponentsTests {

	@Test
	void expandAndEncode() {
		UriComponents uri = UriComponentsBuilder
				.fromPath("/hotel list/{city} specials").queryParam("q", "{value}").build()
				.expand("Z\u00fcrich", "a+b").encode();

		assertThat(uri.toString()).isEqualTo("/hotel%20list/Z%C3%BCrich%20specials?q=a+b");
	}

	@Test
	void encodeAndExpand() {
		UriComponents uri = UriComponentsBuilder
				.fromPath("/hotel list/{city} specials").queryParam("q", "{value}").encode().build()
				.expand("Z\u00fcrich", "a+b");

		assertThat(uri.toString()).isEqualTo("/hotel%20list/Z%C3%BCrich%20specials?q=a%2Bb");
	}

	@Test
	void encodeAndExpandPartially() {
		UriComponents uri = UriComponentsBuilder
				.fromPath("/hotel list/{city} specials").queryParam("q", "{value}").encode()
				.uriVariables(Collections.singletonMap("city", "Z\u00fcrich")).build();

		assertThat(uri.expand("a+b").toString()).isEqualTo("/hotel%20list/Z%C3%BCrich%20specials?q=a%2Bb");
	}

	@Test  // SPR-17168
	void encodeAndExpandWithDollarSign() {
		UriComponents uri = UriComponentsBuilder.fromPath("/path").queryParam("q", "{value}").encode().build();
		assertThat(uri.expand("JavaClass$1.class").toString()).isEqualTo("/path?q=JavaClass%241.class");
	}

	@ParameterizedTest
	@EnumSource(value = ParserType.class)
	void toUriEncoded(ParserType parserType) {
		UriComponents uri = UriComponentsBuilder.fromUriString("https://example.com/hotel list/Z\u00fcrich", parserType).build();
		assertThat(uri.encode().toUri()).isEqualTo(URI.create("https://example.com/hotel%20list/Z%C3%BCrich"));
	}

	@ParameterizedTest
	@EnumSource(value = ParserType.class)
	void toUriNotEncoded(ParserType parserType) {
		UriComponents uri = UriComponentsBuilder.fromUriString("https://example.com/hotel list/Z\u00fcrich", parserType).build();
		assertThat(uri.toUri()).isEqualTo(URI.create("https://example.com/hotel%20list/Z\u00fcrich"));
	}

	@ParameterizedTest
	@EnumSource(value = ParserType.class)
	void toUriAlreadyEncoded(ParserType parserType) {
		UriComponents uri = UriComponentsBuilder.fromUriString("https://example.com/hotel%20list/Z%C3%BCrich", parserType).build(true);
		assertThat(uri.encode().toUri()).isEqualTo(URI.create("https://example.com/hotel%20list/Z%C3%BCrich"));
	}

	@ParameterizedTest
	@EnumSource(value = ParserType.class)
	void toUriWithIpv6HostAlreadyEncoded(ParserType parserType) {
		UriComponents uri = UriComponentsBuilder.fromUriString(
				"http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich", parserType).build(true);

		assertThat(uri.encode().toUri()).isEqualTo(
				URI.create("http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich"));
	}

	@ParameterizedTest
	@EnumSource(value = ParserType.class)
	void toUriStringWithPortVariable(ParserType parserType) {
		String url = "http://localhost:{port}/first";
		assertThat(UriComponentsBuilder.fromUriString(url, parserType).build().toUriString()).isEqualTo(url);
	}

	@ParameterizedTest
	@EnumSource(value = ParserType.class)
	void expand(ParserType parserType) {
		UriComponents uri = UriComponentsBuilder.fromUriString("https://example.com", parserType).path("/{foo} {bar}").build();
		uri = uri.expand("1 2", "3 4");

		assertThat(uri.getPath()).isEqualTo("/1 2 3 4");
		assertThat(uri.toUriString()).isEqualTo("https://example.com/1 2 3 4");
	}

	@ParameterizedTest // SPR-13311
	@EnumSource(value = ParserType.class)
	void expandWithRegexVar(ParserType parserType) {
		String template = "/myurl/{name:[a-z]{1,5}}/show";
		UriComponents uri = UriComponentsBuilder.fromUriString(template, parserType).build();
		uri = uri.expand(Collections.singletonMap("name", "test"));

		assertThat(uri.getPath()).isEqualTo("/myurl/test/show");
	}

	@ParameterizedTest // SPR-17630
	@EnumSource(value = ParserType.class)
	void uirTemplateExpandWithMismatchedCurlyBraces(ParserType parserType) {
		UriComponents uri = UriComponentsBuilder.fromUriString("/myurl/?q={{{{", parserType).encode().build();
		assertThat(uri.toUriString()).isEqualTo("/myurl/?q=%7B%7B%7B%7B");
	}

	@ParameterizedTest // gh-22447
	@EnumSource(value = ParserType.class)
	void expandWithFragmentOrder(ParserType parserType) {
		UriComponents uri = UriComponentsBuilder
				.fromUriString("https://{host}/{path}#{fragment}", parserType).build()
				.expand("example.com", "foo", "bar");

		assertThat(uri.toUriString()).isEqualTo("https://example.com/foo#bar");
	}

	@ParameterizedTest // SPR-12123
	@EnumSource(value = ParserType.class)
	void port(ParserType parserType) {
		UriComponents uri1 = fromUriString("https://example.com:8080/bar", parserType).build();
		UriComponents uri2 = fromUriString("https://example.com/bar", parserType).port(8080).build();
		UriComponents uri3 = fromUriString("https://example.com/bar", parserType).port("{port}").build().expand(8080);
		UriComponents uri4 = fromUriString("https://example.com/bar", parserType).port("808{digit}").build().expand(0);

		assertThat(uri1.getPort()).isEqualTo(8080);
		assertThat(uri1.toUriString()).isEqualTo("https://example.com:8080/bar");
		assertThat(uri2.getPort()).isEqualTo(8080);
		assertThat(uri2.toUriString()).isEqualTo("https://example.com:8080/bar");
		assertThat(uri3.getPort()).isEqualTo(8080);
		assertThat(uri3.toUriString()).isEqualTo("https://example.com:8080/bar");
		assertThat(uri4.getPort()).isEqualTo(8080);
		assertThat(uri4.toUriString()).isEqualTo("https://example.com:8080/bar");
	}

	@ParameterizedTest // gh-28521
	@EnumSource(value = ParserType.class)
	void invalidPort(ParserType parserType) {
		assertThatExceptionOfType(InvalidUrlException.class)
				.isThrownBy(() -> fromUriString("https://example.com:XXX/bar", parserType));
		assertExceptionsForInvalidPort(fromUriString("https://example.com/bar", parserType).port("XXX").build());
	}

	private void assertExceptionsForInvalidPort(UriComponents uriComponents) {
		assertThatIllegalStateException()
			.isThrownBy(uriComponents::getPort)
			.withMessage("The port must be an integer: XXX");
		assertThatIllegalStateException()
			.isThrownBy(uriComponents::toUri)
			.withMessage("The port must be an integer: XXX");
	}

	@Test
	void expandEncoded() {
		assertThatIllegalStateException().isThrownBy(() ->
				UriComponentsBuilder.fromPath("/{foo}").build().encode().expand("bar"));
	}

	@Test
	void invalidCharacters() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				UriComponentsBuilder.fromPath("/{foo}").build(true));
	}

	@Test
	void invalidEncodedSequence() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				UriComponentsBuilder.fromPath("/fo%2o").build(true));
	}

	@ParameterizedTest
	@EnumSource(value = ParserType.class)
	void normalize(ParserType parserType) {
		UriComponents uri = UriComponentsBuilder.fromUriString("https://example.com/foo/../bar", parserType).build();
		assertThat(uri.normalize().toString()).isEqualTo("https://example.com/bar");
	}

	@ParameterizedTest
	@EnumSource(value = ParserType.class)
	void serializable(ParserType parserType) throws Exception {
		UriComponents uri = UriComponentsBuilder.fromUriString(
				"https://example.com", parserType).path("/{foo}").query("bar={baz}").build();

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(uri);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
		UriComponents readObject = (UriComponents) ois.readObject();

		assertThat(uri.toString()).isEqualTo(readObject.toString());
	}

	@Test
	void copyToUriComponentsBuilder() {
		UriComponents source = UriComponentsBuilder.fromPath("/foo/bar").pathSegment("ba/z").build();
		UriComponentsBuilder targetBuilder = UriComponentsBuilder.newInstance();
		source.copyToUriComponentsBuilder(targetBuilder);
		UriComponents result = targetBuilder.build().encode();

		assertThat(result.getPath()).isEqualTo("/foo/bar/ba%2Fz");
		assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("foo", "bar", "ba%2Fz"));
	}

	@ParameterizedTest
	@EnumSource(value = ParserType.class)
	void equalsHierarchicalUriComponents(ParserType parserType) {
		String url = "https://example.com";
		UriComponents uric1 = UriComponentsBuilder.fromUriString(url, parserType).path("/{foo}").query("bar={baz}").build();
		UriComponents uric2 = UriComponentsBuilder.fromUriString(url, parserType).path("/{foo}").query("bar={baz}").build();
		UriComponents uric3 = UriComponentsBuilder.fromUriString(url, parserType).path("/{foo}").query("bin={baz}").build();

		assertThat(uric1).isInstanceOf(HierarchicalUriComponents.class);
		assertThat(uric1).isEqualTo(uric1);
		assertThat(uric1).isEqualTo(uric2);
		assertThat(uric1).isNotEqualTo(uric3);
	}

	@ParameterizedTest
	@EnumSource(value = ParserType.class)
	void equalsOpaqueUriComponents(ParserType parserType) {
		String baseUrl = "http:example.com";
		UriComponents uric1 = UriComponentsBuilder.fromUriString(baseUrl + "/foo/bar", parserType).build();
		UriComponents uric2 = UriComponentsBuilder.fromUriString(baseUrl + "/foo/bar", parserType).build();
		UriComponents uric3 = UriComponentsBuilder.fromUriString(baseUrl + "/foo/bin", parserType).build();

		assertThat(uric1).isEqualTo(uric1);
		assertThat(uric1).isEqualTo(uric2);
		assertThat(uric1).isNotEqualTo(uric3);
	}

}
