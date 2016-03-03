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
package org.springframework.web.util;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link DefaultUriTemplateHandler}.
 * @author Rossen Stoyanchev
 */
public class DefaultUriTemplateHandlerTests {

	private final DefaultUriTemplateHandler handler = new DefaultUriTemplateHandler();


	@Test
	public void baseUrl() throws Exception {
		this.handler.setBaseUrl("http://localhost:8080");
		URI actual = this.handler.expand("/myapiresource");
		URI expected = new URI("http://localhost:8080/myapiresource");
		assertEquals(expected, actual);
	}

	@Test
	public void baseUrlWithPartialPath() throws Exception {
		this.handler.setBaseUrl("http://localhost:8080/context");
		URI actual = this.handler.expand("/myapiresource");
		URI expected = new URI("http://localhost:8080/context/myapiresource");
		assertEquals(expected, actual);
	}

	@Test
	public void expandWithFullPath() throws Exception {
		Map<String, String> vars = new HashMap<>(2);
		vars.put("hotel", "1");
		vars.put("publicpath", "pics/logo.png");
		String template = "http://example.com/hotels/{hotel}/pic/{publicpath}";
		URI actual = this.handler.expand(template, vars);
		URI expected = new URI("http://example.com/hotels/1/pic/pics/logo.png");
		assertEquals(expected, actual);
	}

	@Test
	public void expandWithFullPathAndParsePathEnabled() throws Exception {
		this.handler.setParsePath(true);
		Map<String, String> vars = new HashMap<>(2);
		vars.put("hotel", "1");
		vars.put("publicpath", "pics/logo.png");
		vars.put("scale", "150x150");
		String template = "http://example.com/hotels/{hotel}/pic/{publicpath}/size/{scale}";
		URI actual = this.handler.expand(template, vars);
		URI expected = new URI("http://example.com/hotels/1/pic/pics%2Flogo.png/size/150x150");
		assertEquals(expected, actual);
	}

}
