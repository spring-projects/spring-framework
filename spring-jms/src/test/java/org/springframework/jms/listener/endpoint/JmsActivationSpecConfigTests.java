/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jms.listener.endpoint;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.jms.Session;
import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link JmsActivationSpecConfig}.
 *
 * @author Sam Brannen
 * @since 6.1
 */
class JmsActivationSpecConfigTests {

	private final JmsActivationSpecConfig specConfig = new JmsActivationSpecConfig();


	@Test
	void setAcknowledgeModeNameToUnsupportedValues() {
		assertThatIllegalArgumentException().isThrownBy(() -> specConfig.setAcknowledgeModeName(null));
		assertThatIllegalArgumentException().isThrownBy(() -> specConfig.setAcknowledgeModeName("   "));
		assertThatIllegalArgumentException().isThrownBy(() -> specConfig.setAcknowledgeModeName("bogus"));
	}

	/**
	 * This test effectively verifies that the internal 'constants' map is properly
	 * configured for all acknowledge mode constants defined in
	 * {@link jakarta.jms.Session}.
	 */
	@Test
	void setAcknowledgeModeNameToAllSupportedValues() {
		Set<Integer> uniqueValues = new HashSet<>();
		streamAcknowledgeModeConstants().forEach(name -> {
			specConfig.setAcknowledgeModeName(name);
			int acknowledgeMode = specConfig.getAcknowledgeMode();
			assertThat(acknowledgeMode).isBetween(0, 3);
			uniqueValues.add(acknowledgeMode);
		});
		assertThat(uniqueValues).hasSize(4);
	}

	@Test
	void setSessionAcknowledgeMode() {
		assertThatIllegalArgumentException().isThrownBy(() -> specConfig.setAcknowledgeMode(999));

		specConfig.setAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
		assertThat(specConfig.getAcknowledgeMode()).isEqualTo(Session.AUTO_ACKNOWLEDGE);

		specConfig.setAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
		assertThat(specConfig.getAcknowledgeMode()).isEqualTo(Session.CLIENT_ACKNOWLEDGE);

		specConfig.setAcknowledgeMode(Session.DUPS_OK_ACKNOWLEDGE);
		assertThat(specConfig.getAcknowledgeMode()).isEqualTo(Session.DUPS_OK_ACKNOWLEDGE);

		specConfig.setAcknowledgeMode(Session.SESSION_TRANSACTED);
		assertThat(specConfig.getAcknowledgeMode()).isEqualTo(Session.SESSION_TRANSACTED);
	}


	private static Stream<String> streamAcknowledgeModeConstants() {
		return Arrays.stream(Session.class.getFields())
				.filter(ReflectionUtils::isPublicStaticFinal)
				.map(Field::getName);
	}

}
