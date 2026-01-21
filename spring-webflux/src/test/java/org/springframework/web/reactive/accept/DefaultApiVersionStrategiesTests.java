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

package org.springframework.web.reactive.accept;

import java.util.List;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.accept.MissingApiVersionException;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link org.springframework.web.accept.DefaultApiVersionStrategy}.
 * @author Rossen Stoyanchev
 * @author Jonathan Kaplan
 */
public class DefaultApiVersionStrategiesTests {

	private static final SemanticApiVersionParser parser = new SemanticApiVersionParser();


	@Test
	void defaultVersionIsParsed() {
		String version = "1.2.3";
		ApiVersionStrategy strategy = apiVersionStrategy(version, false, null);
		assertThat(strategy.getDefaultVersion()).isEqualTo(parser.parseVersion(version));
	}

	@Test
	void missingRequiredVersion() {
		StepVerifier.create(testValidate(null, apiVersionStrategy()))
				.expectErrorSatisfies(ex -> assertThat(ex)
						.isInstanceOf(MissingApiVersionException.class)
						.hasMessage(("400 BAD_REQUEST \"API version is required.\"")))
				.verify();
	}

	@Test
	void validateSupportedVersion() {
		String version = "1.2";
		DefaultApiVersionStrategy strategy = apiVersionStrategy();
		strategy.addSupportedVersion(version);
		Mono<String> result = testValidate(version, strategy);
		StepVerifier.create(result).expectNext("1.2.0").verifyComplete();
	}

	@Test
	void validateSupportedVersionForDefaultVersion() {
		String defaultVersion = "1.2";
		DefaultApiVersionStrategy strategy = apiVersionStrategy(defaultVersion, false, null);
		Mono<String> result = testValidate(defaultVersion, strategy);
		StepVerifier.create(result).expectNext("1.2.0").verifyComplete();
	}

	@Test
	void validateUnsupportedVersion() {
		StepVerifier.create(testValidate("1.2", apiVersionStrategy()))
				.expectErrorSatisfies(ex -> assertThat(ex)
						.isInstanceOf(InvalidApiVersionException.class)
						.hasMessage(("400 BAD_REQUEST \"Invalid API version: '1.2.0'.\"")))
				.verify();
	}

	@Test
	void validateDetectedVersion() {
		String version = "1.2";
		DefaultApiVersionStrategy strategy = apiVersionStrategy(null, true, null);
		strategy.addMappedVersion(version);
		Mono<String> result = testValidate(version, strategy);
		StepVerifier.create(result).expectNext("1.2.0").verifyComplete();
	}

	@Test
	void validateWhenDetectedVersionOff() {
		String version = "1.2";
		DefaultApiVersionStrategy strategy = apiVersionStrategy();
		strategy.addMappedVersion(version);
		Mono<String> result = testValidate(version, strategy);
		StepVerifier.create(result).expectError(InvalidApiVersionException.class).verify();
	}

	@Test
	void validateSupportedWithPredicate() {
		SemanticApiVersionParser.Version parsedVersion = parser.parseVersion("1.2");
		ApiVersionStrategy strategy = apiVersionStrategy(null, false, version -> version.equals(parsedVersion));
		Mono<String> result = testValidate("1.2", strategy);
		StepVerifier.create(result).expectNext("1.2.0").verifyComplete();
	}

	@Test
	void validateUnsupportedWithPredicate() {
		ApiVersionStrategy strategy = apiVersionStrategy(null, false, version -> version.equals("1.2"));
		Mono<?> result = testValidate("1.2", strategy);
		StepVerifier.create(result).verifyError(InvalidApiVersionException.class);
	}

	@Test
	void versionRequiredAndDefaultVersionSet() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> {
					ApiVersionResolver resolver = new QueryApiVersionResolver("api-version");
					new DefaultApiVersionStrategy(List.of(resolver), parser, true, "1.2", true, v -> true, null);
				})
				.withMessage("versionRequired cannot be set to true if a defaultVersion is also configured");
	}

	private static DefaultApiVersionStrategy apiVersionStrategy() {
		return apiVersionStrategy(null, false, null);
	}

	private static DefaultApiVersionStrategy apiVersionStrategy(
			@Nullable String defaultVersion, boolean detectSupportedVersions,
			@Nullable Predicate<Comparable<?>> supportedVersionPredicate) {

			return new DefaultApiVersionStrategy(
				List.of(new QueryApiVersionResolver("api-version")),
				parser, null, defaultVersion, detectSupportedVersions, supportedVersionPredicate, null);
	}

	private Mono<String> testValidate(@Nullable String version, ApiVersionStrategy strategy) {
		MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get("/");
		if (version != null) {
			builder.queryParam("api-version", version);
		}
		MockServerWebExchange exchange = MockServerWebExchange.builder(builder).build();
		return strategy.resolveParseAndValidateApiVersion(exchange).map(Object::toString);
	}

}
