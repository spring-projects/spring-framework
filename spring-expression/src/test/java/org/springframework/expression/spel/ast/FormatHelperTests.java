/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.ast;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.convert.TypeDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andy Wilkinson
 */
class FormatHelperTests {

	@Test
	void formatMethodWithSingleArgumentForMessage() {
		String message = FormatHelper.formatMethodForMessage("foo", List.of(TypeDescriptor.forObject("a string")));
		assertThat(message).isEqualTo("foo(java.lang.String)");
	}

	@Test
	void formatMethodWithMultipleArgumentsForMessage() {
		String message = FormatHelper.formatMethodForMessage("foo", Arrays.asList(TypeDescriptor.forObject("a string"), TypeDescriptor.forObject(5)));
		assertThat(message).isEqualTo("foo(java.lang.String,java.lang.Integer)");
	}

}
