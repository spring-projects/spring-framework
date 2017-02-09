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

package org.springframework.web.util.patterns;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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

	private PathPatternRegistry registry;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setUp() throws Exception {
		this.registry = new PathPatternRegistry();
	}

	@Test
	public void shouldFixFileExtensions() {
		Set<String> fileExtensions = new HashSet<>();
		fileExtensions.add("json");
		fileExtensions.add("xml");
		this.registry.setFileExtensions(fileExtensions);
		assertThat(this.registry.getFileExtensions(), contains(".json", ".xml"));
	}

	@Test
	public void shouldPrependPatternsWithSlash() {
		this.registry.register("foo/bar");
		assertThat(getPatternList(this.registry.getPatterns()), Matchers.containsInAnyOrder("/foo/bar"));
	}

	@Test
	public void shouldNotRegisterInvalidPatterns() {
		this.thrown.expect(PatternParseException.class);
		this.thrown.expectMessage(Matchers.containsString("Expected close capture character after variable name"));
		this.registry.register("/{invalid");
	}

	@Test
	public void shouldNotRegisterPatternVariants() {
		List<PathPattern> patterns = this.registry.register("/foo/{bar}");
		assertThat(getPatternList(patterns), Matchers.containsInAnyOrder("/foo/{bar}"));
	}

	@Test
	public void shouldRegisterTrailingSlashVariants() {
		this.registry.setUseTrailingSlashMatch(true);
		List<PathPattern> patterns = this.registry.register("/foo/{bar}");
		assertThat(getPatternList(patterns), Matchers.containsInAnyOrder("/foo/{bar}", "/foo/{bar}/"));
	}

	@Test
	public void shouldRegisterSuffixVariants() {
		this.registry.setUseSuffixPatternMatch(true);
		List<PathPattern> patterns = this.registry.register("/foo/{bar}");
		assertThat(getPatternList(patterns), Matchers.containsInAnyOrder("/foo/{bar}", "/foo/{bar}.*"));
	}

	@Test
	public void shouldRegisterExtensionsVariants() {
		Set<String> fileExtensions = new HashSet<>();
		fileExtensions.add(".json");
		fileExtensions.add(".xml");
		this.registry.setUseSuffixPatternMatch(true);
		this.registry.setFileExtensions(fileExtensions);
		List<PathPattern> patterns = this.registry.register("/foo/{bar}");
		assertThat(getPatternList(patterns),
				Matchers.containsInAnyOrder("/foo/{bar}", "/foo/{bar}.xml", "/foo/{bar}.json"));
	}

	@Test
	public void shouldRegisterAllVariants() {
		Set<String> fileExtensions = new HashSet<>();
		fileExtensions.add(".json");
		fileExtensions.add(".xml");
		this.registry.setUseSuffixPatternMatch(true);
		this.registry.setUseTrailingSlashMatch(true);
		this.registry.setFileExtensions(fileExtensions);
		List<PathPattern> patterns = this.registry.register("/foo/{bar}");
		assertThat(getPatternList(patterns), Matchers.containsInAnyOrder("/foo/{bar}",
				"/foo/{bar}.xml", "/foo/{bar}.json", "/foo/{bar}/"));
	}

	@Test
	public void combineEmptyRegistries() {
		PathPatternRegistry result = this.registry.combine(new PathPatternRegistry());
		assertThat(getPatternList(result.getPatterns()), Matchers.containsInAnyOrder(""));
	}

	@Test
	public void combineWithEmptyRegistry() {
		this.registry.register("/foo");
		PathPatternRegistry result = this.registry.combine(new PathPatternRegistry());
		assertThat(getPatternList(result.getPatterns()), Matchers.containsInAnyOrder("/foo"));
	}

	@Test
	public void combineRegistries() {
		this.registry.register("/foo");
		PathPatternRegistry other = new PathPatternRegistry();
		other.register("/bar");
		other.register("/baz");
		PathPatternRegistry result = this.registry.combine(other);
		assertThat(getPatternList(result.getPatterns()), Matchers.containsInAnyOrder("/foo/bar", "/foo/baz"));
	}

	@Test
	public void registerPatternsWithSameSpecificity() {
		PathPattern fooOne = this.registry.parsePattern("/fo?");
		PathPattern fooTwo = this.registry.parsePattern("/f?o");
		assertThat(fooOne.compareTo(fooTwo), is(0));

		this.registry.register("/fo?");
		this.registry.register("/f?o");
		Set<PathPattern> matches = this.registry.findMatches("/foo");
		assertThat(getPatternList(matches), Matchers.contains("/f?o", "/fo?"));
	}

	@Test
	public void findNoMatch() {
		this.registry.register("/foo/{bar}");
		assertThat(this.registry.findMatches("/other"), hasSize(0));
	}

	@Test
	public void orderMatchesBySpecificity() {
		this.registry.register("/foo/{*baz}");
		this.registry.register("/foo/bar/baz");
		this.registry.register("/foo/bar/{baz}");
		Set<PathPattern> matches = this.registry.findMatches("/foo/bar/baz");
		assertThat(getPatternList(matches), Matchers.contains("/foo/bar/baz", "/foo/bar/{baz}",
				"/foo/{*baz}"));
	}


	private List<String> getPatternList(Collection<PathPattern> parsedPatterns) {
		return parsedPatterns.stream().map(pattern -> pattern.getPatternString()).collect(Collectors.toList());

	}

}
