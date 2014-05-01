/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Mattias Severson
 */
public class HttpHeadersBuilderTests {

	private HttpHeadersBuilder builder;


	@Before
	public void setUp() {
		builder = new HttpHeadersBuilder();
	}

	@Test
	public void accept() {
		MediaType mediaType = new MediaType("text", "plain");
		List<MediaType> mediaTypes = Arrays.asList(mediaType);
		builder.setAccept(mediaTypes);
		HttpHeaders httpHeaders = builder.build();
		assertEquals("Invalid Accept header", mediaTypes, httpHeaders.getAccept());
	}

	@Test
	public void acceptCharsets() {
		Charset charset = Charset.forName("UTF-8");
		List<Charset> charsets = Arrays.asList(charset);
		builder.setAcceptCharset(charsets);
		HttpHeaders httpHeaders = builder.build();
		assertEquals("Invalid Accept Charset header", charsets, httpHeaders.getAcceptCharset());
	}

	@Test
	public void allow() {
		EnumSet<HttpMethod> methods = EnumSet.of(HttpMethod.GET, HttpMethod.POST);
		builder.setAllow(methods);
		HttpHeaders httpHeaders = builder.build();
		assertEquals("Invalid Allow header", methods, httpHeaders.getAllow());
	}

	@Test
	public void contentLength() {
		long length = 42L;
		builder.setContentLength(length);
		HttpHeaders httpHeaders = builder.build();
		assertEquals("Invalid Content-Length header", length, httpHeaders.getContentLength());
	}

	@Test
	public void contentType() {
		MediaType contentType = new MediaType("text", "html", Charset.forName("UTF-8"));
		builder.setContentType(contentType);
		HttpHeaders httpHeaders = builder.build();
		assertEquals("Invalid Content-Type header", contentType, httpHeaders.getContentType());
	}

	@Test
	public void location() throws URISyntaxException {
		URI location = new URI("http://www.example.com/hotels");
		builder.setLocation(location);
		HttpHeaders httpHeaders = builder.build();
		assertEquals("Invalid Location header", location, httpHeaders.getLocation());
	}

	@Test
	public void eTag() {
		String eTag = "\"v2.6\"";
		builder.setETag(eTag);
		HttpHeaders httpHeaders = builder.build();
		assertEquals("Invalid ETag header", eTag, httpHeaders.getETag());
	}

	@Test
	public void ifNoneMatch() {
		String ifNoneMatch = "\"v2.6\"";
		builder.setIfNoneMatch(ifNoneMatch);
		HttpHeaders httpHeaders = builder.build();
		assertEquals("Invalid If-None-Match header", ifNoneMatch, httpHeaders.getIfNoneMatch().get(0));
	}

	@Test
	public void ifNoneMatchList() {
		String ifNoneMatch1 = "\"v2.6\"";
		String ifNoneMatch2 = "\"v2.7\"";
		List<String> ifNoneMatchList = new ArrayList<String>(2);
		ifNoneMatchList.add(ifNoneMatch1);
		ifNoneMatchList.add(ifNoneMatch2);
		builder.setIfNoneMatch(ifNoneMatchList);
		HttpHeaders httpHeaders = builder.build();
		assertEquals("Invalid If-None-Match header", ifNoneMatchList, httpHeaders.getIfNoneMatch());
	}

	@Test
	public void date() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		builder.setDate(date);
		HttpHeaders httpHeaders = builder.build();
		assertEquals("Invalid Date header", date, httpHeaders.getDate());

		builder.setDate("Date", date);
		httpHeaders = builder.build();
		assertEquals("Invalid Date header", date, httpHeaders.getDate());
	}

	@Test
	public void lastModified() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		builder.setLastModified(date);
		HttpHeaders httpHeaders = builder.build();
		assertEquals("Invalid Last-Modified header", date, httpHeaders.getLastModified());
	}

	@Test
	public void expires() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		builder.setExpires(date);
		HttpHeaders httpHeaders = builder.build();
		assertEquals("Invalid Expires header", date, httpHeaders.getExpires());
	}

	@Test
	public void ifModifiedSince() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		builder.setIfModifiedSince(date);
		HttpHeaders httpHeaders = builder.build();
		assertEquals("Invalid If-Modified-Since header", date, httpHeaders.getIfModifiedSince());
	}

	@Test
	public void pragma() {
		String pragma = "no-cache";
		builder.setPragma(pragma);
		HttpHeaders httpHeaders = builder.build();
		assertEquals("Invalid Pragma header", pragma, httpHeaders.getPragma());
	}

	@Test
	public void cacheControl() {
		String cacheControl = "no-cache";
		builder.setCacheControl(cacheControl);
		HttpHeaders httpHeaders = builder.build();
		assertEquals("Invalid Cache-Control header", cacheControl, httpHeaders.getCacheControl());
	}

	@Test
	public void contentDisposition() {
		builder.setContentDispositionFormData("name", null);
		HttpHeaders httpHeaders = builder.build();
		assertEquals("Invalid Content-Disposition header", "form-data; name=\"name\"",
				httpHeaders.getFirst("Content-Disposition"));

		builder.setContentDispositionFormData("name", "filename");
		assertEquals("Invalid Content-Disposition header", "form-data; name=\"name\"; filename=\"filename\"",
				httpHeaders.getFirst("Content-Disposition"));
	}

}
