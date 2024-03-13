/*
 * Copyright 2002-2024 the original author or authors.
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
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 */
class UriTemplateTests {

	@Test
	void emptyPathDoesNotThrowException() {
		assertThatNoException().isThrownBy(() -> new UriTemplate(""));
	}

	@Test
	void nullPathThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new UriTemplate(null));
	}

	@Test
	void getVariableNames() {
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		List<String> variableNames = template.getVariableNames();
		assertThat(variableNames).as("Invalid variable names").isEqualTo(Arrays.asList("hotel", "booking"));
	}

	@Test
	void getVariableNamesFromEmpty() {
		UriTemplate template = new UriTemplate("");
		List<String> variableNames = template.getVariableNames();
		assertThat(variableNames).isEmpty();
	}

	@Test
	void expandVarArgs() {
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		URI result = template.expand("1", "42");
		assertThat(result).as("Invalid expanded template").isEqualTo(URI.create("/hotels/1/bookings/42"));
	}

	@Test
	void expandVarArgsFromEmpty() {
		UriTemplate template = new UriTemplate("");
		URI result = template.expand();
		assertThat(result).as("Invalid expanded template").isEqualTo(URI.create(""));
	}

	@Test  // SPR-9712
	void expandVarArgsWithArrayValue() {
		UriTemplate template = new UriTemplate("/sum?numbers={numbers}");
		URI result = template.expand(new int[] {1, 2, 3});
		assertThat(result).isEqualTo(URI.create("/sum?numbers=1,2,3"));
	}

	@Test
	void expandVarArgsNotEnoughVariables() {
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		assertThatIllegalArgumentException().isThrownBy(() -> template.expand("1"));
	}

	@Test
	void expandMap() {
		Map<String, String> uriVariables = new HashMap<>(2);
		uriVariables.put("booking", "42");
		uriVariables.put("hotel", "1");
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		URI result = template.expand(uriVariables);
		assertThat(result).as("Invalid expanded template").isEqualTo(URI.create("/hotels/1/bookings/42"));
	}

	@Test
	void expandMapDuplicateVariables() {
		UriTemplate template = new UriTemplate("/order/{c}/{c}/{c}");
		assertThat(template.getVariableNames()).isEqualTo(Arrays.asList("c", "c", "c"));
		URI result = template.expand(Collections.singletonMap("c", "cheeseburger"));
		assertThat(result).isEqualTo(URI.create("/order/cheeseburger/cheeseburger/cheeseburger"));
	}

	@Test
	void expandMapNonString() {
		Map<String, Integer> uriVariables = new HashMap<>(2);
		uriVariables.put("booking", 42);
		uriVariables.put("hotel", 1);
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		URI result = template.expand(uriVariables);
		assertThat(result).as("Invalid expanded template").isEqualTo(URI.create("/hotels/1/bookings/42"));
	}

	@Test
	void expandMapEncoded() {
		Map<String, String> uriVariables = Collections.singletonMap("hotel", "Z\u00fcrich");
		UriTemplate template = new UriTemplate("/hotel list/{hotel}");
		URI result = template.expand(uriVariables);
		assertThat(result).as("Invalid expanded template").isEqualTo(URI.create("/hotel%20list/Z%C3%BCrich"));
	}

	@Test
	void expandMapUnboundVariables() {
		Map<String, String> uriVariables = new HashMap<>(2);
		uriVariables.put("booking", "42");
		uriVariables.put("bar", "1");
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		assertThatIllegalArgumentException().isThrownBy(() ->
				template.expand(uriVariables));
	}

	@Test
	void expandEncoded() {
		UriTemplate template = new UriTemplate("/hotel list/{hotel}");
		URI result = template.expand("Z\u00fcrich");
		assertThat(result).as("Invalid expanded template").isEqualTo(URI.create("/hotel%20list/Z%C3%BCrich"));
	}

	@Test
	void matches() {
		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		assertThat(template.matches("/hotels/1/bookings/42")).as("UriTemplate does not match").isTrue();
		assertThat(template.matches("/hotels/bookings")).as("UriTemplate matches").isFalse();
		assertThat(template.matches("")).as("UriTemplate matches").isFalse();
		assertThat(template.matches(null)).as("UriTemplate matches").isFalse();
	}

	@Test
	void matchesAgainstEmpty() {
		UriTemplate template = new UriTemplate("");
		assertThat(template.matches("/hotels/1/bookings/42")).as("UriTemplate matches").isFalse();
		assertThat(template.matches("/hotels/bookings")).as("UriTemplate matches").isFalse();
		assertThat(template.matches("")).as("UriTemplate does not match").isTrue();
		assertThat(template.matches(null)).as("UriTemplate matches").isFalse();
	}

	@Test
	void matchesCustomRegex() {
		UriTemplate template = new UriTemplate("/hotels/{hotel:\\d+}");
		assertThat(template.matches("/hotels/42")).as("UriTemplate does not match").isTrue();
		assertThat(template.matches("/hotels/foo")).as("UriTemplate matches").isFalse();
	}

	@Test
	void match() {
		Map<String, String> expected = new HashMap<>(2);
		expected.put("booking", "42");
		expected.put("hotel", "1");

		UriTemplate template = new UriTemplate("/hotels/{hotel}/bookings/{booking}");
		Map<String, String> result = template.match("/hotels/1/bookings/42");
		assertThat(result).as("Invalid match").isEqualTo(expected);
	}

	@Test
	void matchAgainstEmpty() {
		UriTemplate template = new UriTemplate("");
		Map<String, String> result = template.match("/hotels/1/bookings/42");
		assertThat(result).as("Invalid match").isEmpty();
	}

	@Test
	void matchCustomRegex() {
		Map<String, String> expected = new HashMap<>(2);
		expected.put("booking", "42");
		expected.put("hotel", "1");

		UriTemplate template = new UriTemplate("/hotels/{hotel:\\d}/bookings/{booking:\\d+}");
		Map<String, String> result = template.match("/hotels/1/bookings/42");
		assertThat(result).as("Invalid match").isEqualTo(expected);
	}

	@Test  // SPR-13627
	void matchCustomRegexWithNestedCurlyBraces() {
		UriTemplate template = new UriTemplate("/site.{domain:co.[a-z]{2}}");
		Map<String, String> result = template.match("/site.co.eu");
		assertThat(result).as("Invalid match").isEqualTo(Collections.singletonMap("domain", "co.eu"));
	}

	@Test
	void matchDuplicate() {
		UriTemplate template = new UriTemplate("/order/{c}/{c}/{c}");
		Map<String, String> result = template.match("/order/cheeseburger/cheeseburger/cheeseburger");
		Map<String, String> expected = Collections.singletonMap("c", "cheeseburger");
		assertThat(result).as("Invalid match").isEqualTo(expected);
	}

	@Test
	void matchMultipleInOneSegment() {
		UriTemplate template = new UriTemplate("/{foo}-{bar}");
		Map<String, String> result = template.match("/12-34");
		Map<String, String> expected = new HashMap<>(2);
		expected.put("foo", "12");
		expected.put("bar", "34");
		assertThat(result).as("Invalid match").isEqualTo(expected);
	}

	@Test  // SPR-16169
	void matchWithMultipleSegmentsAtTheEnd() {
		UriTemplate template = new UriTemplate("/account/{accountId}");
		assertThat(template.matches("/account/15/alias/5")).isFalse();
	}

	@Test
	void queryVariables() {
		UriTemplate template = new UriTemplate("/search?q={query}");
		assertThat(template.matches("/search?q=foo")).isTrue();
	}

	@Test
	void fragments() {
		UriTemplate template = new UriTemplate("/search#{fragment}");
		assertThat(template.matches("/search#foo")).isTrue();

		template = new UriTemplate("/search?query={query}#{fragment}");
		assertThat(template.matches("/search?query=foo#bar")).isTrue();
	}

	@Test  // SPR-13705
	void matchesWithSlashAtTheEnd() {
		assertThat(new UriTemplate("/test/").matches("/test/")).isTrue();
	}

	@Test
	void expandWithDollar() {
		UriTemplate template = new UriTemplate("/{a}");
		URI uri = template.expand("$replacement");
		assertThat(uri.toString()).isEqualTo("/$replacement");
	}

	@Test
	void expandWithAtSign() {
		UriTemplate template = new UriTemplate("http://localhost/query={query}");
		URI uri = template.expand("foo@bar");
		assertThat(uri.toString()).isEqualTo("http://localhost/query=foo@bar");
	}

}
