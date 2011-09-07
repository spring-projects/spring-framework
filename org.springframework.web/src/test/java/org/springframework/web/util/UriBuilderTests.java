/*
 * Copyright 2002-2011 the original author or authors.
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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

/** @author Arjen Poutsma */
public class UriBuilderTests {

	@Test
	public void plain() throws URISyntaxException {
		UriBuilder builder = UriBuilder.newInstance();
		URI result = builder.scheme("http").host("example.com").path("foo").queryParam("bar").fragment("baz").build();

		URI expected = new URI("http://example.com/foo?bar#baz");
		assertEquals("Invalid result URI", expected, result);
	}

	@Test
	public void fromPath() throws URISyntaxException {
		URI result = UriBuilder.fromPath("foo").queryParam("bar").fragment("baz").build();

		URI expected = new URI("/foo?bar#baz");
		assertEquals("Invalid result URI", expected, result);
	}

	@Test
	public void fromUri() throws URISyntaxException {
		URI uri = new URI("http://example.com/foo?bar#baz");

		URI result = UriBuilder.fromUri(uri).build();
		assertEquals("Invalid result URI", uri, result);
	}

	@Test
	public void templateVarsVarArgs() throws URISyntaxException {
		UriBuilder builder = UriBuilder.newInstance();
		URI result = builder.scheme("http").host("example.com").path("{foo}").build("bar");

		URI expected = new URI("http://example.com/bar");
		assertEquals("Invalid result URI", expected, result);
	}

	@Test
	public void templateVarsEncoded() throws URISyntaxException, UnsupportedEncodingException {
		URI result = UriBuilder.fromPath("{foo}").build("bar baz");

		URI expected = new URI("/bar%20baz");
		assertEquals("Invalid result URI", expected, result);
	}

	@Test
	public void templateVarsNotEncoded() throws URISyntaxException {
		UriBuilder builder = UriBuilder.newInstance();
		URI result = builder.scheme("http").host("example.com").path("{foo}").buildFromEncoded("bar%20baz");

		URI expected = new URI("http://example.com/bar%20baz");
		assertEquals("Invalid result URI", expected, result);
	}

	@Test
	public void templateVarsMap() throws URISyntaxException {
		Map<String, String> vars = Collections.singletonMap("foo", "bar");
		UriBuilder builder = UriBuilder.newInstance();
		URI result = builder.scheme("http").host("example.com").path("{foo}").build(vars);

		URI expected = new URI("http://example.com/bar");
		assertEquals("Invalid result URI", expected, result);
	}

	@Test
	public void unusedTemplateVars() throws URISyntaxException {
		UriBuilder builder = UriBuilder.newInstance();
		URI result = builder.scheme("http").host("example.com").path("{foo}").build();

		URI expected = new URI("http://example.com/%7Bfoo%7D");
		assertEquals("Invalid result URI", expected, result);
	}

	@Test
	public void pathSegments() throws URISyntaxException {
		UriBuilder builder = UriBuilder.newInstance();
		URI result = builder.pathSegment("foo").pathSegment("bar").build();

		URI expected = new URI("/foo/bar");
		assertEquals("Invalid result URI", expected, result);
	}

	@Test
	public void queryParam() throws URISyntaxException {
		UriBuilder builder = UriBuilder.newInstance();
		URI result = builder.queryParam("baz", "qux", 42).build();

		URI expected = new URI("?baz=qux&baz=42");
		assertEquals("Invalid result URI", expected, result);
	}

	@Test
	public void emptyQueryParam() throws URISyntaxException {
		UriBuilder builder = UriBuilder.newInstance();
		URI result = builder.queryParam("baz").build();

		URI expected = new URI("?baz");
		assertEquals("Invalid result URI", expected, result);
	}


}
