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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class UriUtilsTests {

	private static final String ENC = "UTF-8";

	

	@Test
	public void encodeScheme() throws UnsupportedEncodingException {
		assertEquals("Invalid encoded result", "foobar+-.", UriUtils.encodeScheme("foobar+-.", ENC));
		assertEquals("Invalid encoded result", "foo%20bar", UriUtils.encodeScheme("foo bar", ENC));
	}

	@Test
	public void encodeUserInfo() throws UnsupportedEncodingException {
		assertEquals("Invalid encoded result", "foobar:", UriUtils.encodeUserInfo("foobar:", ENC));
		assertEquals("Invalid encoded result", "foo%20bar", UriUtils.encodeUserInfo("foo bar", ENC));
	}

	@Test
	public void encodeHost() throws UnsupportedEncodingException {
		assertEquals("Invalid encoded result", "foobar", UriUtils.encodeHost("foobar", ENC));
		assertEquals("Invalid encoded result", "foo%20bar", UriUtils.encodeHost("foo bar", ENC));
	}

	@Test
	public void encodePort() throws UnsupportedEncodingException {
		assertEquals("Invalid encoded result", "80", UriUtils.encodePort("80", ENC));
	}

	@Test
	public void encodePath() throws UnsupportedEncodingException {
		assertEquals("Invalid encoded result", "/foo/bar", UriUtils.encodePath("/foo/bar", ENC));
		assertEquals("Invalid encoded result", "/foo%20bar", UriUtils.encodePath("/foo bar", ENC));
		assertEquals("Invalid encoded result", "/Z%C3%BCrich", UriUtils.encodePath("/Z\u00fcrich", ENC));
	}

	@Test
	public void encodePathSegment() throws UnsupportedEncodingException {
		assertEquals("Invalid encoded result", "foobar", UriUtils.encodePathSegment("foobar", ENC));
		assertEquals("Invalid encoded result", "%2Ffoo%2Fbar", UriUtils.encodePathSegment("/foo/bar", ENC));
	}

	@Test
	public void encodeQuery() throws UnsupportedEncodingException {
		assertEquals("Invalid encoded result", "foobar", UriUtils.encodeQuery("foobar", ENC));
		assertEquals("Invalid encoded result", "foo%20bar", UriUtils.encodeQuery("foo bar", ENC));
		assertEquals("Invalid encoded result", "foobar/+", UriUtils.encodeQuery("foobar/+", ENC));
		assertEquals("Invalid encoded result", "T%C5%8Dky%C5%8D", UriUtils.encodeQuery("T\u014dky\u014d", ENC));
	}

	@Test
	public void encodeQueryParam() throws UnsupportedEncodingException {
		assertEquals("Invalid encoded result", "foobar", UriUtils.encodeQueryParam("foobar", ENC));
		assertEquals("Invalid encoded result", "foo%20bar", UriUtils.encodeQueryParam("foo bar", ENC));
		assertEquals("Invalid encoded result", "foo%26bar", UriUtils.encodeQueryParam("foo&bar", ENC));
	}

	@Test
	public void encodeFragment() throws UnsupportedEncodingException {
		assertEquals("Invalid encoded result", "foobar", UriUtils.encodeFragment("foobar", ENC));
		assertEquals("Invalid encoded result", "foo%20bar", UriUtils.encodeFragment("foo bar", ENC));
		assertEquals("Invalid encoded result", "foobar/", UriUtils.encodeFragment("foobar/", ENC));
	}

	@Test
	public void decode() throws UnsupportedEncodingException {
		assertEquals("Invalid encoded URI", "", UriUtils.decode("", ENC));
		assertEquals("Invalid encoded URI", "foobar", UriUtils.decode("foobar", ENC));
		assertEquals("Invalid encoded URI", "foo bar", UriUtils.decode("foo%20bar", ENC));
		assertEquals("Invalid encoded URI", "foo+bar", UriUtils.decode("foo%2bbar", ENC));
		assertEquals("Invalid encoded result", "T\u014dky\u014d", UriUtils.decode("T%C5%8Dky%C5%8D", ENC));
		assertEquals("Invalid encoded result", "/Z\u00fcrich", UriUtils.decode("/Z%C3%BCrich", ENC));
		assertEquals("Invalid encoded result", "T\u014dky\u014d", UriUtils.decode("T\u014dky\u014d", ENC));
	}

	@Test(expected = IllegalArgumentException.class)
	public void decodeInvalidSequence() throws UnsupportedEncodingException {
		UriUtils.decode("foo%2", ENC);
	}

	@Test
	public void encodeUri() throws UnsupportedEncodingException {
		assertEquals("Invalid encoded URI", "http://www.ietf.org/rfc/rfc3986.txt",
				UriUtils.encodeUri("http://www.ietf.org/rfc/rfc3986.txt", ENC));
		assertEquals("Invalid encoded URI", "https://www.ietf.org/rfc/rfc3986.txt",
				UriUtils.encodeUri("https://www.ietf.org/rfc/rfc3986.txt", ENC));
		assertEquals("Invalid encoded URI", "http://www.google.com/?q=Z%C3%BCrich",
				UriUtils.encodeUri("http://www.google.com/?q=Z\u00fcrich", ENC));
		assertEquals("Invalid encoded URI",
				"http://arjen:foobar@java.sun.com:80/javase/6/docs/api/java/util/BitSet.html?foo=bar#and(java.util.BitSet)",
				UriUtils.encodeUri(
						"http://arjen:foobar@java.sun.com:80/javase/6/docs/api/java/util/BitSet.html?foo=bar#and(java.util.BitSet)",
						ENC));
		assertEquals("Invalid encoded URI", "http://java.sun.com/j2se/1.3/",
				UriUtils.encodeUri("http://java.sun.com/j2se/1.3/", ENC));
		assertEquals("Invalid encoded URI", "docs/guide/collections/designfaq.html#28",
				UriUtils.encodeUri("docs/guide/collections/designfaq.html#28", ENC));
		assertEquals("Invalid encoded URI", "../../../demo/jfc/SwingSet2/src/SwingSet2.java",
				UriUtils.encodeUri("../../../demo/jfc/SwingSet2/src/SwingSet2.java", ENC));
		assertEquals("Invalid encoded URI", "file:///~/calendar", UriUtils.encodeUri("file:///~/calendar", ENC));
		assertEquals("Invalid encoded URI", "http://example.com/query=foo@bar",
				UriUtils.encodeUri("http://example.com/query=foo@bar", ENC));

		// SPR-8974
		assertEquals("http://example.org?format=json&url=http://another.com?foo=bar",
				UriUtils.encodeUri("http://example.org?format=json&url=http://another.com?foo=bar", ENC));
	}

	@Test
	public void encodeHttpUrl() throws UnsupportedEncodingException {
		assertEquals("Invalid encoded HTTP URL", "http://www.ietf.org/rfc/rfc3986.txt",
				UriUtils.encodeHttpUrl("http://www.ietf.org/rfc/rfc3986.txt", ENC));
		assertEquals("Invalid encoded URI", "https://www.ietf.org/rfc/rfc3986.txt",
				UriUtils.encodeHttpUrl("https://www.ietf.org/rfc/rfc3986.txt", ENC));
		assertEquals("Invalid encoded HTTP URL", "http://www.google.com/?q=Z%C3%BCrich",
				UriUtils.encodeHttpUrl("http://www.google.com/?q=Z\u00fcrich", ENC));
		assertEquals("Invalid encoded HTTP URL", "http://ws.geonames.org/searchJSON?q=T%C5%8Dky%C5%8D&style=FULL&maxRows=300",
				UriUtils.encodeHttpUrl("http://ws.geonames.org/searchJSON?q=T\u014dky\u014d&style=FULL&maxRows=300", ENC));
		assertEquals("Invalid encoded HTTP URL",
				"http://arjen:foobar@java.sun.com:80/javase/6/docs/api/java/util/BitSet.html?foo=bar",
				UriUtils.encodeHttpUrl(
						"http://arjen:foobar@java.sun.com:80/javase/6/docs/api/java/util/BitSet.html?foo=bar", ENC));
		assertEquals("Invalid encoded HTTP URL", "http://search.twitter.com/search.atom?q=%23avatar",
				UriUtils.encodeHttpUrl("http://search.twitter.com/search.atom?q=#avatar", ENC));
		assertEquals("Invalid encoded HTTP URL", "http://java.sun.com/j2se/1.3/",
				UriUtils.encodeHttpUrl("http://java.sun.com/j2se/1.3/", ENC));
		assertEquals("Invalid encoded HTTP URL", "http://example.com/query=foo@bar",
				UriUtils.encodeHttpUrl("http://example.com/query=foo@bar", ENC));
	}

	@Test(expected = IllegalArgumentException.class)
	public void encodeHttpUrlMail() throws UnsupportedEncodingException {
		UriUtils.encodeHttpUrl("mailto:java-net@java.sun.com", ENC);
	}

}
