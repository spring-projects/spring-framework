/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.basic;

import org.junit.jupiter.api.Nested;

import org.springframework.test.context.aot.samples.common.DefaultMessageService;
import org.springframework.test.context.aot.samples.common.MessageService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 6.0
 */
class BasicJupiterTests {

	private final MessageService messageService = new DefaultMessageService();

	@org.junit.jupiter.api.Test
	void test() {
		assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
	}

	@Nested
	class NestedTests {

		@org.junit.jupiter.api.Test
		void test() {
			assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
		}

	}

}
