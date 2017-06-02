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
package org.springframework.web.server.support;

import java.util.Arrays;

import org.junit.Test;

import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link HttpRequestPathHelper}.
 * @author Rossen Stoyanchev
 */
public class HttpRequestPathHelperTests {


	@Test
	public void parseMatrixVariables() {

		HttpRequestPathHelper pathHelper = new HttpRequestPathHelper();
		ServerWebExchange exchange = MockServerHttpRequest.get("").toExchange();
		MultiValueMap<String, String> variables;

		variables = pathHelper.parseMatrixVariables(exchange, null);
		assertEquals(0, variables.size());

		variables = pathHelper.parseMatrixVariables(exchange, "year");
		assertEquals(1, variables.size());
		assertEquals("", variables.getFirst("year"));

		variables = pathHelper.parseMatrixVariables(exchange, "year=2012");
		assertEquals(1, variables.size());
		assertEquals("2012", variables.getFirst("year"));

		variables = pathHelper.parseMatrixVariables(exchange, "year=2012;colors=red,blue,green");
		assertEquals(2, variables.size());
		assertEquals(Arrays.asList("red", "blue", "green"), variables.get("colors"));
		assertEquals("2012", variables.getFirst("year"));

		variables = pathHelper.parseMatrixVariables(exchange, ";year=2012;colors=red,blue,green;");
		assertEquals(2, variables.size());
		assertEquals(Arrays.asList("red", "blue", "green"), variables.get("colors"));
		assertEquals("2012", variables.getFirst("year"));

		variables = pathHelper.parseMatrixVariables(exchange, "colors=red;colors=blue;colors=green");
		assertEquals(1, variables.size());
		assertEquals(Arrays.asList("red", "blue", "green"), variables.get("colors"));
	}

	@Test
	public void parseMatrixVariablesAndDecode() {

		HttpRequestPathHelper pathHelper = new HttpRequestPathHelper();
		pathHelper.setUrlDecode(false);

		ServerWebExchange exchange = MockServerHttpRequest.get("").toExchange();
		MultiValueMap<String, String> variables;

		variables = pathHelper.parseMatrixVariables(exchange, "mvar=a%2fb");
		assertEquals(1, variables.size());
		assertEquals("a/b", variables.getFirst("mvar"));
	}

}
