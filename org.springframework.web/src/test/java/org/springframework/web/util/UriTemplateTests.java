/*
 * Copyright 2002-2009 the original author or authors.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Arjen Poutsma
 */
public class UriTemplateTests {

	private UriTemplate template;

	@Before
	public void create() {
		template = new UriTemplate("http://example.com/hotels/{hotel}/bookings/{booking}");
	}

	@Test
	public void getVariableNames() throws Exception {
		List<String> variableNames = template.getVariableNames();
		assertEquals("Invalid variable names", Arrays.asList("hotel", "booking"), variableNames);
	}

	@Test
	public void expandVarArgs() throws Exception {
		URI result = template.expand("1", "42");
		assertEquals("Invalid expanded template", new URI("http://example.com/hotels/1/bookings/42"), result);
	}

	@Test(expected = IllegalArgumentException.class)
	public void expandVarArgsInvalidAmountVariables() throws Exception {
		template.expand("1", "42", "100");
	}

	@Test
	public void expandMapDuplicateVariables() throws Exception {
		template = new UriTemplate("/order/{c}/{c}/{c}");
		assertEquals("Invalid variable names", Arrays.asList("c", "c", "c"), template.getVariableNames());
		URI result = template.expand(Collections.singletonMap("c", "cheeseburger"));
		assertEquals("Invalid expanded template", new URI("/order/cheeseburger/cheeseburger/cheeseburger"), result);
	}

	@Test
	public void expandMap() throws Exception {
		Map<String, String> uriVariables = new HashMap<String, String>(2);
		uriVariables.put("booking", "42");
		uriVariables.put("hotel", "1");
		URI result = template.expand(uriVariables);
		assertEquals("Invalid expanded template", new URI("http://example.com/hotels/1/bookings/42"), result);
	}

	@Test(expected = IllegalArgumentException.class)
	public void expandMapInvalidAmountVariables() throws Exception {
		template.expand(Collections.singletonMap("hotel", "1"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void expandMapUnboundVariables() throws Exception {
		Map<String, String> uriVariables = new HashMap<String, String>(2);
		uriVariables.put("booking", "42");
		uriVariables.put("bar", "1");
		template.expand(uriVariables);
	}

	@Test
	public void expandEncoded() throws Exception {
		template = new UriTemplate("http://example.com/hotel list/{hotel}");
		URI result = template.expand(Collections.singletonMap("hotel", "foo bar \u20AC"));
		assertEquals("Invalid expanded template", new URI("http", "//example.com/hotel list/foo bar \u20AC", null),
				result);
		assertEquals("Invalid expanded template", "http://example.com/hotel%20list/foo%20bar%20%E2%82%AC",
				result.toASCIIString());
	}

	@Test
	public void matches() throws Exception {
		assertTrue("UriTemplate does not match", template.matches("http://example.com/hotels/1/bookings/42"));
		assertFalse("UriTemplate matches", template.matches("http://example.com/hotels/bookings"));
		assertFalse("UriTemplate matches", template.matches(""));
		assertFalse("UriTemplate matches", template.matches(null));
	}

	@Test
	public void match() throws Exception {
		Map<String, String> expected = new HashMap<String, String>(2);
		expected.put("booking", "42");
		expected.put("hotel", "1");

		Map<String, String> result = template.match("http://example.com/hotels/1/bookings/42");
		assertEquals("Invalid match", expected, result);
	}

	@Test
	public void matchDuplicate() throws Exception {
		template = new UriTemplate("/order/{c}/{c}/{c}");
		Map<String, String> result = template.match("/order/cheeseburger/cheeseburger/cheeseburger");
		Map<String, String> expected = Collections.singletonMap("c", "cheeseburger");
		assertEquals("Invalid match", expected, result);
	}

	@Test
	public void matchMultipleInOneSegment() throws Exception {
		template = new UriTemplate("/{foo}-{bar}");
		Map<String, String> result = template.match("/12-34");
		Map<String, String> expected = new HashMap<String, String>(2);
		expected.put("foo", "12");
		expected.put("bar", "34");
		assertEquals("Invalid match", expected, result);
	}
}