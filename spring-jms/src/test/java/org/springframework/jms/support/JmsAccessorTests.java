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

package org.springframework.jms.support;

import javax.jms.Session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the {@link JmsAccessor} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public class JmsAccessorTests {

	@Test
	public void testChokesIfConnectionFactoryIsNotSupplied() throws Exception {
		JmsAccessor accessor = new StubJmsAccessor();
		assertThatIllegalArgumentException().isThrownBy(
				accessor::afterPropertiesSet);
	}

	@Test
	public void testSessionTransactedModeReallyDoesDefaultToFalse() throws Exception {
		JmsAccessor accessor = new StubJmsAccessor();
		assertThat(accessor.isSessionTransacted()).as("The [sessionTransacted] property of JmsAccessor must default to " +
				"false. Change this test (and the attendant Javadoc) if you have " +
				"changed the default.").isFalse();
	}

	@Test
	public void testAcknowledgeModeReallyDoesDefaultToAutoAcknowledge() throws Exception {
		JmsAccessor accessor = new StubJmsAccessor();
		assertThat(accessor.getSessionAcknowledgeMode()).as("The [sessionAcknowledgeMode] property of JmsAccessor must default to " +
				"[Session.AUTO_ACKNOWLEDGE]. Change this test (and the attendant " +
				"Javadoc) if you have changed the default.").isEqualTo(Session.AUTO_ACKNOWLEDGE);
	}

	@Test
	public void testSetAcknowledgeModeNameChokesIfBadAckModeIsSupplied() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new StubJmsAccessor().setSessionAcknowledgeModeName("Tally ho chaps!"));
	}


	/**
	 * Crummy, stub, do-nothing subclass of the JmsAccessor class for use in testing.
	 */
	private static final class StubJmsAccessor extends JmsAccessor {
	}

}
