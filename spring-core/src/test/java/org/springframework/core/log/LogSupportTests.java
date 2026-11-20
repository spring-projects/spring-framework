/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.log;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @since 5.2
 */
class LogSupportTests {

	@Test
	void logMessageWithSupplier() {
		LogMessage msg = LogMessage.of(() -> new StringBuilder("a").append(" b"));
		assertThat(msg.toString()).isEqualTo("a b");
		assertThat(msg.toString()).isSameAs(msg.toString());
	}

	@Test
	void logMessageWithFormat1() {
		LogMessage msg = LogMessage.format("a %s", "b");
		assertThat(msg.toString()).isEqualTo("a b");
		assertThat(msg.toString()).isSameAs(msg.toString());
	}

	@Test
	void logMessageWithFormat2() {
		LogMessage msg = LogMessage.format("a %s %s", "b", "c");
		assertThat(msg.toString()).isEqualTo("a b c");
		assertThat(msg.toString()).isSameAs(msg.toString());
	}

	@Test
	void logMessageWithFormat3() {
		LogMessage msg = LogMessage.format("a %s %s %s", "b", "c", "d");
		assertThat(msg.toString()).isEqualTo("a b c d");
		assertThat(msg.toString()).isSameAs(msg.toString());
	}

	@Test
	void logMessageWithFormat4() {
		LogMessage msg = LogMessage.format("a %s %s %s %s", "b", "c", "d", "e");
		assertThat(msg.toString()).isEqualTo("a b c d e");
		assertThat(msg.toString()).isSameAs(msg.toString());
	}

	@Test
	void logMessageWithFormatX() {
		LogMessage msg = LogMessage.format("a %s %s %s %s %s", "b", "c", "d", "e", "f");
		assertThat(msg.toString()).isEqualTo("a b c d e f");
		assertThat(msg.toString()).isSameAs(msg.toString());
	}

}
