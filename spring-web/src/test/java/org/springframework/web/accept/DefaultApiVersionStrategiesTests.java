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

package org.springframework.web.accept;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

/**
 * Unit tests for {@link DefaultApiVersionStrategy}.
 * @author Rossen Stoyanchev
 */
public class DefaultApiVersionStrategiesTests {

	private final SemanticApiVersionParser parser = new SemanticApiVersionParser();


	@Test
	void defaultVersion() {
		SemanticApiVersionParser.Version version = this.parser.parseVersion("1.2.3");
		ApiVersionStrategy strategy = initVersionStrategy(version.toString());

		assertThat(strategy.getDefaultVersion()).isEqualTo(version);
	}

	@Test
	void supportedVersions() {
		SemanticApiVersionParser.Version v1 = this.parser.parseVersion("1");
		SemanticApiVersionParser.Version v2 = this.parser.parseVersion("2");
		SemanticApiVersionParser.Version v9 = this.parser.parseVersion("9");

		DefaultApiVersionStrategy strategy = initVersionStrategy(null);
		strategy.addSupportedVersion(v1.toString());
		strategy.addSupportedVersion(v2.toString());

		MockHttpServletRequest request = new MockHttpServletRequest();
		strategy.validateVersion(v1, request);
		strategy.validateVersion(v2, request);

		assertThatThrownBy(() -> strategy.validateVersion(v9, request))
				.isInstanceOf(InvalidApiVersionException.class);
	}

	private static DefaultApiVersionStrategy initVersionStrategy(@Nullable String defaultValue) {
		return new DefaultApiVersionStrategy(
				List.of(request -> request.getParameter("api-version")),
				new SemanticApiVersionParser(), true, defaultValue);
	}

}
