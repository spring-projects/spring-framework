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

package org.springframework.orm.toplink;

import junit.framework.TestCase;
import oracle.toplink.exceptions.ValidationException;
import oracle.toplink.publicinterface.UnitOfWork;
import oracle.toplink.sessionbroker.SessionBroker;
import oracle.toplink.sessions.Session;

/**
 * @author <a href="mailto:james.x.clark@oracle.com">James Clark</a>
 */
public class SessionBrokerFactoryTests extends TestCase {

	/*
	 * When acquiring ClientSessionBrokers, the SessionBroker can throw RuntimeExceptions indicating
	 * that this SessionBroker is not capable of creating "client" Sessions. We need to handle
	 * these differently depending on how the SessionFactory is being used. If we are creating a
	 * plain Session than we can return the original SessionBroker.
	 */
	public void testSessionBrokerThrowingValidationException() {
		SessionBroker broker = new MockSingleSessionBroker();
		SessionBrokerSessionFactory factory = new SessionBrokerSessionFactory(broker);

		assertEquals(factory.createSession(), broker);
		try {
			factory.createManagedClientSession();
			fail("Should have thrown ValidationException");
		}
		catch (ValidationException ex) {
			// expected
		}
	}

    /**
     * Insure that the managed TopLink Session proxy is behaving correctly
     * when it has been initialized with a SessionBroker.  
     */
	public void testManagedSessionBroker() {
		SessionBroker client = new MockClientSessionBroker();
		SessionBroker broker = new MockServerSessionBroker(client);
		SessionBrokerSessionFactory factory = new SessionBrokerSessionFactory(broker);

		assertEquals(client, factory.createSession());

		Session session = factory.createManagedClientSession();
		assertEquals(client, session.getActiveSession());
        assertNotNull(session.getActiveUnitOfWork());
		assertEquals(session.getActiveUnitOfWork(), session.getActiveUnitOfWork());
	}


	private class MockSingleSessionBroker extends SessionBroker {

		public MockSingleSessionBroker() {
		}

		public SessionBroker acquireClientSessionBroker() {
			throw new ValidationException();
		}
	}


	private class MockServerSessionBroker extends SessionBroker {

		private SessionBroker client;

		public MockServerSessionBroker(SessionBroker client) {
			this.client = client;
		}

		public SessionBroker acquireClientSessionBroker() {
			return client;
		}
	}


	private class MockClientSessionBroker extends SessionBroker {

		public MockClientSessionBroker() {
		}

		public UnitOfWork acquireUnitOfWork() {
			return new UnitOfWork(this);
		}
	}
}
