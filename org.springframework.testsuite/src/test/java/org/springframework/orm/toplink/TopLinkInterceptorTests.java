/*
 * Created on Mar 20, 2005
 *
 */

package org.springframework.orm.toplink;

import junit.framework.TestCase;
import org.aopalliance.intercept.MethodInvocation;
import org.easymock.MockControl;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import oracle.toplink.sessions.Session;

/**
 * @author Juergen Hoeller
 * @author <a href="mailto:james.x.clark@oracle.com">James Clark</a>
 * @since 28.04.2005
 */
public class TopLinkInterceptorTests extends TestCase {

	public void testInterceptorWithNoSessionBoundAndNoSynchronizations() throws Throwable {
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();
		MockControl methodInvocationControl = MockControl.createControl(MethodInvocation.class);
		MethodInvocation methodInvocation = (MethodInvocation) methodInvocationControl.getMock();

		SessionFactory factory = new SingleSessionFactory(session);

		TopLinkInterceptor interceptor = new TopLinkInterceptor();
		interceptor.setSessionFactory(factory);

		methodInvocation.proceed();
		methodInvocationControl.setReturnValue(null, 1);
		session.release();
		sessionControl.setVoidCallable(1);

		methodInvocationControl.replay();
		sessionControl.replay();

		try {
			interceptor.invoke(methodInvocation);
		}
		catch (Throwable t) {
			System.out.println(t);
			t.printStackTrace();
			fail();
		}

		assertFalse(TransactionSynchronizationManager.hasResource(factory));

		sessionControl.verify();
		methodInvocationControl.verify();
		sessionControl.verify();
	}

	public void testInterceptorWithNoSessionBoundAndSynchronizationsActive() {
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();
		MockControl methodInvocationControl = MockControl.createControl(MethodInvocation.class);
		MethodInvocation methodInvocation = (MethodInvocation) methodInvocationControl.getMock();

		SessionFactory factory = new SingleSessionFactory(session);

		TopLinkInterceptor interceptor = new TopLinkInterceptor();
		interceptor.setSessionFactory(factory);

		try {
			methodInvocation.proceed();
		}
		catch (Throwable e) {
			fail();
		}
		methodInvocationControl.setReturnValue(null, 1);

		methodInvocationControl.replay();
		sessionControl.replay();

		TransactionSynchronizationManager.initSynchronization();
		try {
			interceptor.invoke(methodInvocation);
		}
		catch (Throwable t) {
			fail();
		}

		assertTrue(TransactionSynchronizationManager.hasResource(factory));
		assertTrue(TransactionSynchronizationManager.getSynchronizations().size() == 1);

		TransactionSynchronizationManager.clearSynchronization();
		TransactionSynchronizationManager.unbindResource(factory);

		sessionControl.verify();
		methodInvocationControl.verify();
	}

}
