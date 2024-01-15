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

package org.springframework.messaging.handler;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.AntPathMatcher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DestinationPatternsMessageCondition}.
 *
 * @author Rossen Stoyanchev
 */
class DestinationPatternsMessageConditionTests {

	@Test
	void prependSlash() {
		DestinationPatternsMessageCondition c = condition("foo");
		assertThat(c.getPatterns()).containsExactly("/foo");
	}

	@Test
	void prependSlashWithCustomPathSeparator() {
		DestinationPatternsMessageCondition c =
				new DestinationPatternsMessageCondition(new String[] {"foo"}, new AntPathMatcher("."));

		assertThat(c.getPatterns())
				.as("Pre-pending should be disabled when not using '/' as path separator")
				.containsExactly("foo");
	}

	// SPR-8255

	@Test
	void prependNonEmptyPatternsOnly() {
		DestinationPatternsMessageCondition c = condition("");
		assertThat(c.getPatterns()).containsOnly("");
	}

	@Test
	void combineEmptySets() {
		DestinationPatternsMessageCondition c1 = condition();
		DestinationPatternsMessageCondition c2 = condition();

		assertThat(c1.combine(c2)).isEqualTo(condition(""));
	}

	@Test
	void combineOnePatternWithEmptySet() {
		DestinationPatternsMessageCondition c1 = condition("/type1", "/type2");
		DestinationPatternsMessageCondition c2 = condition();

		assertThat(c1.combine(c2)).isEqualTo(condition("/type1", "/type2"));

		c1 = condition();
		c2 = condition("/method1", "/method2");

		assertThat(c1.combine(c2)).isEqualTo(condition("/method1", "/method2"));
	}

	@Test
	void combineMultiplePatterns() {
		DestinationPatternsMessageCondition c1 = condition("/t1", "/t2");
		DestinationPatternsMessageCondition c2 = condition("/m1", "/m2");

		assertThat(c1.combine(c2)).isEqualTo(new DestinationPatternsMessageCondition(
				"/t1/m1", "/t1/m2", "/t2/m1", "/t2/m2"));
	}

	@Test
	void matchDirectPath() {
		DestinationPatternsMessageCondition condition = condition("/foo");
		DestinationPatternsMessageCondition match = condition.getMatchingCondition(messageTo("/foo"));

		assertThat(match).isNotNull();
	}

	@Test
	void matchPattern() {
		DestinationPatternsMessageCondition condition = condition("/foo/*");
		DestinationPatternsMessageCondition match = condition.getMatchingCondition(messageTo("/foo/bar"));

		assertThat(match).isNotNull();
	}

	@Test
	void matchSortPatterns() {
		DestinationPatternsMessageCondition condition = condition("/**", "/foo/bar", "/foo/*");
		DestinationPatternsMessageCondition match = condition.getMatchingCondition(messageTo("/foo/bar"));
		DestinationPatternsMessageCondition expected = condition("/foo/bar", "/foo/*", "/**");

		assertThat(match).isEqualTo(expected);
	}

	@Test
	void compareEqualPatterns() {
		DestinationPatternsMessageCondition c1 = condition("/foo*");
		DestinationPatternsMessageCondition c2 = condition("/foo*");

		assertThat(c1.compareTo(c2, messageTo("/foo"))).isEqualTo(0);
	}

	@Test
	void comparePatternSpecificity() {
		DestinationPatternsMessageCondition c1 = condition("/fo*");
		DestinationPatternsMessageCondition c2 = condition("/foo");

		assertThat(c1.compareTo(c2, messageTo("/foo"))).isEqualTo(1);
	}

	@Test
	void compareNumberOfMatchingPatterns() {
		Message<?> message = messageTo("/foo");

		DestinationPatternsMessageCondition c1 = condition("/foo", "bar");
		DestinationPatternsMessageCondition c2 = condition("/foo", "f*");

		DestinationPatternsMessageCondition match1 = c1.getMatchingCondition(message);
		DestinationPatternsMessageCondition match2 = c2.getMatchingCondition(message);

		assertThat(match1.compareTo(match2, message)).isEqualTo(1);
	}


	private DestinationPatternsMessageCondition condition(String... patterns) {
		return new DestinationPatternsMessageCondition(patterns);
	}

	private Message<?> messageTo(String destination) {
		return MessageBuilder.withPayload(new byte[0]).setHeader(
				DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER, destination).build();
	}

}
