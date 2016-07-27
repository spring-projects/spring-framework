/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.function;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Collections;

import org.junit.Test;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.junit.Assert.assertEquals;

/**
 * @author Arjen Poutsma
 */
public class ResponseTests {

	@Test
	public void from() throws Exception {
		Response<Void> other = Response.ok().header("foo", "bar").build();
		Response<Void> result = Response.from(other).build();
		assertEquals(HttpStatus.OK, result.statusCode());
		assertEquals("bar", result.headers().getFirst("foo"));
	}

	@Test
	public void status() throws Exception {
		Response<Void> result = Response.status(HttpStatus.CREATED).build();
		assertEquals(HttpStatus.CREATED, result.statusCode());
	}

	@Test
	public void statusInt() throws Exception {
		Response<Void> result = Response.status(201).build();
		assertEquals(HttpStatus.CREATED, result.statusCode());
	}

	@Test
	public void ok() throws Exception {
		Response<Void> result = Response.ok().build();
		assertEquals(HttpStatus.OK, result.statusCode());
	}

	@Test
	public void created() throws Exception {
		URI location = URI.create("http://example.com");
		Response<Void> result = Response.created(location).build();
		assertEquals(HttpStatus.CREATED, result.statusCode());
		assertEquals(location, result.headers().getLocation());
	}

	@Test
	public void accepted() throws Exception {
		Response<Void> result = Response.accepted().build();
		assertEquals(HttpStatus.ACCEPTED, result.statusCode());
	}

	@Test
	public void noContent() throws Exception {
		Response<Void> result = Response.noContent().build();
		assertEquals(HttpStatus.NO_CONTENT, result.statusCode());
	}

	@Test
	public void badRequest() throws Exception {
		Response<Void> result = Response.badRequest().build();
		assertEquals(HttpStatus.BAD_REQUEST, result.statusCode());
	}

	@Test
	public void notFound() throws Exception {
		Response<Void> result = Response.notFound().build();
		assertEquals(HttpStatus.NOT_FOUND, result.statusCode());
	}

	@Test
	public void unprocessableEntity() throws Exception {
		Response<Void> result = Response.unprocessableEntity().build();
		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.statusCode());
	}

	@Test
	public void allow() throws Exception {
		Response<Void> result = Response.ok().allow(HttpMethod.GET).build();
		assertEquals(Collections.singleton(HttpMethod.GET), result.headers().getAllow());
	}

	@Test
	public void contentLength() throws Exception {
		Response<Void> result = Response.ok().contentLength(42).build();
		assertEquals(42, result.headers().getContentLength());
	}

	@Test
	public void contentType() throws Exception {
		Response<Void> result = Response.ok().contentType(MediaType.APPLICATION_JSON).build();
		assertEquals(MediaType.APPLICATION_JSON, result.headers().getContentType());
	}

	@Test
	public void eTag() throws Exception {
		Response<Void> result = Response.ok().eTag("foo").build();
		assertEquals("\"foo\"", result.headers().getETag());
	}
	@Test
	public void lastModified() throws Exception {
		ZonedDateTime now = ZonedDateTime.now();
		Response<Void> result = Response.ok().lastModified(now).build();
		assertEquals(now.toInstant().toEpochMilli()/1000, result.headers().getLastModified()/1000);
	}

	@Test
	public void cacheControlTag() throws Exception {
		Response<Void> result = Response.ok().cacheControl(CacheControl.noCache()).build();
		assertEquals("no-cache", result.headers().getCacheControl());
	}

	@Test
	public void varyBy() throws Exception {
		Response<Void> result = Response.ok().varyBy("foo").build();
		assertEquals(Collections.singletonList("foo"), result.headers().getVary());
	}

	@Test
	public void writeTo() throws Exception {
	}

}