/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.servlet.mvc.condition;

import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.accept.DefaultApiVersionStrategy;
import org.springframework.web.accept.NotAcceptableApiVersionException;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

/**
 * Unit tests for {@link VersionRequestCondition}.
 * @author Rossen Stoyanchev
 */
public class VersionRequestConditionTests {

	private DefaultApiVersionStrategy strategy;


	@BeforeEach
	void setUp() {
		this.strategy = initVersionStrategy(null);
	}

	private static DefaultApiVersionStrategy initVersionStrategy(@Nullable String defaultValue) {
		return new DefaultApiVersionStrategy(
				List.of(request -> request.getParameter("api-version")),
				new SemanticApiVersionParser(), true, defaultValue);
	}

	@Test
	void combineMethodLevelOnly() {
		VersionRequestCondition condition = emptyCondition().combine(condition("1.1"));
		assertThat(condition.getVersion()).isEqualTo("1.1");
	}

	@Test
	void combineTypeLevelOnly() {
		VersionRequestCondition condition = condition("1.1").combine(emptyCondition());
		assertThat(condition.getVersion()).isEqualTo("1.1");
	}

	@Test
	void combineTypeAndMethodLevel() {
		assertThat(condition("1.1").combine(condition("1.2")).getVersion()).isEqualTo("1.2");
	}

	@Test
	void fixedVersionMatch() {
		String conditionVersion = "1.2";
		this.strategy.addSupportedVersion("1.1", "1.3");

		testMatch("v1.1", conditionVersion, true, false);
		testMatch("v1.2", conditionVersion, false, false);
		testMatch("v1.3", conditionVersion, false, true);
	}

	@Test
	void baselineVersionMatch() {
		String conditionVersion = "1.2+";
		this.strategy.addSupportedVersion("1.1", "1.3");

		testMatch("v1.1", conditionVersion, true, false);
		testMatch("v1.2", conditionVersion, false, false);
		testMatch("v1.3", conditionVersion, false, false);
	}

	private void testMatch(
			String requestVersion, String conditionVersion, boolean notCompatible, boolean notAcceptable) {

		MockHttpServletRequest request = requestWithVersion(requestVersion);
		VersionRequestCondition condition = condition(conditionVersion);
		VersionRequestCondition match = condition.getMatchingCondition(request);

		if (notCompatible) {
			assertThat(match).isNull();
			return;
		}

		assertThat(match).isSameAs(condition);

		if (notAcceptable) {
			assertThatThrownBy(() -> condition.handleMatch(request)).isInstanceOf(NotAcceptableApiVersionException.class);
			return;
		}

		condition.handleMatch(request);
	}

	@Test
	void missingRequiredVersion() {
		assertThatThrownBy(() -> condition("1.2").getMatchingCondition(new MockHttpServletRequest("GET", "/path")))
				.hasMessage("400 BAD_REQUEST \"API version is required.\"");
	}

	@Test
	void defaultVersion() {
		String version = "1.2";
		this.strategy = initVersionStrategy(version);
		VersionRequestCondition condition = condition(version);
		VersionRequestCondition match = condition.getMatchingCondition(new MockHttpServletRequest("GET", "/path"));

		assertThat(match).isSameAs(condition);
	}

	@Test
	void unsupportedVersion() {
		assertThatThrownBy(() -> condition("1.2").getMatchingCondition(requestWithVersion("1.3")))
				.hasMessage("400 BAD_REQUEST \"Invalid API version: '1.3.0'.\"");
	}

	@Test
	void compare() {
		testCompare("1.1", "1", "1.1");
		testCompare("1.1.1", "1", "1.1", "1.1.1");
		testCompare("10", "1.1", "10");
		testCompare("10", "2", "10");
	}

	private void testCompare(String expected, String... versions) {
		List<VersionRequestCondition> list = Arrays.stream(versions)
				.map(this::condition)
				.sorted((c1, c2) -> c1.compareTo(c2, new MockHttpServletRequest()))
				.toList();

		assertThat(list.get(0)).isEqualTo(condition(expected));
	}

	private VersionRequestCondition condition(String v) {
		this.strategy.addSupportedVersion(v.endsWith("+") ? v.substring(0, v.length() - 1) : v);
		return new VersionRequestCondition(v, this.strategy);
	}

	private VersionRequestCondition emptyCondition() {
		return new VersionRequestCondition();
	}

	private MockHttpServletRequest requestWithVersion(String v) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path");
		request.addParameter("api-version", v);
		return request;
	}

}
