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
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.web.util.UriComponents.UriTemplateVariables;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
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

		assertEquals("/hotel%20list/Z%C3%BCrich%20specials?q=a+b", uri.toString());
	}

	@Test
	public void encodeAndExpand() {

		UriComponents uri = UriComponentsBuilder
				.fromPath("/hotel list/{city} specials").queryParam("q", "{value}").encode().build()
				.expand("Z\u00fcrich", "a+b");

		assertEquals("/hotel%20list/Z%C3%BCrich%20specials?q=a%2Bb", uri.toString());
	}

	@Test
	public void encodeAndExpandPartially() {

		UriComponents uri = UriComponentsBuilder
				.fromPath("/hotel list/{city} specials").queryParam("q", "{value}").encode()
				.uriVariables(Collections.singletonMap("city", "Z\u00fcrich"))
				.build();

		assertEquals("/hotel%20list/Z%C3%BCrich%20specials?q=a%2Bb", uri.expand("a+b").toString());
	}

	@Test // SPR-17168
	public void encodeAndExpandWithDollarSign() {
		UriComponents uri = UriComponentsBuilder.fromPath("/path").queryParam("q", "{value}").encode().build();
		assertEquals("/path?q=JavaClass%241.class", uri.expand("JavaClass$1.class").toString());
	}

	@Test
	public void toUriEncoded() throws URISyntaxException {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				"https://example.com/hotel list/Z\u00fcrich").build();
		assertEquals(new URI("https://example.com/hotel%20list/Z%C3%BCrich"), uriComponents.encode().toUri());
	}

	@Test
	public void toUriNotEncoded() throws URISyntaxException {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				"https://example.com/hotel list/Z\u00fcrich").build();
		assertEquals(new URI("https://example.com/hotel%20list/Z\u00fcrich"), uriComponents.toUri());
	}

	@Test
	public void toUriAlreadyEncoded() throws URISyntaxException {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				"https://example.com/hotel%20list/Z%C3%BCrich").build(true);
		UriComponents encoded = uriComponents.encode();
		assertEquals(new URI("https://example.com/hotel%20list/Z%C3%BCrich"), encoded.toUri());
	}

	@Test
	public void toUriWithIpv6HostAlreadyEncoded() throws URISyntaxException {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				"http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich").build(true);
		UriComponents encoded = uriComponents.encode();
		assertEquals(new URI("http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich"), encoded.toUri());
	}

	@Test
	public void expand() {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				"https://example.com").path("/{foo} {bar}").build();
		uriComponents = uriComponents.expand("1 2", "3 4");
		assertEquals("/1 2 3 4", uriComponents.getPath());
		assertEquals("https://example.com/1 2 3 4", uriComponents.toUriString());
	}

	@Test // SPR-13311
	public void expandWithRegexVar() {
		String template = "/myurl/{name:[a-z]{1,5}}/show";
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(template).build();
		uriComponents = uriComponents.expand(Collections.singletonMap("name", "test"));
		assertEquals("/myurl/test/show", uriComponents.getPath());
	}

	@Test // SPR-17630
	public void uirTemplateExpandWithMismatchedCurlyBraces() {
		assertEquals("/myurl/?q=%7B%7B%7B%7B",
				UriComponentsBuilder.fromUriString("/myurl/?q={{{{").encode().build().toUriString());
	}

	@Test // SPR-12123
	public void port() {
		UriComponents uri1 = fromUriString("https://example.com:8080/bar").build();
		UriComponents uri2 = fromUriString("https://example.com/bar").port(8080).build();
		UriComponents uri3 = fromUriString("https://example.com/bar").port("{port}").build().expand(8080);
		UriComponents uri4 = fromUriString("https://example.com/bar").port("808{digit}").build().expand(0);
		assertEquals(8080, uri1.getPort());
		assertEquals("https://example.com:8080/bar", uri1.toUriString());
		assertEquals(8080, uri2.getPort());
		assertEquals("https://example.com:8080/bar", uri2.toUriString());
		assertEquals(8080, uri3.getPort());
		assertEquals("https://example.com:8080/bar", uri3.toUriString());
		assertEquals(8080, uri4.getPort());
		assertEquals("https://example.com:8080/bar", uri4.toUriString());
	}

	@Test(expected = IllegalStateException.class)
	public void expandEncoded() {
		UriComponentsBuilder.fromPath("/{foo}").build().encode().expand("bar");
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidCharacters() {
		UriComponentsBuilder.fromPath("/{foo}").build(true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidEncodedSequence() {
		UriComponentsBuilder.fromPath("/fo%2o").build(true);
	}

	@Test
	public void normalize() {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString("https://example.com/foo/../bar").build();
		assertEquals("https://example.com/bar", uriComponents.normalize().toString());
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
		assertThat(uriComponents.toString(), equalTo(readObject.toString()));
	}

	@Test
	public void copyToUriComponentsBuilder() {
		UriComponents source = UriComponentsBuilder.fromPath("/foo/bar").pathSegment("ba/z").build();
		UriComponentsBuilder targetBuilder = UriComponentsBuilder.newInstance();
		source.copyToUriComponentsBuilder(targetBuilder);
		UriComponents result = targetBuilder.build().encode();
		assertEquals("/foo/bar/ba%2Fz", result.getPath());
		assertEquals(Arrays.asList("foo", "bar", "ba%2Fz"), result.getPathSegments());
	}

	@Test
	public void equalsHierarchicalUriComponents() {
		String url = "https://example.com";
		UriComponents uric1 = UriComponentsBuilder.fromUriString(url).path("/{foo}").query("bar={baz}").build();
		UriComponents uric2 = UriComponentsBuilder.fromUriString(url).path("/{foo}").query("bar={baz}").build();
		UriComponents uric3 = UriComponentsBuilder.fromUriString(url).path("/{foo}").query("bin={baz}").build();
		assertThat(uric1, instanceOf(HierarchicalUriComponents.class));
		assertThat(uric1, equalTo(uric1));
		assertThat(uric1, equalTo(uric2));
		assertThat(uric1, not(equalTo(uric3)));
	}

	@Test
	public void equalsOpaqueUriComponents() {
		String baseUrl = "http:example.com";
		UriComponents uric1 = UriComponentsBuilder.fromUriString(baseUrl + "/foo/bar").build();
		UriComponents uric2 = UriComponentsBuilder.fromUriString(baseUrl + "/foo/bar").build();
		UriComponents uric3 = UriComponentsBuilder.fromUriString(baseUrl + "/foo/bin").build();
		assertThat(uric1, instanceOf(OpaqueUriComponents.class));
		assertThat(uric1, equalTo(uric1));
		assertThat(uric1, equalTo(uric2));
		assertThat(uric1, not(equalTo(uric3)));
	}

}
