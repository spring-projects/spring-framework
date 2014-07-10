/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.handler;

import org.junit.Test;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.Parameter;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link DestinationPatternsMessageCondition}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
@RunWith(Parameterized.class)
public class DestinationPatternsMessageConditionTests {

	@Parameter(0)
	public String pathSeparator;

	@Parameters
	public static Iterable<Object[]> arguments() {
		return Arrays.asList(new Object[][]{{"/"}, {"."}});
	}

	@Test
	public void prependSlash() {
		DestinationPatternsMessageCondition c = condition("foo");
		assertEquals("/foo", c.getPatterns().iterator().next());
	}

	// SPR-8255

	@Test
	public void prependNonEmptyPatternsOnly() {
		DestinationPatternsMessageCondition c = condition("");
		assertEquals("", c.getPatterns().iterator().next());
	}

	@Test
	public void combineEmptySets() {
		DestinationPatternsMessageCondition c1 = condition();
		DestinationPatternsMessageCondition c2 = suffixCondition();

		assertEquals(condition(""), c1.combine(c2));
	}

	@Test
	public void combineOnePatternWithEmptySet() {
		DestinationPatternsMessageCondition c1 = condition("/type1",
				pathSeparator + "type2");
		DestinationPatternsMessageCondition c2 = suffixCondition();

		assertEquals(condition("/type1", pathSeparator + "type2"), c1.combine(c2));

		c1 = condition();
		c2 = suffixCondition("/method1", "/method2");

		assertEquals(condition("/method1", "/method2"), c1.combine(c2));
	}

	@Test
	public void combineMultiplePatterns() {
		DestinationPatternsMessageCondition c1 = condition("/t1", "/t2");
		DestinationPatternsMessageCondition c2 = suffixCondition(pathSeparator + "m1",
				pathSeparator + "m2");

		assertEquals(
				condition("/t1" + pathSeparator + "m1", "/t1" + pathSeparator + "m2",
						"/t2" + pathSeparator + "m1", "/t2" + pathSeparator + "m2"), c1.combine(c2));
	}

	@Test
	public void matchDirectPath() {
		DestinationPatternsMessageCondition condition = condition("/foo");
		DestinationPatternsMessageCondition match = condition.getMatchingCondition(messageTo("/foo"));

		assertNotNull(match);
	}

	@Test
	public void matchPattern() {
		DestinationPatternsMessageCondition condition = condition(
				"/foo" + pathSeparator + "*");
		DestinationPatternsMessageCondition match = condition.getMatchingCondition(messageTo("/foo" + pathSeparator + "bar"));

		assertNotNull(match);
	}

	@Test
	public void matchSortPatterns() {
		DestinationPatternsMessageCondition condition = suffixCondition(
				pathSeparator + "**", pathSeparator + "foo" + pathSeparator + "bar",
				pathSeparator + "foo" + pathSeparator + "*");
		DestinationPatternsMessageCondition match = condition.getMatchingCondition(messageTo(pathSeparator + "foo" + pathSeparator + "bar"));
		DestinationPatternsMessageCondition expected = suffixCondition(
				pathSeparator + "foo" + pathSeparator + "bar",
				pathSeparator + "foo" + pathSeparator + "*", pathSeparator + "**");

		assertEquals(expected, match);
	}

	@Test
	public void compareEqualPatterns() {
		DestinationPatternsMessageCondition c1 = suffixCondition(pathSeparator + "foo*");
		DestinationPatternsMessageCondition c2 = suffixCondition(pathSeparator + "foo*");

		assertEquals(0, c1.compareTo(c2, messageTo(pathSeparator + "foo")));
	}

	@Test
	public void comparePatternSpecificity() {
		DestinationPatternsMessageCondition c1 = suffixCondition(pathSeparator + "fo*");
		DestinationPatternsMessageCondition c2 = suffixCondition(pathSeparator + "foo");

		assertEquals(1, c1.compareTo(c2, messageTo(pathSeparator + "foo")));
	}

	@Test
	public void compareNumberOfMatchingPatterns() throws Exception {
		Message<?> message = messageTo("/foo");

		DestinationPatternsMessageCondition c1 = condition("/foo", "bar");
		DestinationPatternsMessageCondition c2 = condition("/foo", "f*");

		DestinationPatternsMessageCondition match1 = c1.getMatchingCondition(message);
		DestinationPatternsMessageCondition match2 = c2.getMatchingCondition(message);

		assertEquals(1, match1.compareTo(match2, message));
	}


	private DestinationPatternsMessageCondition condition(String... patterns) {
		return new DestinationPatternsMessageCondition(patterns, new AntPathMatcher(this.pathSeparator));
	}

	private DestinationPatternsMessageCondition suffixCondition(String... patterns) {
		return new DestinationPatternsMessageCondition(patterns, new AntPathMatcher(this.pathSeparator), false);
	}

	private Message<?> messageTo(String destination) {
		return MessageBuilder.withPayload(new byte[0]).setHeader(
				DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER, destination).build();
	}

}
