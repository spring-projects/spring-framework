/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jms.support;

import static org.junit.Assert.*;

import javax.jms.Session;

import org.junit.Test;

/**
 * Unit tests for the {@link JmsAccessor} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public final class JmsAccessorTests {

	@Test(expected=IllegalArgumentException.class)
	public void testChokesIfConnectionFactoryIsNotSupplied() throws Exception {
		JmsAccessor accessor = new StubJmsAccessor();
		accessor.afterPropertiesSet();
	}

	@Test
	public void testSessionTransactedModeReallyDoesDefaultToFalse() throws Exception {
		JmsAccessor accessor = new StubJmsAccessor();
		assertFalse("The [sessionTransacted] property of JmsAccessor must default to " +
				"false. Change this test (and the attendant Javadoc) if you have " +
				"changed the default.",
				accessor.isSessionTransacted());
	}

	@Test
	public void testAcknowledgeModeReallyDoesDefaultToAutoAcknowledge() throws Exception {
		JmsAccessor accessor = new StubJmsAccessor();
		assertEquals("The [sessionAcknowledgeMode] property of JmsAccessor must default to " +
				"[Session.AUTO_ACKNOWLEDGE]. Change this test (and the attendant " +
				"Javadoc) if you have changed the default.",
				Session.AUTO_ACKNOWLEDGE,
				accessor.getSessionAcknowledgeMode());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetAcknowledgeModeNameChokesIfBadAckModeIsSupplied() throws Exception {
		new StubJmsAccessor().setSessionAcknowledgeModeName("Tally ho chaps!");
	}


	/**
	 * Crummy, stub, do-nothing subclass of the JmsAccessor class for use in testing.
	 */
	private static final class StubJmsAccessor extends JmsAccessor {
	}

}
