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

package org.springframework.web.servlet.view;

import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.handler.PathPatternsParameterizedTest;
import org.springframework.web.servlet.handler.PathPatternsTestUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 */
class DefaultRequestToViewNameTranslatorTests {

	private static final String VIEW_NAME = "apple";
	private static final String EXTENSION = ".html";

	private final DefaultRequestToViewNameTranslator translator = new DefaultRequestToViewNameTranslator();


	@SuppressWarnings("unused")
	private static Stream<Named<Function<String, MockHttpServletRequest>>> pathPatternsArguments() {
		return PathPatternsTestUtils.requestArguments("/sundays");
	}


	@PathPatternsParameterizedTest
	void getViewNameLeavesLeadingSlashIfSoConfigured(Function<String, MockHttpServletRequest> requestFactory) {
		MockHttpServletRequest request = requestFactory.apply(VIEW_NAME + "/");
		this.translator.setStripLeadingSlash(false);
		assertViewName(request, "/" + VIEW_NAME);
	}

	@PathPatternsParameterizedTest
	void getViewNameLeavesTrailingSlashIfSoConfigured(Function<String, MockHttpServletRequest> requestFactory) {
		MockHttpServletRequest request = requestFactory.apply(VIEW_NAME + "/");
		this.translator.setStripTrailingSlash(false);
		assertViewName(request, VIEW_NAME + "/");
	}

	@PathPatternsParameterizedTest
	void getViewNameLeavesExtensionIfSoConfigured(Function<String, MockHttpServletRequest> requestFactory) {
		MockHttpServletRequest request = requestFactory.apply(VIEW_NAME + EXTENSION);
		this.translator.setStripExtension(false);
		assertViewName(request, VIEW_NAME + EXTENSION);
	}

	@PathPatternsParameterizedTest
	void getViewNameWithDefaultConfiguration(Function<String, MockHttpServletRequest> requestFactory) {
		MockHttpServletRequest request = requestFactory.apply(VIEW_NAME + EXTENSION);
		assertViewName(request, VIEW_NAME);
	}

	@PathPatternsParameterizedTest
	void getViewNameWithCustomSeparator(Function<String, MockHttpServletRequest> requestFactory) {
		MockHttpServletRequest request = requestFactory.apply(VIEW_NAME + "/fiona" + EXTENSION);
		this.translator.setSeparator("_");
		assertViewName(request, VIEW_NAME + "_fiona");
	}

	@PathPatternsParameterizedTest
	void getViewNameWithNoExtension(Function<String, MockHttpServletRequest> requestFactory) {
		MockHttpServletRequest request = requestFactory.apply(VIEW_NAME);
		assertViewName(request, VIEW_NAME);
	}

	@PathPatternsParameterizedTest
	void getViewNameWithSemicolonContent(Function<String, MockHttpServletRequest> requestFactory) {
		MockHttpServletRequest request = requestFactory.apply(VIEW_NAME + ";a=A;b=B");
		assertViewName(request, VIEW_NAME);
	}

	@PathPatternsParameterizedTest
	void getViewNameWithPrefix(Function<String, MockHttpServletRequest> requestFactory) {
		final String prefix = "fiona_";
		MockHttpServletRequest request = requestFactory.apply(VIEW_NAME);
		this.translator.setPrefix(prefix);
		assertViewName(request, prefix + VIEW_NAME);
	}

	@PathPatternsParameterizedTest
	void getViewNameWithNullPrefix(Function<String, MockHttpServletRequest> requestFactory) {
		MockHttpServletRequest request = requestFactory.apply(VIEW_NAME);
		this.translator.setPrefix(null);
		assertViewName(request, VIEW_NAME);
	}

	@PathPatternsParameterizedTest
	void getViewNameWithSuffix(Function<String, MockHttpServletRequest> requestFactory) {
		final String suffix = ".fiona";
		MockHttpServletRequest request = requestFactory.apply(VIEW_NAME);
		this.translator.setSuffix(suffix);
		assertViewName(request, VIEW_NAME + suffix);
	}

	@PathPatternsParameterizedTest
	void getViewNameWithNullSuffix(Function<String, MockHttpServletRequest> requestFactory) {
		MockHttpServletRequest request = requestFactory.apply(VIEW_NAME);
		this.translator.setSuffix(null);
		assertViewName(request, VIEW_NAME);
	}

	@PathPatternsParameterizedTest
	void getViewNameWithRedirectPrefixFails(Function<String, MockHttpServletRequest> requestFactory) {
		MockHttpServletRequest request = requestFactory.apply(UrlBasedViewResolver.REDIRECT_URL_PREFIX + VIEW_NAME);
		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> this.translator.getViewName(request))
				.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
	}

	@PathPatternsParameterizedTest
	void getViewNameWithForwardPrefixFails(Function<String, MockHttpServletRequest> requestFactory) {
		MockHttpServletRequest request = requestFactory.apply(UrlBasedViewResolver.FORWARD_URL_PREFIX + VIEW_NAME);
		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> this.translator.getViewName(request))
				.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
	}


	private void assertViewName(MockHttpServletRequest request, String expectedViewName) {
		String actualViewName = this.translator.getViewName(request);
		assertThat(actualViewName).isNotNull();
		assertThat(actualViewName)
				.as("Did not get the expected viewName from the DefaultRequestToViewNameTranslator.getViewName(..)")
				.isEqualTo(expectedViewName);
	}

}
