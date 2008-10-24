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
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Helper class that allows for writing JPA data access code in the same style
 * as with Spring's well-known JdoTemplate and HibernateTemplate classes.
 * Automatically converts PersistenceExceptions into Spring DataAccessExceptions,
 * following the <code>org.springframework.dao</code> exception hierarchy.
 *
 * <p>The central method is of this template is "execute", supporting JPA access code
 * implementing the {@link JpaCallback} interface. It provides JPA EntityManager
 * handling such that neither the JpaCallback implementation nor the calling code
 * needs to explicitly care about retrieving/closing EntityManagers, or handling
 * JPA lifecycle exceptions.
 *
 * <p>Can be used within a service implementation via direct instantiation with
 * a EntityManagerFactory reference, or get prepared in an application context
 * and given to services as bean reference. Note: The EntityManagerFactory should
 * always be configured as bean in the application context, in the first case
 * given to the service directly, in the second case to the prepared template.
 *
 * <p><b>NOTE: JpaTemplate mainly exists as a sibling of JdoTemplate and
 * HibernateTemplate, offering the same style for people used to it. For newly
 * started projects, consider adopting the standard JPA style of coding data
 * access objects instead, based on a "shared EntityManager" reference injected
 * via a Spring bean definition or the JPA PersistenceContext annotation.</b>
 * (Using Spring's SharedEntityManagerBean / PersistenceAnnotationBeanPostProcessor,
 * or using a direct JNDI lookup for an EntityManager on a Java EE 5 server.)
 *
 * <p>JpaTemplate can be considered as direct alternative to working with the
 * native JPA EntityManager API (through a shared EntityManager reference,
 * as outlined above). The major advantage is its automatic conversion to
 * DataAccessExceptions; the major disadvantage is that it introduces
 * another thin layer on top of the native JPA API. Note that exception
 * translation can also be achieved through AOP advice; check out
 * {@link org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor}.
 *
 * <p>{@link LocalContainerEntityManagerFactoryBean} is the preferred way of
 * obtaining a reference to an EntityManagerFactory, at least outside of a full
 * Java EE 5 environment. The Spring application context will manage its lifecycle,
 * initializing and shutting down the factory as part of the application.
 * Within a Java EE 5 environment, you will typically work with a server-managed
 * EntityManagerFactory that is exposed via JNDI, obtained through Spring's
 * {@link org.springframework.jndi.JndiObjectFactoryBean}.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setEntityManagerFactory
 * @see JpaCallback
 * @see javax.persistence.EntityManager
 * @see LocalEntityManagerFactoryBean
 * @see LocalContainerEntityManagerFactoryBean
 * @see JpaTransactionManager
 * @see org.springframework.transaction.jta.JtaTransactionManager
 * @see org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter
 * @see org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor
 */
public class JpaTemplate extends JpaAccessor implements JpaOperations {

	private boolean exposeNativeEntityManager = false;


	/**
	 * Create a new JpaTemplate instance.
	 */
	public JpaTemplate() {
	}

	/**
	 * Create a new JpaTemplate instance.
	 * @param emf EntityManagerFactory to create EntityManagers
	 */
	public JpaTemplate(EntityManagerFactory emf) {
		setEntityManagerFactory(emf);
		afterPropertiesSet();
	}

	/**
	 * Create a new JpaTemplate instance.
	 * @param em EntityManager to use
	 */
	public JpaTemplate(EntityManager em) {
		setEntityManager(em);
		afterPropertiesSet();
	}


	/**
	 * Set whether to expose the native JPA EntityManager to JpaCallback
	 * code. Default is "false": a EntityManager proxy will be returned,
	 * suppressing <code>close</code> calls and automatically applying transaction
	 * timeouts (if any).
	 * <p>As there is often a need to cast to a provider-specific EntityManager
	 * class in DAOs that use the JPA 1.0 API, for JPA 2.0 previews and other
	 * provider-specific functionality, the exposed proxy implements all interfaces
	 * implemented by the original EntityManager. If this is not sufficient,
	 * turn this flag to "true".
	 * @see JpaCallback
	 * @see javax.persistence.EntityManager
	 */
	public void setExposeNativeEntityManager(boolean exposeNativeEntityManager) {
		this.exposeNativeEntityManager = exposeNativeEntityManager;
	}

	/**
	 * Return whether to expose the native JPA EntityManager to JpaCallback
	 * code, or rather an EntityManager proxy.
	 */
	public boolean isExposeNativeEntityManager() {
		return this.exposeNativeEntityManager;
	}


	public Object execute(JpaCallback action) throws DataAccessException {
		return execute(action, isExposeNativeEntityManager());
	}

	public List executeFind(JpaCallback action) throws DataAccessException {
		Object result = execute(action, isExposeNativeEntityManager());
		if (!(result instanceof List)) {
			throw new InvalidDataAccessApiUsageException(
					"Result object returned from JpaCallback isn't a List: [" + result + "]");
		}
		return (List) result;
	}

	/**
	 * Execute the action specified by the given action object within a
	 * EntityManager.
	 * @param action callback object that specifies the JPA action
	 * @param exposeNativeEntityManager whether to expose the native
	 * JPA entity manager to callback code
	 * @return a result object returned by the action, or <code>null</code>
	 * @throws org.springframework.dao.DataAccessException in case of JPA errors
	 */
	public Object execute(JpaCallback action, boolean exposeNativeEntityManager) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");

		EntityManager em = getEntityManager();
		boolean isNewEm = false;
		if (em == null) {
			em = getTransactionalEntityManager();
			if (em == null) {
				logger.debug("Creating new EntityManager for JpaTemplate execution");
				em = createEntityManager();
				isNewEm = true;
			}
		}

		try {
			EntityManager emToExpose = (exposeNativeEntityManager ? em : createEntityManagerProxy(em));
			Object result = action.doInJpa(emToExpose);
			flushIfNecessary(em, !isNewEm);
			return result;
		}
		catch (RuntimeException ex) {
			throw translateIfNecessary(ex);
		}
		finally {
			if (isNewEm) {
				logger.debug("Closing new EntityManager after JPA template execution");
				EntityManagerFactoryUtils.closeEntityManager(em);
			}
		}
	}

	/**
	 * Create a close-suppressing proxy for the given JPA EntityManager.
	 * The proxy also prepares returned JPA Query objects.
	 * @param em the JPA EntityManager to create a proxy for
	 * @return the EntityManager proxy, implementing all interfaces
	 * implemented by the passed-in EntityManager object (that is,
	 * also implementing all provider-specific extension interfaces)
	 * @see javax.persistence.EntityManager#close
	 */
	protected EntityManager createEntityManagerProxy(EntityManager em) {
		Class[] ifcs = null;
		EntityManagerFactory emf = getEntityManagerFactory();
		if (emf instanceof EntityManagerFactoryInfo) {
			Class entityManagerInterface = ((EntityManagerFactoryInfo) emf).getEntityManagerInterface();
			if (entityManagerInterface != null) {
				ifcs = new Class[] {entityManagerInterface};
			}
		}
		if (ifcs == null) {
			ifcs = ClassUtils.getAllInterfacesForClass(em.getClass());
		}
		return (EntityManager) Proxy.newProxyInstance(
				em.getClass().getClassLoader(), ifcs, new CloseSuppressingInvocationHandler(em));
	}


	//-------------------------------------------------------------------------
	// Convenience methods for load, save, delete
	//-------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	public <T> T find(final Class<T> entityClass, final Object id) throws DataAccessException {
		return (T) execute(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				return em.find(entityClass, id);
			}
		}, true);
	}

	@SuppressWarnings("unchecked")
	public <T> T getReference(final Class<T> entityClass, final Object id) throws DataAccessException {
		return (T) execute(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				return em.getReference(entityClass, id);
			}
		}, true);
	}

	public boolean contains(final Object entity) throws DataAccessException {
		Boolean result = (Boolean) execute(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				return new Boolean(em.contains(entity));
			}
		}, true);
		return result.booleanValue();
	}

	public void refresh(final Object entity) throws DataAccessException {
		execute(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				em.refresh(entity);
				return null;
			}
		}, true);
	}

	public void persist(final Object entity) throws DataAccessException {
		execute(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				em.persist(entity);
				return null;
			}
		}, true);
	}

	@SuppressWarnings("unchecked")
	public <T> T merge(final T entity) throws DataAccessException {
		return (T) execute(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				return em.merge(entity);
			}
		}, true);
	}

	public void remove(final Object entity) throws DataAccessException {
		execute(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				em.remove(entity);
				return null;
			}
		}, true);
	}

	public void flush() throws DataAccessException {
		execute(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				em.flush();
				return null;
			}
		}, true);
	}


	//-------------------------------------------------------------------------
	// Convenience finder methods
	//-------------------------------------------------------------------------

	public List find(String queryString) throws DataAccessException {
		return find(queryString, (Object[]) null);
	}

	public List find(final String queryString, final Object... values) throws DataAccessException {
		return executeFind(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				Query queryObject = em.createQuery(queryString);
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						queryObject.setParameter(i + 1, values[i]);
					}
				}
				return queryObject.getResultList();
			}
		});
	}

	public List findByNamedParams(final String queryString, final Map<String, ?> params) throws DataAccessException {
		return executeFind(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				Query queryObject = em.createQuery(queryString);
				if (params != null) {
					for (Map.Entry<String, ?> entry : params.entrySet()) {
						queryObject.setParameter(entry.getKey(), entry.getValue());
					}
				}
				return queryObject.getResultList();
			}
		});
	}

	public List findByNamedQuery(String queryName) throws DataAccessException {
		return findByNamedQuery(queryName, (Object[]) null);
	}

	public List findByNamedQuery(final String queryName, final Object... values) throws DataAccessException {
		return executeFind(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				Query queryObject = em.createNamedQuery(queryName);
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						queryObject.setParameter(i + 1, values[i]);
					}
				}
				return queryObject.getResultList();
			}
		});
	}

	public List findByNamedQueryAndNamedParams(final String queryName, final Map<String, ?> params)
			throws DataAccessException {

		return executeFind(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				Query queryObject = em.createNamedQuery(queryName);
				if (params != null) {
					for (Map.Entry<String, ?> entry : params.entrySet()) {
						queryObject.setParameter(entry.getKey(), entry.getValue());
					}
				}
				return queryObject.getResultList();
			}
		});
	}


	/**
	 * Invocation handler that suppresses close calls on JPA EntityManagers.
	 * Also prepares returned Query and Criteria objects.
	 * @see javax.persistence.EntityManager#close
	 */
	private class CloseSuppressingInvocationHandler implements InvocationHandler {

		private final EntityManager target;

		public CloseSuppressingInvocationHandler(EntityManager target) {
			this.target = target;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on EntityManager interface (or provider-specific extension) coming in...

			if (method.getName().equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0] ? Boolean.TRUE : Boolean.FALSE);
			}
			else if (method.getName().equals("hashCode")) {
				// Use hashCode of EntityManager proxy.
				return new Integer(System.identityHashCode(proxy));
			}
			else if (method.getName().equals("close")) {
				// Handle close method: suppress, not valid.
				return null;
			}

			// Invoke method on target EntityManager.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
