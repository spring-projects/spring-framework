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

package org.springframework.web.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

/**
 * Unit tests for {@link UriComponents}.
 *
 * @author Arjen Poutsma
 * @author Phillip Webb
 * @author Rossen Stoyanchev
 */
public class UriComponentsTests {

	@Test
	public void expandAndEncode() {

		UriComponents uri = UriComponentsBuilder
				.fromPath("/hotel list/{city} specials").queryParam("q", "{value}").build()
				.expand("Z\u00fcrich", "a+b").encode();

		assertThat(uri.toString()).isEqualTo("/hotel%20list/Z%C3%BCrich%20specials?q=a+b");
	}

	@Test
	public void encodeAndExpand() {

		UriComponents uri = UriComponentsBuilder
				.fromPath("/hotel list/{city} specials").queryParam("q", "{value}").encode().build()
				.expand("Z\u00fcrich", "a+b");

		assertThat(uri.toString()).isEqualTo("/hotel%20list/Z%C3%BCrich%20specials?q=a%2Bb");
	}

	@Test
	public void encodeAndExpandPartially() {

		UriComponents uri = UriComponentsBuilder
				.fromPath("/hotel list/{city} specials").queryParam("q", "{value}").encode()
				.uriVariables(Collections.singletonMap("city", "Z\u00fcrich"))
				.build();

		assertThat(uri.expand("a+b").toString()).isEqualTo("/hotel%20list/Z%C3%BCrich%20specials?q=a%2Bb");
	}

	@Test // SPR-17168
	public void encodeAndExpandWithDollarSign() {
		UriComponents uri = UriComponentsBuilder.fromPath("/path").queryParam("q", "{value}").encode().build();
		assertThat(uri.expand("JavaClass$1.class").toString()).isEqualTo("/path?q=JavaClass%241.class");
	}

	@Test
	public void toUriEncoded() throws URISyntaxException {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				"https://example.com/hotel list/Z\u00fcrich").build();
		assertThat(uriComponents.encode().toUri()).isEqualTo(new URI("https://example.com/hotel%20list/Z%C3%BCrich"));
	}

	@Test
	public void toUriNotEncoded() throws URISyntaxException {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				"https://example.com/hotel list/Z\u00fcrich").build();
		assertThat(uriComponents.toUri()).isEqualTo(new URI("https://example.com/hotel%20list/Z\u00fcrich"));
	}

	@Test
	public void toUriAlreadyEncoded() throws URISyntaxException {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				"https://example.com/hotel%20list/Z%C3%BCrich").build(true);
		UriComponents encoded = uriComponents.encode();
		assertThat(encoded.toUri()).isEqualTo(new URI("https://example.com/hotel%20list/Z%C3%BCrich"));
	}

	@Test
	public void toUriWithIpv6HostAlreadyEncoded() throws URISyntaxException {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				"http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich").build(true);
		UriComponents encoded = uriComponents.encode();
		assertThat(encoded.toUri()).isEqualTo(new URI("http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich"));
	}

	@Test
	public void expand() {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				"https://example.com").path("/{foo} {bar}").build();
		uriComponents = uriComponents.expand("1 2", "3 4");
		assertThat(uriComponents.getPath()).isEqualTo("/1 2 3 4");
		assertThat(uriComponents.toUriString()).isEqualTo("https://example.com/1 2 3 4");
	}

	@Test // SPR-13311
	public void expandWithRegexVar() {
		String template = "/myurl/{name:[a-z]{1,5}}/show";
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(template).build();
		uriComponents = uriComponents.expand(Collections.singletonMap("name", "test"));
		assertThat(uriComponents.getPath()).isEqualTo("/myurl/test/show");
	}

	@Test // SPR-17630
	public void uirTemplateExpandWithMismatchedCurlyBraces() {
		assertThat(UriComponentsBuilder.fromUriString("/myurl/?q={{{{").encode().build().toUriString()).isEqualTo("/myurl/?q=%7B%7B%7B%7B");
	}

	@Test // gh-22447
	public void expandWithFragmentOrder() {
		UriComponents uriComponents = UriComponentsBuilder
				.fromUriString("https://{host}/{path}#{fragment}").build()
				.expand("example.com", "foo", "bar");

		assertThat(uriComponents.toUriString()).isEqualTo("https://example.com/foo#bar");
	}

	@Test // SPR-12123
	public void port() {
		UriComponents uri1 = fromUriString("https://example.com:8080/bar").build();
		UriComponents uri2 = fromUriString("https://example.com/bar").port(8080).build();
		UriComponents uri3 = fromUriString("https://example.com/bar").port("{port}").build().expand(8080);
		UriComponents uri4 = fromUriString("https://example.com/bar").port("808{digit}").build().expand(0);
		assertThat(uri1.getPort()).isEqualTo(8080);
		assertThat(uri1.toUriString()).isEqualTo("https://example.com:8080/bar");
		assertThat(uri2.getPort()).isEqualTo(8080);
		assertThat(uri2.toUriString()).isEqualTo("https://example.com:8080/bar");
		assertThat(uri3.getPort()).isEqualTo(8080);
		assertThat(uri3.toUriString()).isEqualTo("https://example.com:8080/bar");
		assertThat(uri4.getPort()).isEqualTo(8080);
		assertThat(uri4.toUriString()).isEqualTo("https://example.com:8080/bar");
	}

	@Test
	public void expandEncoded() {
		assertThatIllegalStateException().isThrownBy(() ->
				UriComponentsBuilder.fromPath("/{foo}").build().encode().expand("bar"));
	}

	@Test
	public void invalidCharacters() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				UriComponentsBuilder.fromPath("/{foo}").build(true));
	}

	@Test
	public void invalidEncodedSequence() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				UriComponentsBuilder.fromPath("/fo%2o").build(true));
	}

	@Test
	public void normalize() {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString("https://example.com/foo/../bar").build();
		assertThat(uriComponents.normalize().toString()).isEqualTo("https://example.com/bar");
	}

	@Test
	public void serializable() throws Exception {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				"https://example.com").path("/{foo}").query("bar={baz}").build();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(uriComponents);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
		UriComponents readObject = (UriComponents) ois.readObject();
		assertThat(uriComponents.toString()).isEqualTo(readObject.toString());
	}

	@Test
	public void copyToUriComponentsBuilder() {
		UriComponents source = UriComponentsBuilder.fromPath("/foo/bar").pathSegment("ba/z").build();
		UriComponentsBuilder targetBuilder = UriComponentsBuilder.newInstance();
		source.copyToUriComponentsBuilder(targetBuilder);
		UriComponents result = targetBuilder.build().encode();
		assertThat(result.getPath()).isEqualTo("/foo/bar/ba%2Fz");
		assertThat(result.getPathSegments()).isEqualTo(Arrays.asList("foo", "bar", "ba%2Fz"));
	}

	@Test
	public void equalsHierarchicalUriComponents() {
		String url = "https://example.com";
		UriComponents uric1 = UriComponentsBuilder.fromUriString(url).path("/{foo}").query("bar={baz}").build();
		UriComponents uric2 = UriComponentsBuilder.fromUriString(url).path("/{foo}").query("bar={baz}").build();
		UriComponents uric3 = UriComponentsBuilder.fromUriString(url).path("/{foo}").query("bin={baz}").build();
		assertThat(uric1).isInstanceOf(HierarchicalUriComponents.class);
		assertThat(uric1).isEqualTo(uric1);
		assertThat(uric1).isEqualTo(uric2);
		assertThat(uric1).isNotEqualTo(uric3);
	}

	@Test
	public void equalsOpaqueUriComponents() {
		String baseUrl = "http:example.com";
		UriComponents uric1 = UriComponentsBuilder.fromUriString(baseUrl + "/foo/bar").build();
		UriComponents uric2 = UriComponentsBuilder.fromUriString(baseUrl + "/foo/bar").build();
		UriComponents uric3 = UriComponentsBuilder.fromUriString(baseUrl + "/foo/bin").build();
		assertThat(uric1).isInstanceOf(OpaqueUriComponents.class);
		assertThat(uric1).isEqualTo(uric1);
		assertThat(uric1).isEqualTo(uric2);
		assertThat(uric1).isNotEqualTo(uric3);
	}

}
