/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.mail.javamail;

import java.time.Instant;
import java.util.Date;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link MimeMessageHelper}.
 *
 * @author Stephane Nicoll
 */
class MimeMessageHelperTests {

	@Test
	void setSentDateWithInstant() throws MessagingException {
		MimeMessage mimeMessage = new MimeMessage((Session) null);
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
		Instant instant = Instant.parse("2024-01-02T03:04:05Z");
		helper.setSentDate(instant);
		assertThat(mimeMessage.getSentDate()).isEqualTo(Date.from(instant));
	}

	@Test
	void setSentDateWithInstantRejectsNull() throws MessagingException {
		MimeMessage mimeMessage = new MimeMessage((Session) null);
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
		assertThatIllegalArgumentException().isThrownBy(() -> helper.setSentDate((Instant) null));
	}

}
