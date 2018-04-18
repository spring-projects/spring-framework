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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 */
public class UriTemplateTests {

	@Test
	public void getVariableNames() throws Exception {
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		List<String> variableNames = template.getVariableNames();
		assertEquals("Invalid variable names", Arrays.asList("hotel", "booking"), variableNames);
	}

	@Test
	public void expandVarArgs() throws Exception {
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		URI result = template.expand("1", "42");
		assertEquals("Invalid expanded template", new URI("/hotels/1/bookings/42"), result);
	}

	// SPR-9712

	@Test
	public void expandVarArgsWithArrayValue() throws Exception {
		UriTemplate template = new UriTemplate("/sum?numbers={numbers}");
		URI result = template.expand(new int[] {1, 2, 3});
		assertEquals(new URI("/sum?numbers=1,2,3"), result);
	}

	@Test(expected = IllegalArgumentException.class)
	public void expandVarArgsNotEnoughVariables() throws Exception {
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		template.expand("1");
	}

	@Test
	public void expandMap() throws Exception {
		Map<String, String> uriVariables = new HashMap<>(2);
		uriVariables.put("booking", "42");
		uriVariables.put("hotel", "1");
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		URI result = template.expand(uriVariables);
		assertEquals("Invalid expanded template", new URI("/hotels/1/bookings/42"), result);
	}

	@Test
	public void expandMapDuplicateVariables() throws Exception {
		UriTemplate template = new UriTemplate("/order/{c}/{c}/{c}");
		assertEquals(Arrays.asList("c", "c", "c"), template.getVariableNames());
		URI result = template.expand(Collections.singletonMap("c", "cheeseburger"));
		assertEquals(new URI("/order/cheeseburger/cheeseburger/cheeseburger"), result);
	}

	@Test
	public void expandMapNonString() throws Exception {
		Map<String, Integer> uriVariables = new HashMap<>(2);
		uriVariables.put("booking", 42);
		uriVariables.put("hotel", 1);
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		URI result = template.expand(uriVariables);
		assertEquals("Invalid expanded template", new URI("/hotels/1/bookings/42"), result);
	}

	@Test
	public void expandMapEncoded() throws Exception {
		Map<String, String> uriVariables = Collections.singletonMap("hotel", "Z\u00fcrich");
		UriTemplate template = new UriTemplate("/hotel list/{hotel}");
		URI result = template.expand(uriVariables);
		assertEquals("Invalid expanded template", new URI("/hotel%20list/Z%C3%BCrich"), result);
	}

	@Test(expected = IllegalArgumentException.class)
	public void expandMapUnboundVariables() throws Exception {
		Map<String, String> uriVariables = new HashMap<>(2);
		uriVariables.put("booking", "42");
		uriVariables.put("bar", "1");
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		template.expand(uriVariables);
	}

	@Test
	public void expandEncoded() throws Exception {
		UriTemplate template = new UriTemplate("/hotel list/{hotel}");
		URI result = template.expand("Z\u00fcrich");
		assertEquals("Invalid expanded template", new URI("/hotel%20list/Z%C3%BCrich"), result);
	}

	@Test
	public void matches() throws Exception {
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		assertTrue("UriTemplate does not match", template.matches("/hotels/1/bookings/42"));
		assertFalse("UriTemplate matches", template.matches("/hotels/bookings"));
		assertFalse("UriTemplate matches", template.matches(""));
		assertFalse("UriTemplate matches", template.matches(null));
	}

	@Test
	public void matchesCustomRegex() throws Exception {
		UriTemplate template = new UriTemplate("/hotels/{hotel:\\d+}");
		assertTrue("UriTemplate does not match", template.matches("/hotels/42"));
		assertFalse("UriTemplate matches", template.matches("/hotels/foo"));
	}

	@Test
	public void match() throws Exception {
		Map<String, String> expected = new HashMap<>(2);
		expected.put("booking", "42");
		expected.put("hotel", "1");

		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		Map<String, String> result = template.match("/hotels/1/bookings/42");
		assertEquals("Invalid match", expected, result);
	}

	@Test
	public void matchCustomRegex() throws Exception {
		Map<String, String> expected = new HashMap<>(2);
		expected.put("booking", "42");
		expected.put("hotel", "1");

		UriTemplate template = new UriTemplate("/hotels/{hotel:\\d}/bookings/{booking:\\d+}");
		Map<String, String> result = template.match("/hotels/1/bookings/42");
		assertEquals("Invalid match", expected, result);
	}

	@Test // SPR-13627
	public void matchCustomRegexWithNestedCurlyBraces() throws Exception {
		UriTemplate template = new UriTemplate("/site.{domain:co.[a-z]{2}}");
		Map<String, String> result = template.match("/site.co.eu");
		assertEquals("Invalid match", Collections.singletonMap("domain", "co.eu"), result);
	}

	@Test
	public void matchDuplicate() throws Exception {
		UriTemplate template = new UriTemplate("/order/{c}/{c}/{c}");
		Map<String, String> result = template.match("/order/cheeseburger/cheeseburger/cheeseburger");
		Map<String, String> expected = Collections.singletonMap("c", "cheeseburger");
		assertEquals("Invalid match", expected, result);
	}

	@Test
	public void matchMultipleInOneSegment() throws Exception {
		UriTemplate template = new UriTemplate("/{foo}-{bar}");
		Map<String, String> result = template.match("/12-34");
		Map<String, String> expected = new HashMap<>(2);
		expected.put("foo", "12");
		expected.put("bar", "34");
		assertEquals("Invalid match", expected, result);
	}

	@Test // SPR-16169
	public void matchWithMultipleSegmentsAtTheEnd() {
		UriTemplate template = new UriTemplate("/account/{accountId}");
		assertFalse(template.matches("/account/15/alias/5"));
	}

	@Test
	public void queryVariables() throws Exception {
		UriTemplate template = new UriTemplate("/search?q={query}");
		assertTrue(template.matches("/search?q=foo"));
	}

	@Test
	public void fragments() throws Exception {
		UriTemplate template = new UriTemplate("/search#{fragment}");
		assertTrue(template.matches("/search#foo"));

		template = new UriTemplate("/search?query={query}#{fragment}");
		assertTrue(template.matches("/search?query=foo#bar"));
	}

	@Test // SPR-13705
	public void matchesWithSlashAtTheEnd() {
		UriTemplate uriTemplate = new UriTemplate("/test/");
		assertTrue(uriTemplate.matches("/test/"));
	}

	@Test
	public void expandWithDollar() {
		UriTemplate template = new UriTemplate("/{a}");
		URI uri = template.expand("$replacement");
		assertEquals("/$replacement", uri.toString());
	}

	@Test
	public void expandWithAtSign() {
		UriTemplate template = new UriTemplate("http://localhost/query={query}");
		URI uri = template.expand("foo@bar");
		assertEquals("http://localhost/query=foo@bar", uri.toString());
	}

}
