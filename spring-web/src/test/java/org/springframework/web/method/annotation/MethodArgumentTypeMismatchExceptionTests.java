/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.method.annotation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MethodArgumentTypeMismatchExceptionTests {

	@Test //gh-33573
	void messageIncludesParameterName() {
		@SuppressWarnings("DataFlowIssue")
		MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
				"mismatched value", Integer.class, "paramOne", null, null);

		assertThat(ex).hasMessage("Method parameter 'paramOne': Failed to convert value of type " +
				"'java.lang.String' to required type 'java.lang.Integer'");
	}

}
