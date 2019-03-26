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

package org.springframework.http;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link org.springframework.http.HttpHeaders}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Juergen Hoeller
 */
public class HttpHeadersTests {

	private final HttpHeaders headers = new HttpHeaders();


	@Test
	public void getFirst() {
		headers.add(HttpHeaders.CACHE_CONTROL, "max-age=1000, public");
		headers.add(HttpHeaders.CACHE_CONTROL, "s-maxage=1000");
		assertThat(headers.getFirst(HttpHeaders.CACHE_CONTROL), is("max-age=1000, public"));
	}

	@Test
	public void accept() {
		MediaType mediaType1 = new MediaType("text", "html");
		MediaType mediaType2 = new MediaType("text", "plain");
		List<MediaType> mediaTypes = new ArrayList<>(2);
		mediaTypes.add(mediaType1);
		mediaTypes.add(mediaType2);
		headers.setAccept(mediaTypes);
		assertEquals("Invalid Accept header", mediaTypes, headers.getAccept());
		assertEquals("Invalid Accept header", "text/html, text/plain", headers.getFirst("Accept"));
	}

	@Test  // SPR-9655
	public void acceptWithMultipleHeaderValues() {
		headers.add("Accept", "text/html");
		headers.add("Accept", "text/plain");
		List<MediaType> expected = Arrays.asList(new MediaType("text", "html"), new MediaType("text", "plain"));
		assertEquals("Invalid Accept header", expected, headers.getAccept());
	}

	@Test  // SPR-14506
	public void acceptWithMultipleCommaSeparatedHeaderValues() {
		headers.add("Accept", "text/html,text/pdf");
		headers.add("Accept", "text/plain,text/csv");
		List<MediaType> expected = Arrays.asList(new MediaType("text", "html"), new MediaType("text", "pdf"),
				new MediaType("text", "plain"), new MediaType("text", "csv"));
		assertEquals("Invalid Accept header", expected, headers.getAccept());
	}

	@Test
	public void acceptCharsets() {
		Charset charset1 = StandardCharsets.UTF_8;
		Charset charset2 = StandardCharsets.ISO_8859_1;
		List<Charset> charsets = new ArrayList<>(2);
		charsets.add(charset1);
		charsets.add(charset2);
		headers.setAcceptCharset(charsets);
		assertEquals("Invalid Accept header", charsets, headers.getAcceptCharset());
		assertEquals("Invalid Accept header", "utf-8, iso-8859-1", headers.getFirst("Accept-Charset"));
	}

	@Test
	public void acceptCharsetWildcard() {
		headers.set("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		assertEquals("Invalid Accept header", Arrays.asList(StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8),
				headers.getAcceptCharset());
	}

	@Test
	public void allow() {
		EnumSet<HttpMethod> methods = EnumSet.of(HttpMethod.GET, HttpMethod.POST);
		headers.setAllow(methods);
		assertEquals("Invalid Allow header", methods, headers.getAllow());
		assertEquals("Invalid Allow header", "GET,POST", headers.getFirst("Allow"));
	}

	@Test
	public void contentLength() {
		long length = 42L;
		headers.setContentLength(length);
		assertEquals("Invalid Content-Length header", length, headers.getContentLength());
		assertEquals("Invalid Content-Length header", "42", headers.getFirst("Content-Length"));
	}

	@Test
	public void contentType() {
		MediaType contentType = new MediaType("text", "html", StandardCharsets.UTF_8);
		headers.setContentType(contentType);
		assertEquals("Invalid Content-Type header", contentType, headers.getContentType());
		assertEquals("Invalid Content-Type header", "text/html;charset=UTF-8", headers.getFirst("Content-Type"));
	}

	@Test
	public void location() throws URISyntaxException {
		URI location = new URI("https://www.example.com/hotels");
		headers.setLocation(location);
		assertEquals("Invalid Location header", location, headers.getLocation());
		assertEquals("Invalid Location header", "https://www.example.com/hotels", headers.getFirst("Location"));
	}

	@Test
	public void eTag() {
		String eTag = "\"v2.6\"";
		headers.setETag(eTag);
		assertEquals("Invalid ETag header", eTag, headers.getETag());
		assertEquals("Invalid ETag header", "\"v2.6\"", headers.getFirst("ETag"));
	}

	@Test
	public void host() {
		InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 8080);
		headers.setHost(host);
		assertEquals("Invalid Host header", host, headers.getHost());
		assertEquals("Invalid Host header", "localhost:8080", headers.getFirst("Host"));
	}

	@Test
	public void hostNoPort() {
		InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 0);
		headers.setHost(host);
		assertEquals("Invalid Host header", host, headers.getHost());
		assertEquals("Invalid Host header", "localhost", headers.getFirst("Host"));
	}

	@Test
	public void ipv6Host() {
		InetSocketAddress host = InetSocketAddress.createUnresolved("[::1]", 0);
		headers.setHost(host);
		assertEquals("Invalid Host header", host, headers.getHost());
		assertEquals("Invalid Host header", "[::1]", headers.getFirst("Host"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void illegalETag() {
		String eTag = "v2.6";
		headers.setETag(eTag);
		assertEquals("Invalid ETag header", eTag, headers.getETag());
		assertEquals("Invalid ETag header", "\"v2.6\"", headers.getFirst("ETag"));
	}

	@Test
	public void ifMatch() {
		String ifMatch = "\"v2.6\"";
		headers.setIfMatch(ifMatch);
		assertEquals("Invalid If-Match header", ifMatch, headers.getIfMatch().get(0));
		assertEquals("Invalid If-Match header", "\"v2.6\"", headers.getFirst("If-Match"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ifMatchIllegalHeader() {
		headers.setIfMatch("Illegal");
		headers.getIfMatch();
	}

	@Test
	public void ifMatchMultipleHeaders() {
		headers.add(HttpHeaders.IF_MATCH, "\"v2,0\"");
		headers.add(HttpHeaders.IF_MATCH, "W/\"v2,1\", \"v2,2\"");
		assertEquals("Invalid If-Match header", "\"v2,0\"", headers.get(HttpHeaders.IF_MATCH).get(0));
		assertEquals("Invalid If-Match header", "W/\"v2,1\", \"v2,2\"", headers.get(HttpHeaders.IF_MATCH).get(1));
		assertThat(headers.getIfMatch(), Matchers.contains("\"v2,0\"", "W/\"v2,1\"", "\"v2,2\""));
	}

	@Test
	public void ifNoneMatch() {
		String ifNoneMatch = "\"v2.6\"";
		headers.setIfNoneMatch(ifNoneMatch);
		assertEquals("Invalid If-None-Match header", ifNoneMatch, headers.getIfNoneMatch().get(0));
		assertEquals("Invalid If-None-Match header", "\"v2.6\"", headers.getFirst("If-None-Match"));
	}

	@Test
	public void ifNoneMatchWildCard() {
		String ifNoneMatch = "*";
		headers.setIfNoneMatch(ifNoneMatch);
		assertEquals("Invalid If-None-Match header", ifNoneMatch, headers.getIfNoneMatch().get(0));
		assertEquals("Invalid If-None-Match header", "*", headers.getFirst("If-None-Match"));
	}

	@Test
	public void ifNoneMatchList() {
		String ifNoneMatch1 = "\"v2.6\"";
		String ifNoneMatch2 = "\"v2.7\", \"v2.8\"";
		List<String> ifNoneMatchList = new ArrayList<>(2);
		ifNoneMatchList.add(ifNoneMatch1);
		ifNoneMatchList.add(ifNoneMatch2);
		headers.setIfNoneMatch(ifNoneMatchList);
		assertThat(headers.getIfNoneMatch(), Matchers.contains("\"v2.6\"", "\"v2.7\"", "\"v2.8\""));
		assertEquals("Invalid If-None-Match header", "\"v2.6\", \"v2.7\", \"v2.8\"", headers.getFirst("If-None-Match"));
	}

	@Test
	public void date() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		headers.setDate(date);
		assertEquals("Invalid Date header", date, headers.getDate());
		assertEquals("Invalid Date header", "Thu, 18 Dec 2008 10:20:00 GMT", headers.getFirst("date"));

		// RFC 850
		headers.set("Date", "Thu, 18 Dec 2008 10:20:00 GMT");
		assertEquals("Invalid Date header", date, headers.getDate());
	}

	@Test(expected = IllegalArgumentException.class)
	public void dateInvalid() {
		headers.set("Date", "Foo Bar Baz");
		headers.getDate();
	}

	@Test
	public void dateOtherLocale() {
		Locale defaultLocale = Locale.getDefault();
		try {
			Locale.setDefault(new Locale("nl", "nl"));
			Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
			calendar.setTimeZone(TimeZone.getTimeZone("CET"));
			long date = calendar.getTimeInMillis();
			headers.setDate(date);
			assertEquals("Invalid Date header", "Thu, 18 Dec 2008 10:20:00 GMT", headers.getFirst("date"));
			assertEquals("Invalid Date header", date, headers.getDate());
		}
		finally {
			Locale.setDefault(defaultLocale);
		}
	}

	@Test
	public void lastModified() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		headers.setLastModified(date);
		assertEquals("Invalid Last-Modified header", date, headers.getLastModified());
		assertEquals("Invalid Last-Modified header", "Thu, 18 Dec 2008 10:20:00 GMT",
				headers.getFirst("last-modified"));
	}

	@Test
	public void expiresLong() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		headers.setExpires(date);
		assertEquals("Invalid Expires header", date, headers.getExpires());
		assertEquals("Invalid Expires header", "Thu, 18 Dec 2008 10:20:00 GMT", headers.getFirst("expires"));
	}

	@Test
	public void expiresZonedDateTime() {
		ZonedDateTime zonedDateTime = ZonedDateTime.of(2008, 12, 18, 10, 20, 0, 0, ZoneId.of("GMT"));
		headers.setExpires(zonedDateTime);
		assertEquals("Invalid Expires header", zonedDateTime.toInstant().toEpochMilli(), headers.getExpires());
		assertEquals("Invalid Expires header", "Thu, 18 Dec 2008 10:20:00 GMT", headers.getFirst("expires"));
	}

	@Test  // SPR-10648 (example is from INT-3063)
	public void expiresInvalidDate() {
		headers.set("Expires", "-1");
		assertEquals(-1, headers.getExpires());
	}

	@Test
	public void ifModifiedSince() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		headers.setIfModifiedSince(date);
		assertEquals("Invalid If-Modified-Since header", date, headers.getIfModifiedSince());
		assertEquals("Invalid If-Modified-Since header", "Thu, 18 Dec 2008 10:20:00 GMT",
				headers.getFirst("if-modified-since"));
	}

	@Test  // SPR-14144
	public void invalidIfModifiedSinceHeader() {
		headers.set(HttpHeaders.IF_MODIFIED_SINCE, "0");
		assertEquals(-1, headers.getIfModifiedSince());

		headers.set(HttpHeaders.IF_MODIFIED_SINCE, "-1");
		assertEquals(-1, headers.getIfModifiedSince());

		headers.set(HttpHeaders.IF_MODIFIED_SINCE, "XXX");
		assertEquals(-1, headers.getIfModifiedSince());
	}

	@Test
	public void pragma() {
		String pragma = "no-cache";
		headers.setPragma(pragma);
		assertEquals("Invalid Pragma header", pragma, headers.getPragma());
		assertEquals("Invalid Pragma header", "no-cache", headers.getFirst("pragma"));
	}

	@Test
	public void cacheControl() {
		headers.setCacheControl("no-cache");
		assertEquals("Invalid Cache-Control header", "no-cache", headers.getCacheControl());
		assertEquals("Invalid Cache-Control header", "no-cache", headers.getFirst("cache-control"));
	}

	@Test
	public void cacheControlBuilder() {
		headers.setCacheControl(CacheControl.noCache());
		assertEquals("Invalid Cache-Control header", "no-cache", headers.getCacheControl());
		assertEquals("Invalid Cache-Control header", "no-cache", headers.getFirst("cache-control"));
	}

	@Test
	public void cacheControlAllValues() {
		headers.add(HttpHeaders.CACHE_CONTROL, "max-age=1000, public");
		headers.add(HttpHeaders.CACHE_CONTROL, "s-maxage=1000");
		assertThat(headers.getCacheControl(), is("max-age=1000, public, s-maxage=1000"));
	}

	@Test
	public void contentDisposition() {
		ContentDisposition disposition = headers.getContentDisposition();
		assertNotNull(disposition);
		assertEquals("Invalid Content-Disposition header", ContentDisposition.empty(), headers.getContentDisposition());

		disposition = ContentDisposition.builder("attachment").name("foo").filename("foo.txt").size(123L).build();
		headers.setContentDisposition(disposition);
		assertEquals("Invalid Content-Disposition header", disposition, headers.getContentDisposition());
	}

	@Test  // SPR-11917
	public void getAllowEmptySet() {
		headers.setAllow(Collections.<HttpMethod> emptySet());
		assertThat(headers.getAllow(), Matchers.emptyCollectionOf(HttpMethod.class));
	}

	@Test
	public void accessControlAllowCredentials() {
		assertFalse(headers.getAccessControlAllowCredentials());
		headers.setAccessControlAllowCredentials(false);
		assertFalse(headers.getAccessControlAllowCredentials());
		headers.setAccessControlAllowCredentials(true);
		assertTrue(headers.getAccessControlAllowCredentials());
	}

	@Test
	public void accessControlAllowHeaders() {
		List<String> allowedHeaders = headers.getAccessControlAllowHeaders();
		assertThat(allowedHeaders, Matchers.emptyCollectionOf(String.class));
		headers.setAccessControlAllowHeaders(Arrays.asList("header1", "header2"));
		allowedHeaders = headers.getAccessControlAllowHeaders();
		assertEquals(allowedHeaders, Arrays.asList("header1", "header2"));
	}

	@Test
	public void accessControlAllowHeadersMultipleValues() {
		List<String> allowedHeaders = headers.getAccessControlAllowHeaders();
		assertThat(allowedHeaders, Matchers.emptyCollectionOf(String.class));
		headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "header1, header2");
		headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "header3");
		allowedHeaders = headers.getAccessControlAllowHeaders();
		assertEquals(Arrays.asList("header1", "header2", "header3"), allowedHeaders);
	}

	@Test
	public void accessControlAllowMethods() {
		List<HttpMethod> allowedMethods = headers.getAccessControlAllowMethods();
		assertThat(allowedMethods, Matchers.emptyCollectionOf(HttpMethod.class));
		headers.setAccessControlAllowMethods(Arrays.asList(HttpMethod.GET, HttpMethod.POST));
		allowedMethods = headers.getAccessControlAllowMethods();
		assertEquals(allowedMethods, Arrays.asList(HttpMethod.GET, HttpMethod.POST));
	}

	@Test
	public void accessControlAllowOrigin() {
		assertNull(headers.getAccessControlAllowOrigin());
		headers.setAccessControlAllowOrigin("*");
		assertEquals("*", headers.getAccessControlAllowOrigin());
	}

	@Test
	public void accessControlExposeHeaders() {
		List<String> exposedHeaders = headers.getAccessControlExposeHeaders();
		assertThat(exposedHeaders, Matchers.emptyCollectionOf(String.class));
		headers.setAccessControlExposeHeaders(Arrays.asList("header1", "header2"));
		exposedHeaders = headers.getAccessControlExposeHeaders();
		assertEquals(exposedHeaders, Arrays.asList("header1", "header2"));
	}

	@Test
	public void accessControlMaxAge() {
		assertEquals(-1, headers.getAccessControlMaxAge());
		headers.setAccessControlMaxAge(3600);
		assertEquals(3600, headers.getAccessControlMaxAge());
	}

	@Test
	public void accessControlRequestHeaders() {
		List<String> requestHeaders = headers.getAccessControlRequestHeaders();
		assertThat(requestHeaders, Matchers.emptyCollectionOf(String.class));
		headers.setAccessControlRequestHeaders(Arrays.asList("header1", "header2"));
		requestHeaders = headers.getAccessControlRequestHeaders();
		assertEquals(requestHeaders, Arrays.asList("header1", "header2"));
	}

	@Test
	public void accessControlRequestMethod() {
		assertNull(headers.getAccessControlRequestMethod());
		headers.setAccessControlRequestMethod(HttpMethod.POST);
		assertEquals(HttpMethod.POST, headers.getAccessControlRequestMethod());
	}

	@Test
	public void acceptLanguage() {
		String headerValue = "fr-ch, fr;q=0.9, en-*;q=0.8, de;q=0.7, *;q=0.5";
		headers.setAcceptLanguage(Locale.LanguageRange.parse(headerValue));
		assertEquals(headerValue, headers.getFirst(HttpHeaders.ACCEPT_LANGUAGE));

		List<Locale.LanguageRange> expectedRanges = Arrays.asList(
				new Locale.LanguageRange("fr-ch"),
				new Locale.LanguageRange("fr", 0.9),
				new Locale.LanguageRange("en-*", 0.8),
				new Locale.LanguageRange("de", 0.7),
				new Locale.LanguageRange("*", 0.5)
		);
		assertEquals(expectedRanges, headers.getAcceptLanguage());
		assertEquals(Locale.forLanguageTag("fr-ch"), headers.getAcceptLanguageAsLocales().get(0));

		headers.setAcceptLanguageAsLocales(Collections.singletonList(Locale.FRANCE));
		assertEquals(Locale.FRANCE, headers.getAcceptLanguageAsLocales().get(0));
	}

	@Test // SPR-15603
	public void acceptLanguageWithEmptyValue() throws Exception {
		this.headers.set(HttpHeaders.ACCEPT_LANGUAGE, "");
		assertEquals(Collections.emptyList(), this.headers.getAcceptLanguageAsLocales());
	}

	@Test
	public void contentLanguage() {
		headers.setContentLanguage(Locale.FRANCE);
		assertEquals(Locale.FRANCE, headers.getContentLanguage());
		assertEquals("fr-FR", headers.getFirst(HttpHeaders.CONTENT_LANGUAGE));
	}

	@Test
	public void contentLanguageSerialized() {
		headers.set(HttpHeaders.CONTENT_LANGUAGE,  "de, en_CA");
		assertEquals("Expected one (first) locale", Locale.GERMAN, headers.getContentLanguage());
	}

	@Test
	public void firstDate() {
		headers.setDate(HttpHeaders.DATE, 1496370120000L);
		assertThat(headers.getFirstDate(HttpHeaders.DATE), is(1496370120000L));

		headers.clear();

		headers.add(HttpHeaders.DATE, "Fri, 02 Jun 2017 02:22:00 GMT");
		headers.add(HttpHeaders.DATE, "Sat, 18 Dec 2010 10:20:00 GMT");
		assertThat(headers.getFirstDate(HttpHeaders.DATE), is(1496370120000L));
	}

	@Test
	public void firstZonedDateTime() {
		ZonedDateTime date = ZonedDateTime.of(2017, 6, 2, 2, 22, 0, 0, ZoneId.of("GMT"));
		headers.setZonedDateTime(HttpHeaders.DATE, date);
		assertThat(headers.getFirst(HttpHeaders.DATE), is("Fri, 02 Jun 2017 02:22:00 GMT"));
		assertTrue(headers.getFirstZonedDateTime(HttpHeaders.DATE).isEqual(date));

		headers.clear();
		ZonedDateTime otherDate = ZonedDateTime.of(2010, 12, 18, 10, 20, 0, 0, ZoneId.of("GMT"));
		headers.add(HttpHeaders.DATE, "Fri, 02 Jun 2017 02:22:00 GMT");
		headers.add(HttpHeaders.DATE, "Sat, 18 Dec 2010 10:20:00 GMT");
		assertTrue(headers.getFirstZonedDateTime(HttpHeaders.DATE).isEqual(date));

		// obsolete RFC 850 format
		headers.clear();
		headers.set(HttpHeaders.DATE, "Friday, 02-Jun-17 02:22:00 GMT");
		assertTrue(headers.getFirstZonedDateTime(HttpHeaders.DATE).isEqual(date));

		// ANSI C's asctime() format
		headers.clear();
		headers.set(HttpHeaders.DATE, "Fri Jun 02 02:22:00 2017");
		assertTrue(headers.getFirstZonedDateTime(HttpHeaders.DATE).isEqual(date));
	}

}
