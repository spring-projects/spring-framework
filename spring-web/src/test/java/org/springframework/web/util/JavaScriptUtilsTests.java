/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link JavaScriptUtils}.
 *
 * @author Rossen Stoyanchev
 */
class JavaScriptUtilsTests {

	@Test
	void escape() {
		StringBuilder sb = new StringBuilder();
		sb.append('"');
		sb.append('\'');
		sb.append('\\');
		sb.append('/');
		sb.append('\t');
		sb.append('\n');
		sb.append('\r');
		sb.append('\f');
		sb.append('\b');
		sb.append('\013');
		assertThat(JavaScriptUtils.javaScriptEscape(sb.toString())).isEqualTo("\\\"\\'\\\\\\/\\t\\n\\n\\f\\b\\v");
	}

	// SPR-9983

	@Test
	void escapePsLsLineTerminators() {
		StringBuilder sb = new StringBuilder();
		sb.append('\u2028');
		sb.append('\u2029');
		String result = JavaScriptUtils.javaScriptEscape(sb.toString());

		assertThat(result).isEqualTo("\\u2028\\u2029");
	}

	// SPR-9983

	@Test
	void escapeLessThanGreaterThanSigns() {
		assertThat(JavaScriptUtils.javaScriptEscape("<>")).isEqualTo("\\u003C\\u003E");
	}

	@Test
	void escapeBacktick() {
		assertThat(JavaScriptUtils.javaScriptEscape("`")).isEqualTo("\\u0060");
	}

	@Test
	void escapeDollar() {
		assertThat(JavaScriptUtils.javaScriptEscape("$")).isEqualTo("\\u0024");
	}

}
