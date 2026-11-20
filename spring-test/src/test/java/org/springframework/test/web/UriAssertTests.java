/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
