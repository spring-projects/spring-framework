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

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(this.headers.get("Unknown")).isNull();
	}

	@Test
	public void getFirstWithUnknownHeaderShouldReturnNull() {
		assertThat(this.headers.getFirst("Unknown")).isNull();
	}

	@Test
	public void sizeWithMultipleValuesForHeaderShouldCountHeaders() {
		this.headers.add("TestHeader", "first");
		this.headers.add("TestHeader", "second");
		assertThat(this.headers.size()).isEqualTo(1);
	}

	@Test
	public void keySetShouldNotDuplicateHeaderNames() {
		this.headers.add("TestHeader", "first");
		this.headers.add("OtherHeader", "test");
		this.headers.add("TestHeader", "second");
		assertThat(this.headers.keySet().size()).isEqualTo(2);
	}

	@Test
	public void containsKeyShouldBeCaseInsensitive() {
		this.headers.add("TestHeader", "first");
		assertThat(this.headers.containsKey("testheader")).isTrue();
	}

	@Test
	public void addShouldKeepOrdering() {
		this.headers.add("TestHeader", "first");
		this.headers.add("TestHeader", "second");
		assertThat(this.headers.getFirst("TestHeader")).isEqualTo("first");
		assertThat(this.headers.get("TestHeader").get(0)).isEqualTo("first");
	}

	@Test
	public void putShouldOverrideExisting() {
		this.headers.add("TestHeader", "first");
		this.headers.put("TestHeader", Arrays.asList("override"));
		assertThat(this.headers.getFirst("TestHeader")).isEqualTo("override");
		assertThat(this.headers.get("TestHeader").size()).isEqualTo(1);
	}

}
