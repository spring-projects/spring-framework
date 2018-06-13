/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.env;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.util.StringUtils;

import static org.junit.Assert.*;

/**
 * Tests for {@link Profiles}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class ProfilesTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void ofWhenNullThrowsException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Must specify at least one profile");
		Profiles.of((String[]) null);
	}

	@Test
	public void ofWhenEmptyThrowsException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Must specify at least one profile");
		Profiles.of();
	}

	@Test
	public void ofNullElement() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("must contain text");
		Profiles.of((String) null);
	}

	@Test
	public void ofSingleElement() {
		Profiles profiles = Profiles.of("spring");
		assertTrue(profiles.matches(new MockActiveProfiles("spring")));
		assertFalse(profiles.matches(new MockActiveProfiles("framework")));
	}

	@Test
	public void ofSingleInvertedElement() {
		Profiles profiles = Profiles.of("!spring");
		assertFalse(profiles.matches(new MockActiveProfiles("spring")));
		assertTrue(profiles.matches(new MockActiveProfiles("framework")));
	}

	@Test
	public void ofMultipleElements() {
		Profiles profiles = Profiles.of("spring", "framework");
		assertTrue(profiles.matches(new MockActiveProfiles("spring")));
		assertTrue(profiles.matches(new MockActiveProfiles("framework")));
		assertFalse(profiles.matches(new MockActiveProfiles("java")));
	}

	@Test
	public void ofMultipleElementsWithInverted() {
		Profiles profiles = Profiles.of("!spring", "framework");
		assertFalse(profiles.matches(new MockActiveProfiles("spring")));
		assertTrue(profiles.matches(new MockActiveProfiles("framework")));
		assertTrue(profiles.matches(new MockActiveProfiles("spring", "framework")));
	}

	@Test
	public void ofMultipleElementsAllInverted() {
		Profiles profiles = Profiles.of("!spring", "!framework");
		assertTrue(profiles.matches(new MockActiveProfiles("spring")));
		assertTrue(profiles.matches(new MockActiveProfiles("framework")));
		assertFalse(profiles.matches(new MockActiveProfiles("spring", "framework")));
		assertFalse(
				profiles.matches(new MockActiveProfiles("spring", "framework", "java")));
	}

	@Test
	public void ofSingleExpression() {
		Profiles profiles = Profiles.of("(spring)");
		assertTrue(profiles.matches(new MockActiveProfiles("spring")));
		assertFalse(profiles.matches(new MockActiveProfiles("framework")));
	}

	@Test
	public void ofSingleInvertedExpression() {
		Profiles profiles = Profiles.of("(!spring)");
		assertFalse(profiles.matches(new MockActiveProfiles("spring")));
		assertTrue(profiles.matches(new MockActiveProfiles("framework")));
	}

	@Test
	public void ofOrExpression() {
		Profiles profiles = Profiles.of("(spring | framework)");
		assertOrExpression(profiles);
	}

	@Test
	public void ofOrExpressionWithoutSpace() {
		Profiles profiles = Profiles.of("(spring|framework)");
		assertOrExpression(profiles);
	}

	private void assertOrExpression(Profiles profiles) {
		assertTrue(profiles.matches(new MockActiveProfiles("spring")));
		assertTrue(profiles.matches(new MockActiveProfiles("framework")));
		assertTrue(profiles.matches(new MockActiveProfiles("spring", "framework")));
		assertFalse(profiles.matches(new MockActiveProfiles("java")));
	}

	@Test
	public void ofAndExpression() {
		Profiles profiles = Profiles.of("(spring & framework)");
		assertAndExpression(profiles);
	}

	@Test
	public void ofAndExpressionWithoutSpace() {
		Profiles profiles = Profiles.of("spring&framework)");
		assertAndExpression(profiles);
	}

	@Test
	public void ofAndExpressionWithoutBraces() {
		Profiles profiles = Profiles.of("spring & framework");
		assertAndExpression(profiles);
	}

	private void assertAndExpression(Profiles profiles) {
		assertFalse(profiles.matches(new MockActiveProfiles("spring")));
		assertFalse(profiles.matches(new MockActiveProfiles("framework")));
		assertTrue(profiles.matches(new MockActiveProfiles("spring", "framework")));
		assertFalse(profiles.matches(new MockActiveProfiles("java")));
	}

	@Test
	public void ofNotAndExpression() {
		Profiles profiles = Profiles.of("!(spring & framework)");
		assertOfNotAndExpression(profiles);
	}

	@Test
	public void ofNotAndExpressionWithoutSpace() {
		Profiles profiles = Profiles.of("!(spring&framework)");
		assertOfNotAndExpression(profiles);
	}

	private void assertOfNotAndExpression(Profiles profiles) {
		assertTrue(profiles.matches(new MockActiveProfiles("spring")));
		assertTrue(profiles.matches(new MockActiveProfiles("framework")));
		assertFalse(profiles.matches(new MockActiveProfiles("spring", "framework")));
		assertTrue(profiles.matches(new MockActiveProfiles("java")));
	}

	@Test
	public void ofNotOrExpression() {
		Profiles profiles = Profiles.of("!(spring | framework)");
		assertOfNotOrExpression(profiles);
	}

	@Test
	public void ofNotOrExpressionWithoutSpace() {
		Profiles profiles = Profiles.of("!(spring|framework)");
		assertOfNotOrExpression(profiles);
	}

	private void assertOfNotOrExpression(Profiles profiles) {
		assertFalse(profiles.matches(new MockActiveProfiles("spring")));
		assertFalse(profiles.matches(new MockActiveProfiles("framework")));
		assertFalse(profiles.matches(new MockActiveProfiles("spring", "framework")));
		assertTrue(profiles.matches(new MockActiveProfiles("java")));
	}

	@Test
	public void ofComplexExpression() {
		Profiles profiles = Profiles.of("(spring & framework) | (spring & java)");
		assertComplexExpression(profiles);
	}

	@Test
	public void ofComplexExpressionWithoutSpace() {
		Profiles profiles = Profiles.of("(spring&framework)|(spring&java)");
		assertComplexExpression(profiles);
	}

	private void assertComplexExpression(Profiles profiles) {
		assertFalse(profiles.matches(new MockActiveProfiles("spring")));
		assertTrue(profiles.matches(new MockActiveProfiles("spring", "framework")));
		assertTrue(profiles.matches(new MockActiveProfiles("spring", "java")));
		assertFalse(profiles.matches(new MockActiveProfiles("java", "framework")));
	}

	@Test
	public void malformedExpressions() {
		assertMalformed(() -> Profiles.of("("));
		assertMalformed(() -> Profiles.of(")"));
		assertMalformed(() -> Profiles.of("a & b | c"));
	}

	@Test
	public void sensibleToString() {
		assertEquals("spring & framework or java | kotlin",
				Profiles.of("spring & framework", "java | kotlin").toString());
	}

	private void assertMalformed(Supplier<Profiles> supplier) {
		try {
			supplier.get();
			fail("Not malformed");
		}
		catch (IllegalArgumentException ex) {
			assertTrue(ex.getMessage().contains("Malformed"));
		}
	}

	private static class MockActiveProfiles implements Predicate<String> {

		private Set<String> activeProfiles;

		private Set<String> defaultProfiles;


		public MockActiveProfiles(String... activeProfiles) {
			this(Arrays.asList(activeProfiles));
		}

		public MockActiveProfiles(Collection<String> activeProfiles) {
			this(activeProfiles, Collections.singleton("default"));
		}

		public MockActiveProfiles(Collection<String> activeProfiles,
				Collection<String> defaultProfiles) {
			this.activeProfiles = new LinkedHashSet<>(activeProfiles);
			this.defaultProfiles = new LinkedHashSet<>(defaultProfiles);
		}


		@Override
		public boolean test(String profile) {
			if (!StringUtils.hasText(profile) || profile.charAt(0) == '!') {
				throw new IllegalArgumentException("Invalid profile [" + profile + "]");
			}
			return (this.activeProfiles.contains(profile)
					|| (this.activeProfiles.isEmpty() && this.defaultProfiles.contains(profile)));
		}

	}

}
