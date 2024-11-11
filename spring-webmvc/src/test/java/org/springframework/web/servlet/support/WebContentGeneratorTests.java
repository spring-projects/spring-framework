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

package org.springframework.web.servlet.support;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebContentGenerator}.
 * @author Rossen Stoyanchev
 */
class WebContentGeneratorTests {

	@Test
	void getAllowHeaderWithConstructorTrue() {
		WebContentGenerator generator = new TestWebContentGenerator(true);
		assertThat(generator.getAllowHeader()).isEqualTo("GET,HEAD,POST,OPTIONS");
	}

	@Test
	void getAllowHeaderWithConstructorFalse() {
		WebContentGenerator generator = new TestWebContentGenerator(false);
		assertThat(generator.getAllowHeader()).isEqualTo("GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS");
	}

	@Test
	void getAllowHeaderWithSupportedMethodsConstructor() {
		WebContentGenerator generator = new TestWebContentGenerator("POST");
		assertThat(generator.getAllowHeader()).isEqualTo("POST,OPTIONS");
	}

	@Test
	void getAllowHeaderWithSupportedMethodsSetter() {
		WebContentGenerator generator = new TestWebContentGenerator();
		generator.setSupportedMethods("POST");
		assertThat(generator.getAllowHeader()).isEqualTo("POST,OPTIONS");
	}

	@Test
	void getAllowHeaderWithSupportedMethodsSetterEmpty() {
		WebContentGenerator generator = new TestWebContentGenerator();
		generator.setSupportedMethods();
		assertThat(generator.getAllowHeader()).as("Effectively \"no restriction\" on supported methods").isEqualTo("GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS");
	}

	@Test
	void varyHeaderNone() {
		WebContentGenerator generator = new TestWebContentGenerator();
		MockHttpServletResponse response = new MockHttpServletResponse();
		generator.prepareResponse(response);

		assertThat(response.getHeader("Vary")).isNull();
	}

	@Test
	void varyHeader() {
		String[] configuredValues = {"Accept-Language", "User-Agent"};
		String[] responseValues = {};
		String[] expected = {"Accept-Language", "User-Agent"};
		testVaryHeader(configuredValues, responseValues, expected);
	}

	@Test
	void varyHeaderWithExistingWildcard() {
		String[] configuredValues = {"Accept-Language"};
		String[] responseValues = {"*"};
		String[] expected = {"*"};
		testVaryHeader(configuredValues, responseValues, expected);
	}

	@Test
	void varyHeaderWithExistingCommaValues() {
		String[] configuredValues = {"Accept-Language", "User-Agent"};
		String[] responseValues = {"Accept-Encoding", "Accept-Language"};
		String[] expected = {"Accept-Encoding", "Accept-Language", "User-Agent"};
		testVaryHeader(configuredValues, responseValues, expected);
	}

	@Test
	void varyHeaderWithExistingCommaSeparatedValues() {
		String[] configuredValues = {"Accept-Language", "User-Agent"};
		String[] responseValues = {"Accept-Encoding, Accept-Language"};
		String[] expected = {"Accept-Encoding, Accept-Language", "User-Agent"};
		testVaryHeader(configuredValues, responseValues, expected);
	}

	private void testVaryHeader(String[] configuredValues, String[] responseValues, String[] expected) {
		WebContentGenerator generator = new TestWebContentGenerator();
		generator.setVaryByRequestHeaders(configuredValues);
		MockHttpServletResponse response = new MockHttpServletResponse();
		for (String value : responseValues) {
			response.addHeader("Vary", value);
		}
		generator.prepareResponse(response);
		assertThat(response.getHeaderValues("Vary")).isEqualTo(Arrays.asList(expected));
	}


	private static class TestWebContentGenerator extends WebContentGenerator {

		public TestWebContentGenerator() {
		}

		public TestWebContentGenerator(boolean restrictDefaultSupportedMethods) {
			super(restrictDefaultSupportedMethods);
		}

		public TestWebContentGenerator(String... supportedMethods) {
			super(supportedMethods);
		}
	}
}
