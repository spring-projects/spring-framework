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

package org.springframework.web.util;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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
		assertThat(variableNames).as("Invalid variable names").isEqualTo(Arrays.asList("hotel", "booking"));
	}

	@Test
	public void expandVarArgs() throws Exception {
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		URI result = template.expand("1", "42");
		assertThat(result).as("Invalid expanded template").isEqualTo(new URI("/hotels/1/bookings/42"));
	}

	// SPR-9712

	@Test
	public void expandVarArgsWithArrayValue() throws Exception {
		UriTemplate template = new UriTemplate("/sum?numbers={numbers}");
		URI result = template.expand(new int[] {1, 2, 3});
		assertThat(result).isEqualTo(new URI("/sum?numbers=1,2,3"));
	}

	@Test
	public void expandVarArgsNotEnoughVariables() throws Exception {
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		assertThatIllegalArgumentException().isThrownBy(() ->
				template.expand("1"));
	}

	@Test
	public void expandMap() throws Exception {
		Map<String, String> uriVariables = new HashMap<>(2);
		uriVariables.put("booking", "42");
		uriVariables.put("hotel", "1");
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		URI result = template.expand(uriVariables);
		assertThat(result).as("Invalid expanded template").isEqualTo(new URI("/hotels/1/bookings/42"));
	}

	@Test
	public void expandMapDuplicateVariables() throws Exception {
		UriTemplate template = new UriTemplate("/order/{c}/{c}/{c}");
		assertThat(template.getVariableNames()).isEqualTo(Arrays.asList("c", "c", "c"));
		URI result = template.expand(Collections.singletonMap("c", "cheeseburger"));
		assertThat(result).isEqualTo(new URI("/order/cheeseburger/cheeseburger/cheeseburger"));
	}

	@Test
	public void expandMapNonString() throws Exception {
		Map<String, Integer> uriVariables = new HashMap<>(2);
		uriVariables.put("booking", 42);
		uriVariables.put("hotel", 1);
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		URI result = template.expand(uriVariables);
		assertThat(result).as("Invalid expanded template").isEqualTo(new URI("/hotels/1/bookings/42"));
	}

	@Test
	public void expandMapEncoded() throws Exception {
		Map<String, String> uriVariables = Collections.singletonMap("hotel", "Z\u00fcrich");
		UriTemplate template = new UriTemplate("/hotel list/{hotel}");
		URI result = template.expand(uriVariables);
		assertThat(result).as("Invalid expanded template").isEqualTo(new URI("/hotel%20list/Z%C3%BCrich"));
	}

	@Test
	public void expandMapUnboundVariables() throws Exception {
		Map<String, String> uriVariables = new HashMap<>(2);
		uriVariables.put("booking", "42");
		uriVariables.put("bar", "1");
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		assertThatIllegalArgumentException().isThrownBy(() ->
				template.expand(uriVariables));
	}

	@Test
	public void expandEncoded() throws Exception {
		UriTemplate template = new UriTemplate("/hotel list/{hotel}");
		URI result = template.expand("Z\u00fcrich");
		assertThat(result).as("Invalid expanded template").isEqualTo(new URI("/hotel%20list/Z%C3%BCrich"));
	}

	@Test
	public void matches() throws Exception {
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		assertThat(template.matches("/hotels/1/bookings/42")).as("UriTemplate does not match").isTrue();
		assertThat(template.matches("/hotels/bookings")).as("UriTemplate matches").isFalse();
		assertThat(template.matches("")).as("UriTemplate matches").isFalse();
		assertThat(template.matches(null)).as("UriTemplate matches").isFalse();
	}

	@Test
	public void matchesCustomRegex() throws Exception {
		UriTemplate template = new UriTemplate("/hotels/{hotel:\\d+}");
		assertThat(template.matches("/hotels/42")).as("UriTemplate does not match").isTrue();
		assertThat(template.matches("/hotels/foo")).as("UriTemplate matches").isFalse();
	}

	@Test
	public void match() throws Exception {
		Map<String, String> expected = new HashMap<>(2);
		expected.put("booking", "42");
		expected.put("hotel", "1");

		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		Map<String, String> result = template.match("/hotels/1/bookings/42");
		assertThat(result).as("Invalid match").isEqualTo(expected);
	}

	@Test
	public void matchCustomRegex() throws Exception {
		Map<String, String> expected = new HashMap<>(2);
		expected.put("booking", "42");
		expected.put("hotel", "1");

		UriTemplate template = new UriTemplate("/hotels/{hotel:\\d}/bookings/{booking:\\d+}");
		Map<String, String> result = template.match("/hotels/1/bookings/42");
		assertThat(result).as("Invalid match").isEqualTo(expected);
	}

	@Test // SPR-13627
	public void matchCustomRegexWithNestedCurlyBraces() throws Exception {
		UriTemplate template = new UriTemplate("/site.{domain:co.[a-z]{2}}");
		Map<String, String> result = template.match("/site.co.eu");
		assertThat(result).as("Invalid match").isEqualTo(Collections.singletonMap("domain", "co.eu"));
	}

	@Test
	public void matchDuplicate() throws Exception {
		UriTemplate template = new UriTemplate("/order/{c}/{c}/{c}");
		Map<String, String> result = template.match("/order/cheeseburger/cheeseburger/cheeseburger");
		Map<String, String> expected = Collections.singletonMap("c", "cheeseburger");
		assertThat(result).as("Invalid match").isEqualTo(expected);
	}

	@Test
	public void matchMultipleInOneSegment() throws Exception {
		UriTemplate template = new UriTemplate("/{foo}-{bar}");
		Map<String, String> result = template.match("/12-34");
		Map<String, String> expected = new HashMap<>(2);
		expected.put("foo", "12");
		expected.put("bar", "34");
		assertThat(result).as("Invalid match").isEqualTo(expected);
	}

	@Test // SPR-16169
	public void matchWithMultipleSegmentsAtTheEnd() {
		UriTemplate template = new UriTemplate("/account/{accountId}");
		assertThat(template.matches("/account/15/alias/5")).isFalse();
	}

	@Test
	public void queryVariables() throws Exception {
		UriTemplate template = new UriTemplate("/search?q={query}");
		assertThat(template.matches("/search?q=foo")).isTrue();
	}

	@Test
	public void fragments() throws Exception {
		UriTemplate template = new UriTemplate("/search#{fragment}");
		assertThat(template.matches("/search#foo")).isTrue();

		template = new UriTemplate("/search?query={query}#{fragment}");
		assertThat(template.matches("/search?query=foo#bar")).isTrue();
	}

	@Test // SPR-13705
	public void matchesWithSlashAtTheEnd() {
		UriTemplate uriTemplate = new UriTemplate("/test/");
		assertThat(uriTemplate.matches("/test/")).isTrue();
	}

	@Test
	public void expandWithDollar() {
		UriTemplate template = new UriTemplate("/{a}");
		URI uri = template.expand("$replacement");
		assertThat(uri.toString()).isEqualTo("/$replacement");
	}

	@Test
	public void expandWithAtSign() {
		UriTemplate template = new UriTemplate("http://localhost/query={query}");
		URI uri = template.expand("foo@bar");
		assertThat(uri.toString()).isEqualTo("http://localhost/query=foo@bar");
	}

}
