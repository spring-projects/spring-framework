/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Gary Russell
 * @since 5.0
 */
class ErrorMessageTests {

	@Test
	void toStringOutput() {
		ErrorMessage em = new ErrorMessage(new RuntimeException("foo"));
		String emString = em.toString();
		assertThat(emString).doesNotContain("original");

		em = new ErrorMessage(new RuntimeException("foo"), new GenericMessage<>("bar"));
		emString = em.toString();
		assertThat(emString).contains("original");
		assertThat(emString).contains(em.getOriginalMessage().toString());
	}

}
