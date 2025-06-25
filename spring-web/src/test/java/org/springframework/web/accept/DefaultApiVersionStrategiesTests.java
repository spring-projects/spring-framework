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

package org.springframework.web.accept;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DefaultApiVersionStrategy}.
 * @author Rossen Stoyanchev
 */
public class DefaultApiVersionStrategiesTests {

	private final SemanticApiVersionParser parser = new SemanticApiVersionParser();


	@Test
	void defaultVersionIsParsed() {
		SemanticApiVersionParser.Version version = this.parser.parseVersion("1.2.3");
		ApiVersionStrategy strategy = initVersionStrategy(version.toString());

		assertThat(strategy.getDefaultVersion()).isEqualTo(version);
	}

	@Test
	void validateSupportedVersion() {
		SemanticApiVersionParser.Version v12 = this.parser.parseVersion("1.2");

		DefaultApiVersionStrategy strategy = initVersionStrategy(null);
		strategy.addSupportedVersion(v12.toString());

		MockHttpServletRequest request = new MockHttpServletRequest();
		strategy.validateVersion(v12, request);
	}

	@Test
	void validateUnsupportedVersion() {
		assertThatThrownBy(() -> initVersionStrategy(null).validateVersion("1.2", new MockHttpServletRequest()))
				.isInstanceOf(InvalidApiVersionException.class)
				.hasMessage("400 BAD_REQUEST \"Invalid API version: '1.2'.\"");
	}

	@Test
	void missingRequiredVersion() {
		DefaultApiVersionStrategy strategy = initVersionStrategy(null);
		assertThatThrownBy(() -> strategy.validateVersion(null, new MockHttpServletRequest()))
				.isInstanceOf(MissingApiVersionException.class)
				.hasMessage("400 BAD_REQUEST \"API version is required.\"");
	}

	private static DefaultApiVersionStrategy initVersionStrategy(@Nullable String defaultValue) {
		return new DefaultApiVersionStrategy(
				List.of(request -> request.getParameter("api-version")),
				new SemanticApiVersionParser(), true, defaultValue, null);
	}

}
