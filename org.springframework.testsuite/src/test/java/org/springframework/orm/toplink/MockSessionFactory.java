
package org.springframework.orm.toplink;

import oracle.toplink.sessions.Session;

/**
 * @author Juergen Hoeller
 * @since 28.04.2005
 */
public class MockSessionFactory implements SessionFactory {

	private Session session;

	public MockSessionFactory(Session session) {
		this.session = session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public Session createSession() {
		return this.session;
	}

	public Session createManagedClientSession() {
		return this.session;
	}

	public Session createTransactionAwareSession() {
		return this.session;
	}

	public void close() {
	}

}
