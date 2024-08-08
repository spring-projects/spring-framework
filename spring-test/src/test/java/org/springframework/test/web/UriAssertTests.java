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

package org.springframework.test.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link UriAssert}.
 *
 * @author Stephane Nicoll
 */
class UriAssertTests {

	@Test
	void isEqualToTemplate() {
		assertThat("/orders/1/items/2").isEqualToTemplate("/orders/{orderId}/items/{itemId}", 1, 2);
	}

	@Test
	void isEqualToTemplateWithWrongValue() {
		String expected = "/orders/1/items/3";
		String actual = "/orders/1/items/2";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(expected).isEqualToTemplate("/orders/{orderId}/items/{itemId}", 1, 2))
				.withMessageContainingAll("Test URI", expected, actual);
	}

	@Test
	void isEqualToTemplateMissingArg() {
		String template = "/orders/{orderId}/items/{itemId}";
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat("/orders/1/items/2").isEqualToTemplate(template, 1))
				.withMessageContainingAll("Expecting:", template,
						"Not enough variable values available to expand 'itemId'");
	}

	@Test
	void matchesAntPattern() {
		assertThat("/orders/1").matchesAntPattern("/orders/*");
	}

	@Test
	void matchesAntPatternWithNonValidPattern() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat("/orders/1").matchesAntPattern("/orders/"))
				.withMessage("'/orders/' is not an Ant-style path pattern");
	}

	@Test
	void matchesAntPatternWithWrongValue() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat("/orders/1").matchesAntPattern("/resources/*"))
				.withMessageContainingAll("Test URI", "/resources/*", "/orders/1");
	}


	UriAssert assertThat(String uri) {
		return new UriAssert(uri, "Test URI");
	}

}
