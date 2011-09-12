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

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** @author Arjen Poutsma */
public class UriComponentsBuilderTests {

	@Test
	public void plain() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		UriComponents result = builder.scheme("http").host("example.com").path("foo").queryParam("bar").fragment("baz").build();
        assertEquals("http", result.getScheme());
        assertEquals("example.com", result.getHost());
        assertEquals("/foo", result.getPath());
        assertEquals("bar", result.getQuery());
        assertEquals("baz", result.getFragment());

		URI expected = new URI("http://example.com/foo?bar#baz");
		assertEquals("Invalid result URI", expected, result.toUri());
	}

	@Test
	public void fromPath() throws URISyntaxException {
		UriComponents result = UriComponentsBuilder.fromPath("foo").queryParam("bar").fragment("baz").build();
        assertEquals("/foo", result.getPath());
        assertEquals("bar", result.getQuery());
        assertEquals("baz", result.getFragment());

		URI expected = new URI("/foo?bar#baz");
		assertEquals("Invalid result URI", expected, result.toUri());

		result = UriComponentsBuilder.fromPath("/foo").build();
        assertEquals("/foo", result.getPath());

        expected = new URI("/foo");
		assertEquals("Invalid result URI", expected, result.toUri());
	}

	@Test
	public void fromUri() throws URISyntaxException {
		URI uri = new URI("http://example.com/foo?bar#baz");
        UriComponents result = UriComponentsBuilder.fromUri(uri).build();
        assertEquals("http", result.getScheme());
        assertEquals("example.com", result.getHost());
        assertEquals("/foo", result.getPath());
        assertEquals("bar", result.getQuery());
        assertEquals("baz", result.getFragment());

		assertEquals("Invalid result URI", uri, result.toUri());
	}

	@Test
	public void pathSegments() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		URI result = builder.pathSegment("foo").pathSegment("bar").build().toUri();

		URI expected = new URI("/foo/bar");
		assertEquals("Invalid result URI", expected, result);
	}

	@Test
	public void queryParam() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		URI result = builder.queryParam("baz", "qux", 42).build().toUri();

		URI expected = new URI("?baz=qux&baz=42");
		assertEquals("Invalid result URI", expected, result);
	}

	@Test
	public void emptyQueryParam() throws URISyntaxException {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		URI result = builder.queryParam("baz").build().toUri();

		URI expected = new URI("?baz");
		assertEquals("Invalid result URI", expected, result);
	}

    @Test
    public void combineWithUriTemplate() throws URISyntaxException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/{foo}");
        UriComponents components = builder.build();
        UriTemplate template = new UriTemplate(components);
        URI uri = template.expand("bar baz");
        assertEquals(new URI("/bar%20baz"), uri);
    }


}
