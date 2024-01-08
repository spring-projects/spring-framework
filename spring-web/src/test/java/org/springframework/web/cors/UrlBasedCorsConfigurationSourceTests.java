/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.cors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Named.named;

/**
 * Tests for {@link UrlBasedCorsConfigurationSource}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
class UrlBasedCorsConfigurationSourceTests {

	private final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();


	@PathPatternsParameterizedTest
	void empty(Function<String, MockHttpServletRequest> requestFactory) {
		assertThat(source.getCorsConfiguration(requestFactory.apply("/bar/test.html"))).isNull();
	}

	@PathPatternsParameterizedTest
	void registerAndMatch(Function<String, MockHttpServletRequest> requestFactory) {
		CorsConfiguration config = new CorsConfiguration();
		source.registerCorsConfiguration("/bar/**", config);

		MockHttpServletRequest request = requestFactory.apply("/foo/test.html");
		assertThat(source.getCorsConfiguration(request)).isNull();

		request = requestFactory.apply("/bar/test.html");
		assertThat(source.getCorsConfiguration(request)).isEqualTo(config);
	}

	@Test
	void unmodifiableConfigurationsMap() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> source.getCorsConfigurations().put("/**", new CorsConfiguration()));
	}

	@Test
	void allowInitLookupPath() {
		CorsConfiguration config = new CorsConfiguration();
		source.registerCorsConfiguration("/**", config);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		assertThat(source.getCorsConfiguration(request))
				.as("The path should be resolved lazily by default")
				.isSameAs(config);

		source.setAllowInitLookupPath(false);
		assertThatIllegalArgumentException().isThrownBy(() -> source.getCorsConfiguration(request));
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("pathPatternsArguments")
	private @interface PathPatternsParameterizedTest {
	}

	@SuppressWarnings("unused")
	private static Stream<Named<Function<String, MockHttpServletRequest>>> pathPatternsArguments() {
		return Stream.of(
				named("ServletRequestPathUtils", requestUri -> {
					MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
					ServletRequestPathUtils.parseAndCache(request);
					return request;
				}),
				named("UrlPathHelper", requestUri -> {
					MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
					UrlPathHelper.defaultInstance.getLookupPathForRequest(request);
					return request;
				})
		);
	}

}
