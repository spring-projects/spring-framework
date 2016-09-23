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

package org.springframework.web.client.reactive;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Base64.Encoder;

import org.junit.Test;
import org.springframework.http.HttpHeaders;


import static org.junit.Assert.*;
import static org.springframework.web.client.reactive.ClientWebRequestPostProcessors.*;
import static org.springframework.web.client.reactive.ClientWebRequestBuilders.*;

/**
 *
 * @author Rob Winch
 * @since 5.0
 */
public class ClientWebRequestPostProcessorsTests {

	@Test
	public void httpBasicWhenUsernamePasswordThenHeaderSet() {
		ClientWebRequest request = get("/").apply(httpBasic("user", "password")).build();
		assertEquals(request.getHttpHeaders().getFirst(HttpHeaders.AUTHORIZATION), basic("user:password"));
	}

	@Test
	public void httpBasicWhenUsernameEmptyThenHeaderSet() {
		ClientWebRequest request = get("/").apply(httpBasic("", "password")).build();
		assertEquals(request.getHttpHeaders().getFirst(HttpHeaders.AUTHORIZATION), basic(":password"));
	}

	@Test
	public void httpBasicWhenPasswordEmptyThenHeaderSet() {
		ClientWebRequest request = get("/").apply(httpBasic("user", "")).build();
		assertEquals(request.getHttpHeaders().getFirst(HttpHeaders.AUTHORIZATION), basic("user:"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void httpBasicWhenUsernameNullThenIllegalArgumentException() {
		httpBasic(null, "password");
	}

	@Test(expected = IllegalArgumentException.class)
	public void httpBasicWhenPasswordNullThenIllegalArgumentException() {
		httpBasic("username", null);
	}

	private static String basic(String string) {
		Encoder encoder = Base64.getEncoder();
		byte[] bytes = string.getBytes(Charset.defaultCharset());
		return "Basic " + encoder.encodeToString(bytes);
	}
}
