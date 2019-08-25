/*
 * Copyright 2002-2019 the original author or authors.
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
