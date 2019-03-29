/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.util.Arrays;
import java.util.Locale;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.undertow.util.HeaderMap;
import org.apache.tomcat.util.http.MimeHeaders;
import org.eclipse.jetty.http.HttpFields;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@code HeadersAdapters} {@code MultiValueMap} implementations.
 *
 * @author Brian Clozel
 */
@RunWith(Parameterized.class)
public class HeadersAdaptersTests {

	@Parameterized.Parameter(0)
	public MultiValueMap<String, String> headers;

	@Parameterized.Parameters(name = "headers [{0}]")
	public static Object[][] arguments() {
		return new Object[][] {
				{CollectionUtils.toMultiValueMap(
						new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH))},
				{new NettyHeadersAdapter(new DefaultHttpHeaders())},
				{new TomcatHeadersAdapter(new MimeHeaders())},
				{new UndertowHeadersAdapter(new HeaderMap())},
				{new JettyHeadersAdapter(new HttpFields())}
		};
	}

	@After
	public void tearDown() {
		this.headers.clear();
	}

	@Test
	public void getWithUnknownHeaderShouldReturnNull() {
		assertNull(this.headers.get("Unknown"));
	}

	@Test
	public void getFirstWithUnknownHeaderShouldReturnNull() {
		assertNull(this.headers.getFirst("Unknown"));
	}

	@Test
	public void sizeWithMultipleValuesForHeaderShouldCountHeaders() {
		this.headers.add("TestHeader", "first");
		this.headers.add("TestHeader", "second");
		assertEquals(1, this.headers.size());
	}

	@Test
	public void keySetShouldNotDuplicateHeaderNames() {
		this.headers.add("TestHeader", "first");
		this.headers.add("OtherHeader", "test");
		this.headers.add("TestHeader", "second");
		assertEquals(2, this.headers.keySet().size());
	}

	@Test
	public void containsKeyShouldBeCaseInsensitive() {
		this.headers.add("TestHeader", "first");
		assertTrue(this.headers.containsKey("testheader"));
	}

	@Test
	public void addShouldKeepOrdering() {
		this.headers.add("TestHeader", "first");
		this.headers.add("TestHeader", "second");
		assertEquals("first", this.headers.getFirst("TestHeader"));
		assertEquals("first", this.headers.get("TestHeader").get(0));
	}

	@Test
	public void putShouldOverrideExisting() {
		this.headers.add("TestHeader", "first");
		this.headers.put("TestHeader", Arrays.asList("override"));
		assertEquals("override", this.headers.getFirst("TestHeader"));
		assertEquals(1, this.headers.get("TestHeader").size());
	}

}