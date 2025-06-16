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

package org.springframework.web.reactive.accept;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link org.springframework.web.accept.DefaultApiVersionStrategy}.
 * @author Rossen Stoyanchev
 */
public class DefaultApiVersionStrategiesTests {

	private static final SemanticApiVersionParser parser = new SemanticApiVersionParser();

	private static final ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));


	@Test
	void defaultVersionIsParsed() {
		String version = "1.2.3";
		ApiVersionStrategy strategy = apiVersionStrategy(version, false);
		assertThat(strategy.getDefaultVersion()).isEqualTo(parser.parseVersion(version));
	}

	@Test
	void validateSupportedVersion() {
		String version = "1.2";
		DefaultApiVersionStrategy strategy = apiVersionStrategy();
		strategy.addSupportedVersion(version);
		validateVersion(version, strategy);
	}

	@Test
	void validateUnsupportedVersion() {
		assertThatThrownBy(() -> validateVersion("1.2", apiVersionStrategy()))
				.isInstanceOf(InvalidApiVersionException.class);
	}

	@Test
	void validateDetectedSupportedVersion() {
		String version = "1.2";
		DefaultApiVersionStrategy strategy = apiVersionStrategy(null, true);
		strategy.addMappedVersion(version);
		validateVersion(version, strategy);
	}

	@Test
	void validateWhenDetectSupportedVersionsIsOff() {
		String version = "1.2";
		DefaultApiVersionStrategy strategy = apiVersionStrategy();
		strategy.addMappedVersion(version);

		assertThatThrownBy(() -> strategy.validateVersion(version, exchange))
				.isInstanceOf(InvalidApiVersionException.class);
	}

	private static DefaultApiVersionStrategy apiVersionStrategy() {
		return apiVersionStrategy(null, false);
	}

	private static DefaultApiVersionStrategy apiVersionStrategy(
			@Nullable String defaultValue, boolean detectSupportedVersions) {

			return new DefaultApiVersionStrategy(
				List.of(exchange -> exchange.getRequest().getQueryParams().getFirst("api-version")),
				parser, true, defaultValue, detectSupportedVersions);
	}

	private static void validateVersion(String version, DefaultApiVersionStrategy strategy) {
		strategy.validateVersion(parser.parseVersion(version), exchange);
	}

}
