/*
 * Copyright 2002-2017 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link DefaultUriTemplateHandler}.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("deprecation")
public class DefaultUriTemplateHandlerTests {

	private final DefaultUriTemplateHandler handler = new DefaultUriTemplateHandler();


	@Test
	public void baseUrlWithoutPath() throws Exception {
		this.handler.setBaseUrl("http://localhost:8080");
		URI actual = this.handler.expand("/myapiresource");

		assertEquals("http://localhost:8080/myapiresource", actual.toString());
	}

	@Test
	public void baseUrlWithPath() throws Exception {
		this.handler.setBaseUrl("http://localhost:8080/context");
		URI actual = this.handler.expand("/myapiresource");

		assertEquals("http://localhost:8080/context/myapiresource", actual.toString());
	}

	@Test	// SPR-14147
	public void defaultUriVariables() throws Exception {
		Map<String, String> defaultVars = new HashMap<>(2);
		defaultVars.put("host", "api.example.com");
		defaultVars.put("port", "443");
		this.handler.setDefaultUriVariables(defaultVars);

		Map<String, Object> vars = new HashMap<>(1);
		vars.put("id", 123L);

		String template = "https://{host}:{port}/v42/customers/{id}";
		URI actual = this.handler.expand(template, vars);

		assertEquals("https://api.example.com:443/v42/customers/123", actual.toString());
	}

	@Test
	public void parsePathIsOff() throws Exception {
		this.handler.setParsePath(false);
		Map<String, String> vars = new HashMap<>(2);
		vars.put("hotel", "1");
		vars.put("publicpath", "pics/logo.png");
		String template = "http://example.com/hotels/{hotel}/pic/{publicpath}";
		URI actual = this.handler.expand(template, vars);

		assertEquals("http://example.com/hotels/1/pic/pics/logo.png", actual.toString());
	}

	@Test
	public void parsePathIsOn() throws Exception {
		this.handler.setParsePath(true);
		Map<String, String> vars = new HashMap<>(2);
		vars.put("hotel", "1");
		vars.put("publicpath", "pics/logo.png");
		vars.put("scale", "150x150");
		String template = "http://example.com/hotels/{hotel}/pic/{publicpath}/size/{scale}";
		URI actual = this.handler.expand(template, vars);

		assertEquals("http://example.com/hotels/1/pic/pics%2Flogo.png/size/150x150", actual.toString());
	}

	@Test
	public void strictEncodingIsOffWithMap() throws Exception {
		this.handler.setStrictEncoding(false);
		Map<String, String> vars = new HashMap<>(2);
		vars.put("userId", "john;doe");
		String template = "http://www.example.com/user/{userId}/dashboard";
		URI actual = this.handler.expand(template, vars);

		assertEquals("http://www.example.com/user/john;doe/dashboard", actual.toString());
	}

	@Test
	public void strictEncodingOffWithArray() throws Exception {
		this.handler.setStrictEncoding(false);
		String template = "http://www.example.com/user/{userId}/dashboard";
		URI actual = this.handler.expand(template, "john;doe");

		assertEquals("http://www.example.com/user/john;doe/dashboard", actual.toString());
	}

	@Test
	public void strictEncodingOnWithMap() throws Exception {
		this.handler.setStrictEncoding(true);
		Map<String, String> vars = new HashMap<>(2);
		vars.put("userId", "john;doe");
		String template = "http://www.example.com/user/{userId}/dashboard";
		URI actual = this.handler.expand(template, vars);

		assertEquals("http://www.example.com/user/john%3Bdoe/dashboard", actual.toString());
	}

	@Test
	public void strictEncodingOnWithArray() throws Exception {
		this.handler.setStrictEncoding(true);
		String template = "http://www.example.com/user/{userId}/dashboard";
		URI actual = this.handler.expand(template, "john;doe");

		assertEquals("http://www.example.com/user/john%3Bdoe/dashboard", actual.toString());
	}

	@Test	// SPR-14147
	public void strictEncodingAndDefaultUriVariables() throws Exception {
		Map<String, String> defaultVars = new HashMap<>(1);
		defaultVars.put("host", "www.example.com");
		this.handler.setDefaultUriVariables(defaultVars);
		this.handler.setStrictEncoding(true);

		Map<String, Object> vars = new HashMap<>(1);
		vars.put("userId", "john;doe");

		String template = "http://{host}/user/{userId}/dashboard";
		URI actual = this.handler.expand(template, vars);

		assertEquals("http://www.example.com/user/john%3Bdoe/dashboard", actual.toString());
	}

}
