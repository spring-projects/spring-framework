/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Test fixture for {@link TextMessage}.
 *
 * @author Shinobu Aoki
 * @author Juergen Hoeller
 */
class TextMessageTests {

	@Test
	void toStringWithAscii() {
		String expected = "foo,bar";
		TextMessage actual = new TextMessage(expected);
		assertThat(actual.getPayload()).isEqualTo(expected);
		assertThat(actual.toString()).contains(expected);
	}

	@Test
	void toStringWithMultibyteString() {
		String expected = "\u3042\u3044\u3046\u3048\u304a";
		TextMessage actual = new TextMessage(expected);
		assertThat(actual.getPayload()).isEqualTo(expected);
		assertThat(actual.toString()).contains(expected);
	}

}
