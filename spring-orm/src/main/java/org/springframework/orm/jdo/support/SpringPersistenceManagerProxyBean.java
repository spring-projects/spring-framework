/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.orm.jdo.support;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.jdo.DefaultJdoDialect;
import org.springframework.orm.jdo.JdoDialect;
import org.springframework.orm.jdo.PersistenceManagerFactoryUtils;
import org.springframework.util.Assert;

/**
 * Proxy that implements the {@link javax.jdo.PersistenceManager} interface,
 * delegating to the current thread-bound PersistenceManager (the Spring-managed
 * transactional PersistenceManager or the single OpenPersistenceManagerInView
 * PersistenceManager, if any) on each invocation. This class makes such a
 * Spring-style PersistenceManager proxy available for bean references.
 *
 * <p>The main advantage of this proxy is that it allows DAOs to work with a
 * plain JDO PersistenceManager reference in JDO 2.1 style
 * (see {@link javax.jdo.PersistenceManagerFactory#getPersistenceManagerProxy()}),
 * while still participating in Spring's resource and transaction management.
 *
 * <p>The behavior of this proxy matches the behavior that the JDO 2.1 spec
 * defines for a PersistenceManager proxy. Hence, DAOs could seamlessly switch
 * between {@link StandardPersistenceManagerProxyBean} and this Spring-style proxy,
 * receiving the reference through Dependency Injection. This will work without
 * any Spring API dependencies in the DAO code!
 *
 * <p>Note: In contrast to {@link StandardPersistenceManagerProxyBean}, this proxy
 * works with JDO 2.0 and higher. It does not require JDO 2.1.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see StandardPersistenceManagerProxyBean
 * @see javax.jdo.PersistenceManagerFactory#getPersistenceManagerProxy()
 * @see org.springframework.orm.jdo.PersistenceManagerFactoryUtils#getPersistenceManager
 * @see org.springframework.orm.jdo.PersistenceManagerFactoryUtils#releasePersistenceManager
 */
public class SpringPersistenceManagerProxyBean implements FactoryBean<PersistenceManager>, InitializingBean {

	private PersistenceManagerFactory persistenceManagerFactory;

	private JdoDialect jdoDialect;

	private Class<? extends PersistenceManager> persistenceManagerInterface = PersistenceManager.class;

	private boolean allowCreate = true;

	private PersistenceManager proxy;


	/**
	 * Set the target PersistenceManagerFactory for this proxy.
	 */
	public void setPersistenceManagerFactory(PersistenceManagerFactory persistenceManagerFactory) {
		this.persistenceManagerFactory = persistenceManagerFactory;
	}

	/**
	 * Return the target PersistenceManagerFactory for this proxy.
	 */
	protected PersistenceManagerFactory getPersistenceManagerFactory() {
		return this.persistenceManagerFactory;
	}

	/**
	 * Set the JDO dialect to use for this proxy.
	 * <p>Default is a DefaultJdoDialect based on the PersistenceManagerFactory's
	 * underlying DataSource, if any.
	 */
	public void setJdoDialect(JdoDialect jdoDialect) {
		this.jdoDialect = jdoDialect;
	}

	/**
	 * Return the JDO dialect to use for this proxy.
	 */
	protected JdoDialect getJdoDialect() {
		return this.jdoDialect;
	}

	/**
	 * Specify the PersistenceManager interface to expose,
	 * possibly including vendor extensions.
	 * <p>Default is the standard {@code javax.jdo.PersistenceManager} interface.
	 */
	public void setPersistenceManagerInterface(Class<? extends PersistenceManager> persistenceManagerInterface) {
		this.persistenceManagerInterface = persistenceManagerInterface;
		Assert.notNull(persistenceManagerInterface, "persistenceManagerInterface must not be null");
		Assert.isAssignable(PersistenceManager.class, persistenceManagerInterface);
	}

	/**
	 * Return the PersistenceManager interface to expose.
	 */
	protected Class<? extends PersistenceManager> getPersistenceManagerInterface() {
		return this.persistenceManagerInterface;
	}

	/**
	 * Set whether the PersistenceManagerFactory proxy is allowed to create
	 * a non-transactional PersistenceManager when no transactional
	 * PersistenceManager can be found for the current thread.
	 * <p>Default is "true". Can be turned off to enforce access to
	 * transactional PersistenceManagers, which safely allows for DAOs
	 * written to get a PersistenceManager without explicit closing
	 * (i.e. a {@code PersistenceManagerFactory.getPersistenceManager()}
	 * call without corresponding {@code PersistenceManager.close()} call).
	 * @see org.springframework.orm.jdo.PersistenceManagerFactoryUtils#getPersistenceManager(javax.jdo.PersistenceManagerFactory, boolean)
	 */
	public void setAllowCreate(boolean allowCreate) {
		this.allowCreate = allowCreate;
	}

	/**
	 * Return whether the PersistenceManagerFactory proxy is allowed to create
	 * a non-transactional PersistenceManager when no transactional
	 * PersistenceManager can be found for the current thread.
	 */
	protected boolean isAllowCreate() {
		return this.allowCreate;
	}

	public void afterPropertiesSet() {
		if (getPersistenceManagerFactory() == null) {
			throw new IllegalArgumentException("Property 'persistenceManagerFactory' is required");
		}
		// Build default JdoDialect if none explicitly specified.
		if (this.jdoDialect == null) {
			this.jdoDialect = new DefaultJdoDialect(getPersistenceManagerFactory().getConnectionFactory());
		}
		this.proxy = (PersistenceManager) Proxy.newProxyInstance(
				getPersistenceManagerFactory().getClass().getClassLoader(),
				new Class[] {getPersistenceManagerInterface()}, new PersistenceManagerInvocationHandler());
	}


	public PersistenceManager getObject() {
		return this.proxy;
	}

	public Class<? extends PersistenceManager> getObjectType() {
		return getPersistenceManagerInterface();
	}

	public boolean isSingleton() {
		return true;
	}


	/**
	 * Invocation handler that delegates close calls on PersistenceManagers to
	 * PersistenceManagerFactoryUtils for being aware of thread-bound transactions.
	 */
	private class PersistenceManagerInvocationHandler implements InvocationHandler {

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on PersistenceManager interface coming in...

			if (method.getName().equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// Use hashCode of PersistenceManager proxy.
				return System.identityHashCode(proxy);
			}
			else if (method.getName().equals("toString")) {
				// Deliver toString without touching a target EntityManager.
				return "Spring PersistenceManager proxy for target factory [" + getPersistenceManagerFactory() + "]";
			}
			else if (method.getName().equals("getPersistenceManagerFactory")) {
				// Return PersistenceManagerFactory without creating a PersistenceManager.
				return getPersistenceManagerFactory();
			}
			else if (method.getName().equals("isClosed")) {
				// Proxy is always usable.
				return false;
			}
			else if (method.getName().equals("close")) {
				// Suppress close method.
				return null;
			}

			// Invoke method on target PersistenceManager.
			PersistenceManager pm = PersistenceManagerFactoryUtils.doGetPersistenceManager(
					getPersistenceManagerFactory(), isAllowCreate());
			try {
				Object retVal = method.invoke(pm, args);
				if (retVal instanceof Query) {
					PersistenceManagerFactoryUtils.applyTransactionTimeout(
							(Query) retVal, getPersistenceManagerFactory(), getJdoDialect());
				}
				return retVal;
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
			finally {
				PersistenceManagerFactoryUtils.doReleasePersistenceManager(pm, getPersistenceManagerFactory());
			}
		}
	}

}
