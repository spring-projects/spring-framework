/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.orm.jpa;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.CollectionUtils;

/**
 * Factory for a shareable JPA {@link javax.persistence.EntityManager}
 * for a given {@link javax.persistence.EntityManagerFactory}.
 *
 * <p>The shareable EntityManager will behave just like an EntityManager fetched
 * from an application server's JNDI environment, as defined by the JPA
 * specification. It will delegate all calls to the current transactional
 * EntityManager, if any; otherwise it will fall back to a newly created
 * EntityManager per operation.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 * @see org.springframework.orm.jpa.LocalEntityManagerFactoryBean
 * @see org.springframework.orm.jpa.JpaTransactionManager
 */
public abstract class SharedEntityManagerCreator {

	/**
	 * Create a transactional EntityManager proxy for the given EntityManagerFactory.
	 * @param emf the EntityManagerFactory to delegate to.
	 * If this implements the {@link EntityManagerFactoryInfo} interface,
	 * appropriate handling of the native EntityManagerFactory and available
	 * {@link EntityManagerPlusOperations} will automatically apply.
	 * @return a shareable transaction EntityManager proxy
	 */
	public static EntityManager createSharedEntityManager(EntityManagerFactory emf) {
		return createSharedEntityManager(emf, null);
	}

	/**
	 * Create a transactional EntityManager proxy for the given EntityManagerFactory.
	 * @param emf the EntityManagerFactory to delegate to.
	 * If this implements the {@link EntityManagerFactoryInfo} interface,
	 * appropriate handling of the native EntityManagerFactory and available
	 * {@link EntityManagerPlusOperations} will automatically apply.
	 * @param properties the properties to be passed into the
	 * <code>createEntityManager</code> call (may be <code>null</code>)
	 * @return a shareable transaction EntityManager proxy
	 */
	public static EntityManager createSharedEntityManager(EntityManagerFactory emf, Map properties) {
		Class[] emIfcs = null;
		if (emf instanceof EntityManagerFactoryInfo) {
			EntityManagerFactoryInfo emfInfo = (EntityManagerFactoryInfo) emf;
			Class emIfc = emfInfo.getEntityManagerInterface();
			if (emIfc == null) {
				emIfc = EntityManager.class;
			}
			JpaDialect jpaDialect = emfInfo.getJpaDialect();
			if (jpaDialect != null && jpaDialect.supportsEntityManagerPlusOperations()) {
				emIfcs = new Class[] {emIfc, EntityManagerPlus.class};
			}
			else {
				emIfcs = new Class[] {emIfc};
			}
		}
		else {
			emIfcs = new Class[] {EntityManager.class};
		}
		return createSharedEntityManager(emf, properties, emIfcs);
	}

	/**
	 * Create a transactional EntityManager proxy for the given EntityManagerFactory.
	 * @param emf EntityManagerFactory to obtain EntityManagers from as needed
	 * @param properties the properties to be passed into the
	 * <code>createEntityManager</code> call (may be <code>null</code>)
	 * @param entityManagerInterfaces the interfaces to be implemented by the
	 * EntityManager. Allows the addition or specification of proprietary interfaces.
	 * @return a shareable transactional EntityManager proxy
	 */
	public static EntityManager createSharedEntityManager(
			EntityManagerFactory emf, Map properties, Class... entityManagerInterfaces) {

		ClassLoader cl = null;
		if (emf instanceof EntityManagerFactoryInfo) {
			cl = ((EntityManagerFactoryInfo) emf).getBeanClassLoader();
		}
		Class[] ifcs = new Class[entityManagerInterfaces.length + 1];
		System.arraycopy(entityManagerInterfaces, 0, ifcs, 0, entityManagerInterfaces.length);
		ifcs[entityManagerInterfaces.length] = EntityManagerProxy.class;
		return (EntityManager) Proxy.newProxyInstance(
				(cl != null ? cl : SharedEntityManagerCreator.class.getClassLoader()),
				ifcs, new SharedEntityManagerInvocationHandler(emf, properties));
	}


	/**
	 * Invocation handler that delegates all calls to the current
	 * transactional EntityManager, if any; else, it will fall back
	 * to a newly created EntityManager per operation.
	 */
	private static class SharedEntityManagerInvocationHandler implements InvocationHandler {

		private final Log logger = LogFactory.getLog(getClass());

		private final EntityManagerFactory targetFactory;

		private final Map properties;

		public SharedEntityManagerInvocationHandler(EntityManagerFactory target, Map properties) {
			this.targetFactory = target;
			this.properties = properties;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on EntityManager interface coming in...

			if (method.getName().equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// Use hashCode of EntityManager proxy.
				return hashCode();
			}
			else if (method.getName().equals("toString")) {
				// Deliver toString without touching a target EntityManager.
				return "Shared EntityManager proxy for target factory [" + this.targetFactory + "]";
			}
			else if (method.getName().equals("isOpen")) {
				// Handle isOpen method: always return true.
				return true;
			}
			else if (method.getName().equals("close")) {
				// Handle close method: suppress, not valid.
				return null;
			}
			else if (method.getName().equals("getTransaction")) {
				throw new IllegalStateException(
						"Not allowed to create transaction on shared EntityManager - " +
						"use Spring transactions or EJB CMT instead");
			}
			else if (method.getName().equals("joinTransaction")) {
				throw new IllegalStateException(
						"Not allowed to join transaction on shared EntityManager - " +
						"use Spring transactions or EJB CMT instead");
			}

			// Determine current EntityManager: either the transactional one
			// managed by the factory or a temporary one for the given invocation.
			EntityManager target =
					EntityManagerFactoryUtils.doGetTransactionalEntityManager(this.targetFactory, this.properties);

			// Handle EntityManagerProxy interface.
			if (method.getName().equals("getTargetEntityManager")) {
				if (target == null) {
					throw new IllegalStateException("No transactional EntityManager available");
				}
				return target;
			}

			// Regular EntityManager operations.
			boolean isNewEm = false;
			if (target == null) {
				logger.debug("Creating new EntityManager for shared EntityManager invocation");
				target = (!CollectionUtils.isEmpty(this.properties) ?
						this.targetFactory.createEntityManager(this.properties) :
						this.targetFactory.createEntityManager());
				isNewEm = true;
			}

			// Invoke method on current EntityManager.
			try {
				Object result = method.invoke(target, args);
				if (isNewEm && result instanceof Query) {
					result = Proxy.newProxyInstance(Query.class.getClassLoader(), new Class[] {Query.class},
							new DeferredQueryInvocationHandler((Query) result, target));
					isNewEm = false;
				}
				return result;
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
			finally {
				if (isNewEm) {
					EntityManagerFactoryUtils.closeEntityManager(target);
				}
			}
		}
	}


	/**
	 * Invocation handler that handles deferred Query objects created by
	 * non-transactional createQuery invocations on a shared EntityManager.
	 */
	private static class DeferredQueryInvocationHandler implements InvocationHandler {

		private final Query target;

		private final EntityManager em;

		private DeferredQueryInvocationHandler(Query target, EntityManager em) {
			this.target = target;
			this.em = em;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on Query interface coming in...

			if (method.getName().equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// Use hashCode of EntityManager proxy.
				return hashCode();
			}

			// Invoke method on actual Query object.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
			finally {
				if (method.getName().equals("getResultList") || method.getName().equals("getSingleResult") ||
						method.getName().equals("executeUpdate")) {
					EntityManagerFactoryUtils.closeEntityManager(this.em);
				}
			}
		}
	}

}
