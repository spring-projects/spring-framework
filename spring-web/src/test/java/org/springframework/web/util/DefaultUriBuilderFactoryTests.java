/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.util;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode;

import static java.util.Collections.singletonMap;
import static junit.framework.TestCase.assertEquals;

/**
 * Unit tests for {@link DefaultUriBuilderFactory}.
 * @author Rossen Stoyanchev
 */
public class DefaultUriBuilderFactoryTests {

	@Test
	public void defaultSettings() {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
		URI uri = factory.uriString("/foo/{id}").build("a/b");
		assertEquals("/foo/a%2Fb", uri.toString());
	}

	@Test
	public void baseUri() {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("http://foo.com/v1?id=123");
		URI uri = factory.uriString("/bar").port(8080).build();
		assertEquals("http://foo.com:8080/v1/bar?id=123", uri.toString());
	}

	@Test
	public void baseUriWithFullOverride() {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("http://foo.com/v1?id=123");
		URI uri = factory.uriString("http://example.com/1/2").build();
		assertEquals("Use of host should case baseUri to be completely ignored",
				"http://example.com/1/2", uri.toString());
	}

	@Test
	public void baseUriWithPathOverride() {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("http://foo.com/v1");
		URI uri = factory.builder().replacePath("/baz").build();
		assertEquals("http://foo.com/baz", uri.toString());
	}

	@Test
	public void defaultUriVars() {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("http://{host}/v1");
		factory.setDefaultUriVariables(singletonMap("host", "foo.com"));
		URI uri = factory.uriString("/{id}").build(singletonMap("id", "123"));
		assertEquals("http://foo.com/v1/123", uri.toString());
	}

	@Test
	public void defaultUriVarsWithOverride() {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("http://{host}/v1");
		factory.setDefaultUriVariables(singletonMap("host", "spring.io"));
		URI uri = factory.uriString("/bar").build(singletonMap("host", "docs.spring.io"));
		assertEquals("http://docs.spring.io/v1/bar", uri.toString());
	}

	@Test
	public void defaultUriVarsWithEmptyVarArg() {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("http://{host}/v1");
		factory.setDefaultUriVariables(singletonMap("host", "foo.com"));
		URI uri = factory.uriString("/bar").build();
		assertEquals("Expected delegation to build(Map) method", "http://foo.com/v1/bar", uri.toString());
	}

	@Test
	public void defaultUriVarsSpr14147() {
		Map<String, String> defaultUriVars = new HashMap<>(2);
		defaultUriVars.put("host", "api.example.com");
		defaultUriVars.put("port", "443");
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
		factory.setDefaultUriVariables(defaultUriVars);

		URI uri = factory.expand("https://{host}:{port}/v42/customers/{id}", singletonMap("id", 123L));
		assertEquals("https://api.example.com:443/v42/customers/123", uri.toString());
	}

	@Test
	public void encodeTemplateAndValues() {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
		factory.setEncodingMode(EncodingMode.TEMPLATE_AND_VALUES);
		UriBuilder uriBuilder = factory.uriString("/hotel list/{city} specials?q={value}");

		String expected = "/hotel%20list/Z%C3%BCrich%20specials?q=a%2Bb";

		Map<String, Object> vars = new HashMap<>();
		vars.put("city", "Z\u00fcrich");
		vars.put("value", "a+b");

		assertEquals(expected, uriBuilder.build("Z\u00fcrich", "a+b").toString());
		assertEquals(expected, uriBuilder.build(vars).toString());
	}

	@Test
	public void encodingValuesOnly() {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
		factory.setEncodingMode(EncodingMode.VALUES_ONLY);
		UriBuilder uriBuilder = factory.uriString("/foo/a%2Fb/{id}");

		String id = "c/d";
		String expected = "/foo/a%2Fb/c%2Fd";

		assertEquals(expected, uriBuilder.build(id).toString());
		assertEquals(expected, uriBuilder.build(singletonMap("id", id)).toString());
	}

	@Test
	public void encodingTemplateAndValuesSpr17465() {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
		factory.setEncodingMode(EncodingMode.TEMPLATE_AND_VALUES);
		URI uri = factory.builder().path("/foo/{id}").build("a/b");
		assertEquals("/foo/a%2Fb", uri.toString());
	}

	@Test
	public void encodingValuesOnlySpr14147() {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
		factory.setEncodingMode(EncodingMode.VALUES_ONLY);
		factory.setDefaultUriVariables(singletonMap("host", "www.example.com"));
		UriBuilder uriBuilder = factory.uriString("http://{host}/user/{userId}/dashboard");

		assertEquals("http://www.example.com/user/john%3Bdoe/dashboard",
				uriBuilder.build(singletonMap("userId", "john;doe")).toString());
	}

	@Test
	public void encodingNone() {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
		factory.setEncodingMode(EncodingMode.NONE);
		UriBuilder uriBuilder = factory.uriString("/foo/a%2Fb/{id}");

		String id = "c%2Fd";
		String expected = "/foo/a%2Fb/c%2Fd";

		assertEquals(expected, uriBuilder.build(id).toString());
		assertEquals(expected, uriBuilder.build(singletonMap("id", id)).toString());
	}

	@Test
	public void parsePathWithDefaultSettings() {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("/foo/{bar}");
		URI uri = factory.uriString("/baz/{id}").build("a/b", "c/d");
		assertEquals("/foo/a%2Fb/baz/c%2Fd", uri.toString());
	}

	@Test
	public void parsePathIsTurnedOff() {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("/foo/{bar}");
		factory.setParsePath(false);
		URI uri = factory.uriString("/baz/{id}").build("a/b", "c/d");
		assertEquals("/foo/a/b/baz/c/d", uri.toString());
	}

	@Test // SPR-15201
	public void pathWithTrailingSlash() {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
		URI uri = factory.expand("http://localhost:8080/spring/");
		assertEquals("http://localhost:8080/spring/", uri.toString());
	}

	@Test
	public void pathWithDuplicateSlashes() {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
		URI uri = factory.expand("/foo/////////bar");
		assertEquals("/foo/bar", uri.toString());
	}

}
