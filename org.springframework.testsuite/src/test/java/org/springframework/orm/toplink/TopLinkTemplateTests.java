/*
 * Created on Mar 20, 2005
 *
 */

package org.springframework.orm.toplink;

import junit.framework.TestCase;
import oracle.toplink.exceptions.TopLinkException;
import oracle.toplink.sessions.Session;
import org.easymock.MockControl;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Juergen Hoeller
 * @author <a href="mailto:james.x.clark@oracle.com">James Clark</a>
 * @since 28.04.2005
 */
public class TopLinkTemplateTests extends TestCase {

	public void testTemplateNotAllowingCreate() {
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();

		SessionFactory factory = new SingleSessionFactory(session);

		TopLinkTemplate template = new TopLinkTemplate();
		template.setAllowCreate(false);
		template.setSessionFactory(factory);
		try {
			template.execute(new TopLinkCallback() {
				public Object doInTopLink(Session session) throws TopLinkException {
					return null;
				}
			});
			fail();
		}
		catch (Exception e) {
		}
	}

	public void testTemplateWithCreate() {
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();

		SessionFactory factory = new SingleSessionFactory(session);

		session.release();
		sessionControl.setVoidCallable(1);

		sessionControl.replay();

		TopLinkTemplate template = new TopLinkTemplate();
		template.setAllowCreate(true);
		template.setSessionFactory(factory);
		template.execute(new TopLinkCallback() {
			public Object doInTopLink(Session session) throws TopLinkException {
				assertTrue(session != null);
				return null;
			}
		});
		assertFalse(TransactionSynchronizationManager.hasResource(factory));

		sessionControl.verify();
	}

	public void testTemplateWithExistingSessionAndNoCreate() {
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();

		SessionFactory factory = new SingleSessionFactory(session);

		sessionControl.replay();

		SessionHolder sessionHolder = new SessionHolder(factory.createSession());
		TransactionSynchronizationManager.bindResource(factory, sessionHolder);

		TopLinkTemplate template = new TopLinkTemplate();
		template.setAllowCreate(false);
		template.setSessionFactory(factory);
		template.execute(new TopLinkCallback() {
			public Object doInTopLink(Session session) throws TopLinkException {
				assertTrue(session != null);
				return null;
			}
		});
		assertTrue(TransactionSynchronizationManager.hasResource(factory));
		sessionControl.verify();
		TransactionSynchronizationManager.unbindResource(factory);
	}

	public void testTemplateWithExistingSessionAndCreateAllowed() {
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();

		SessionFactory factory = new SingleSessionFactory(session);

		sessionControl.replay();

		SessionHolder sessionHolder = new SessionHolder(factory.createSession());
		TransactionSynchronizationManager.bindResource(factory, sessionHolder);

		TopLinkTemplate template = new TopLinkTemplate();
		template.setAllowCreate(true);
		template.setSessionFactory(factory);
		template.execute(new TopLinkCallback() {
			public Object doInTopLink(Session session) throws TopLinkException {
				assertTrue(session != null);
				return null;
			}
		});
		assertTrue(TransactionSynchronizationManager.hasResource(factory));
		sessionControl.verify();
		TransactionSynchronizationManager.unbindResource(factory);
	}
}
