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

package org.springframework.jms.support;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Stream;

import jakarta.jms.Session;
import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link JmsAccessor}.
 *
 * @author Rick Evans
 * @author Chris Beams
 * @author Vedran Pavic
 * @author Sam Brannen
 */
class JmsAccessorTests {

	/**
	 * No-op stub of the {@link JmsAccessor} class.
	 */
	private final JmsAccessor accessor = new JmsAccessor() {};


	@Test
	void failsIfConnectionFactoryIsNotSupplied() {
		assertThatIllegalArgumentException().isThrownBy(accessor::afterPropertiesSet);
	}

	@Test
	void sessionTransactedModeDefaultsToFalse() {
		String message = """
				The [sessionTransacted] property of JmsAccessor must default to \
				false. Change this test (and the attendant Javadoc) if you have \
				changed the default.""";
		assertThat(accessor.isSessionTransacted()).as(message).isFalse();
	}

	@Test
	void acknowledgeModeDefaultsToAutoAcknowledge() {
		String message = """
				The [sessionAcknowledgeMode] property of JmsAccessor must default to \
				"[Session.AUTO_ACKNOWLEDGE]. Change this test (and the attendant \
				"Javadoc) if you have changed the default.""";
		assertThat(accessor.getSessionAcknowledgeMode()).as(message).isEqualTo(Session.AUTO_ACKNOWLEDGE);
	}

	@Test
	void setSessionAcknowledgeModeNameToUnsupportedValues() {
		assertThatIllegalArgumentException().isThrownBy(() -> accessor.setSessionAcknowledgeModeName(null));
		assertThatIllegalArgumentException().isThrownBy(() -> accessor.setSessionAcknowledgeModeName("   "));
		assertThatIllegalArgumentException().isThrownBy(() -> accessor.setSessionAcknowledgeModeName("bogus"));
	}

	/**
	 * This test effectively verifies that the internal 'constants' map is properly
	 * configured for all acknowledge mode constants constants defined in
	 * {@link jakarta.jms.Session}.
	 */
	@Test
	void setSessionAcknowledgeModeNameToAllSupportedValues() {
		streamAcknowledgeModeConstants()
				.map(Field::getName)
				.forEach(name -> assertThatNoException().isThrownBy(() -> accessor.setSessionAcknowledgeModeName(name)));
	}


	private static Stream<Field> streamAcknowledgeModeConstants() {
		return Arrays.stream(Session.class.getFields())
				.filter(ReflectionUtils::isPublicStaticFinal);
	}

	@Test
	void customAcknowledgeModeIsConsideredClientAcknowledge() throws Exception {
		Session session = mock();
		given(session.getAcknowledgeMode()).willReturn(100);
		assertThat(accessor.isClientAcknowledge(session)).isTrue();
	}

}
