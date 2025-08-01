/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.result.condition;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.accept.NotAcceptableApiVersionException;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.DefaultApiVersionStrategy;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

	private static DefaultApiVersionStrategy initVersionStrategy(@Nullable String defaultVersion) {
		return new DefaultApiVersionStrategy(
				List.of(exchange -> exchange.getRequest().getQueryParams().getFirst("api-version")),
				new SemanticApiVersionParser(), true, defaultVersion, false, null, null);
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
		VersionRequestCondition condition = condition("1.2");
		this.strategy.addSupportedVersion("1.1", "1.3");

		testMatch("v1.1", condition, false, false);
		testMatch("v1.2", condition, true, false);
		testMatch("v1.3", condition, true, true); // match initially, reject if chosen
	}

	@Test
	void baselineVersionMatch() {
		VersionRequestCondition condition = condition("1.2+");
		this.strategy.addSupportedVersion("1.1", "1.3");

		testMatch("v1.1", condition, false, false);
		testMatch("v1.2", condition, true, false);
		testMatch("v1.3", condition, true, false);
	}

	@Test
	void notVersionedMatch() {
		VersionRequestCondition condition = new VersionRequestCondition(null, this.strategy);
		this.strategy.addSupportedVersion("1.1", "1.3");

		testMatch("v1.1", condition, true, false);
		testMatch("v1.3", condition, true, false);
	}

	private void testMatch(
			String requestVersion, VersionRequestCondition condition, boolean matches, boolean notAcceptable) {

		ServerWebExchange exchange = exchangeWithVersion(requestVersion);
		VersionRequestCondition match = condition.getMatchingCondition(exchange);

		if (!matches) {
			assertThat(match).isNull();
			return;
		}

		assertThat(match).isSameAs(condition);

		if (notAcceptable) {
			assertThatThrownBy(() -> condition.handleMatch(exchange)).isInstanceOf(NotAcceptableApiVersionException.class);
			return;
		}

		condition.handleMatch(exchange);
	}

	@Test
	void defaultVersion() {
		String version = "1.2";
		this.strategy = initVersionStrategy(version);
		VersionRequestCondition condition = condition(version);
		VersionRequestCondition match = condition.getMatchingCondition(exchange());

		assertThat(match).isSameAs(condition);
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
				.sorted((c1, c2) -> c1.compareTo(c2, exchange()))
				.toList();

		assertThat(list.get(0)).isEqualTo(condition(expected));
	}

	@Test // gh-35236
	void noRequestVersion() {
		MockServerWebExchange exchange = exchange();
		VersionRequestCondition condition = condition("1.1");

		VersionRequestCondition match = condition.getMatchingCondition(exchange);
		assertThat(match).isSameAs(condition);

		condition.handleMatch(exchange);
	}

	@Test
	void compareWithoutRequestVersion() {
		VersionRequestCondition condition = Stream.of(condition("1.1"), condition("1.2"), emptyCondition())
				.min((c1, c2) -> c1.compareTo(c2, exchange()))
				.get();

		assertThat(condition).isEqualTo(emptyCondition());
	}

	private VersionRequestCondition condition(String v) {
		this.strategy.addSupportedVersion(v.endsWith("+") ? v.substring(0, v.length() - 1) : v);
		return new VersionRequestCondition(v, this.strategy);
	}

	private VersionRequestCondition emptyCondition() {
		return new VersionRequestCondition(null, this.strategy);
	}

	private static MockServerWebExchange exchange() {
		return MockServerWebExchange.from(MockServerHttpRequest.get("/path"));
	}

	private ServerWebExchange exchangeWithVersion(String v) {
		Comparable<?> version = this.strategy.parseVersion(v);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path"));
		exchange.getAttributes().put(HandlerMapping.API_VERSION_ATTRIBUTE, version);
		return exchange;
	}

}
