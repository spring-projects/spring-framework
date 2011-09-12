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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** @author Arjen Poutsma */
public class UriComponentsTests {

    @Test
    public void fromUri() {
        Map<UriComponents.Type, String> result = UriComponents.fromUriString("http://www.ietf.org/rfc/rfc3986.txt");
        assertEquals("http", result.get(UriComponents.Type.SCHEME));
        assertNull(result.get(UriComponents.Type.USER_INFO));
        assertEquals("www.ietf.org", result.get(UriComponents.Type.HOST));
        assertNull(result.get(UriComponents.Type.PORT));
        assertEquals("/rfc/rfc3986.txt", result.get(UriComponents.Type.PATH));
        assertNull(result.get(UriComponents.Type.QUERY));
        assertNull(result.get(UriComponents.Type.FRAGMENT));

        result = UriComponents.fromUriString(
                "http://arjen:foobar@java.sun.com:80/javase/6/docs/api/java/util/BitSet.html?foo=bar#and(java.util.BitSet)");
        assertEquals("http", result.get(UriComponents.Type.SCHEME));
        assertEquals("arjen:foobar", result.get(UriComponents.Type.USER_INFO));
        assertEquals("java.sun.com", result.get(UriComponents.Type.HOST));
        assertEquals("80", result.get(UriComponents.Type.PORT));
        assertEquals("/javase/6/docs/api/java/util/BitSet.html", result.get(UriComponents.Type.PATH));
        assertEquals("foo=bar", result.get(UriComponents.Type.QUERY));
        assertEquals("and(java.util.BitSet)", result.get(UriComponents.Type.FRAGMENT));

        result = UriComponents.fromUriString("mailto:java-net@java.sun.com");
        assertEquals("mailto", result.get(UriComponents.Type.SCHEME));
        assertNull(result.get(UriComponents.Type.USER_INFO));
        assertNull(result.get(UriComponents.Type.HOST));
        assertNull(result.get(UriComponents.Type.PORT));
        assertEquals("java-net@java.sun.com", result.get(UriComponents.Type.PATH));
        assertNull(result.get(UriComponents.Type.QUERY));
        assertNull(result.get(UriComponents.Type.FRAGMENT));

        result = UriComponents.fromUriString("docs/guide/collections/designfaq.html#28");
        assertNull(result.get(UriComponents.Type.SCHEME));
        assertNull(result.get(UriComponents.Type.USER_INFO));
        assertNull(result.get(UriComponents.Type.HOST));
        assertNull(result.get(UriComponents.Type.PORT));
        assertEquals("docs/guide/collections/designfaq.html", result.get(UriComponents.Type.PATH));
        assertNull(result.get(UriComponents.Type.QUERY));
        assertEquals("28", result.get(UriComponents.Type.FRAGMENT));
    }

	@Test
	public void pathSegments() {
        String path = "/foo/bar";
        UriComponents components = UriComponents.fromUriComponentMap(Collections.singletonMap(UriComponents.Type.PATH, path));
		List<String> expected = Arrays.asList("foo", "bar");

		List<String> pathSegments = components.getPathSegments();
		assertEquals(expected, pathSegments);
	}
	
	@Test
	public void queryParams() {
		String query = "foo=bar&foo=baz&qux";
        UriComponents components = UriComponents.fromUriComponentMap(
                Collections.singletonMap(UriComponents.Type.QUERY, query));
		MultiValueMap<String, String> expected = new LinkedMultiValueMap<String, String>(1);
		expected.put("foo", Arrays.asList("bar", "baz"));
		expected.set("qux", null);

		MultiValueMap<String, String> result  = components.getQueryParams();
		assertEquals(expected, result);
	}
    
    @Test
    public void encode() {
        UriComponents uriComponents = UriComponents.fromUriString("http://example.com/hotel list");
        UriComponents encoded = uriComponents.encode();
        assertEquals("/hotel%20list", encoded.getPath());
    }

    @Test
    public void toUriEncoded() throws URISyntaxException {
        UriComponents uriComponents = UriComponents.fromUriString("http://example.com/hotel list/Z\u00fcrich");
        UriComponents encoded = uriComponents.encode();
        assertEquals(new URI("http://example.com/hotel%20list/Z%C3%BCrich"), encoded.toUri());
    }
    
    @Test
    public void toUriNotEncoded() throws URISyntaxException {
        UriComponents uriComponents = UriComponents.fromUriString("http://example.com/hotel list/Z\u00fcrich");
        assertEquals(new URI("http://example.com/hotel%20list/Z\u00fcrich"), uriComponents.toUri());
    }

}
