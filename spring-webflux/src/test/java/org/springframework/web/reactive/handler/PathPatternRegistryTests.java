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

package org.springframework.web.reactive.handler;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link PathPatternRegistry}
 *
 * @author Brian Clozel
 */
public class PathPatternRegistryTests {

	private PathPatternRegistry<Object> registry;

	private final PathPatternParser parser = new PathPatternParser();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setUp() throws Exception {
		this.registry = new PathPatternRegistry();
	}

	@Test
	public void shouldPrependPatternsWithSlash() {
		this.registry.register("foo/bar", new Object());
		assertThat(this.registry.getPatternsMap().keySet(), contains(pattern("/foo/bar")));
	}

	@Test
	public void shouldNotRegisterInvalidPatterns() {
		this.thrown.expect(PatternParseException.class);
		this.thrown.expectMessage(Matchers.containsString("Expected close capture character after variable name"));
		this.registry.register("/{invalid", new Object());
	}

	@Test
	public void registerPatternsWithSameSpecificity() {
		PathPattern fooOne = this.parser.parse("/fo?");
		PathPattern fooTwo = this.parser.parse("/f?o");
		assertThat(fooOne.compareTo(fooTwo), is(0));

		this.registry.register("/fo?", new Object());
		this.registry.register("/f?o", new Object());
		Set<PathMatchResult<Object>> matches = this.registry.findMatches("/foo");
		assertThat(getPatternList(matches), contains(pattern("/f?o"), pattern("/fo?")));
	}

	@Test
	public void findNoMatch() {
		this.registry.register("/foo/{bar}", new Object());
		assertThat(this.registry.findMatches("/other"), hasSize(0));
	}

	@Test
	public void orderMatchesBySpecificity() {
		this.registry.register("/foo/{*baz}", new Object());
		this.registry.register("/foo/bar/baz", new Object());
		this.registry.register("/foo/bar/{baz}", new Object());
		Set<PathMatchResult<Object>> matches = this.registry.findMatches("/foo/bar/baz");
		assertThat(getPatternList(matches), contains(pattern("/foo/bar/baz"), pattern("/foo/bar/{baz}"),
				pattern("/foo/{*baz}")));
	}


	private List<PathPattern> getPatternList(Collection<PathMatchResult<Object>> results) {
		return results.stream()
				.map(result -> result.getPattern()).collect(Collectors.toList());

	}

	private static PathPatternMatcher pattern(String pattern) {
		return new PathPatternMatcher(pattern);
	}

	private static class PathPatternMatcher extends BaseMatcher<PathPattern> {

		private final String pattern;

		public PathPatternMatcher(String pattern) {
			this.pattern = pattern;
		}

		@Override
		public boolean matches(Object item) {
			if(item != null && item instanceof PathPattern) {
				return ((PathPattern) item).getPatternString().equals(pattern);
			}
			return false;
		}

		@Override
		public void describeTo(Description description) {

		}
	}

}