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

package org.springframework.orm.jpa.support;

import java.io.IOException;
import java.util.concurrent.Callable;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptorAdapter;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet 2.3 Filter that binds a JPA EntityManager to the thread for the
 * entire processing of the request. Intended for the "Open EntityManager in
 * View" pattern, i.e. to allow for lazy loading in web views despite the
 * original transactions already being completed.
 *
 * <p>This filter makes JPA EntityManagers available via the current thread,
 * which will be autodetected by transaction managers. It is suitable for service
 * layer transactions via {@link org.springframework.orm.jpa.JpaTransactionManager}
 * or {@link org.springframework.transaction.jta.JtaTransactionManager} as well
 * as for non-transactional read-only execution.
 *
 * <p>Looks up the EntityManagerFactory in Spring's root web application context.
 * Supports an "entityManagerFactoryBeanName" filter init-param in <code>web.xml</code>;
 * the default bean name is "entityManagerFactory". As an alternative, the
 * "persistenceUnitName" init-param allows for retrieval by logical unit name
 * (as specified in <code>persistence.xml</code>).
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see OpenEntityManagerInViewInterceptor
 * @see org.springframework.orm.jpa.JpaInterceptor
 * @see org.springframework.orm.jpa.JpaTransactionManager
 * @see org.springframework.orm.jpa.JpaTemplate#execute
 * @see org.springframework.orm.jpa.SharedEntityManagerCreator
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 */
public class OpenEntityManagerInViewFilter extends OncePerRequestFilter {

	/**
	 * Default EntityManagerFactory bean name: "entityManagerFactory".
	 * Only applies when no "persistenceUnitName" param has been specified.
	 * @see #setEntityManagerFactoryBeanName
	 * @see #setPersistenceUnitName
	 */
	public static final String DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME = "entityManagerFactory";

	private String entityManagerFactoryBeanName;

	private String persistenceUnitName;

	private volatile EntityManagerFactory entityManagerFactory;


	/**
	 * Set the bean name of the EntityManagerFactory to fetch from Spring's
	 * root application context.
	 * <p>Default is "entityManagerFactory". Note that this default only applies
	 * when no "persistenceUnitName" param has been specified.
	 * @see #setPersistenceUnitName
	 * @see #DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME
	 */
	public void setEntityManagerFactoryBeanName(String entityManagerFactoryBeanName) {
		this.entityManagerFactoryBeanName = entityManagerFactoryBeanName;
	}

	/**
	 * Return the bean name of the EntityManagerFactory to fetch from Spring's
	 * root application context.
	 */
	protected String getEntityManagerFactoryBeanName() {
		return this.entityManagerFactoryBeanName;
	}

	/**
	 * Set the name of the persistence unit to access the EntityManagerFactory for.
	 * <p>This is an alternative to specifying the EntityManagerFactory by bean name,
	 * resolving it by its persistence unit name instead. If no bean name and no persistence
	 * unit name have been specified, we'll check whether a bean exists for the default
	 * bean name "entityManagerFactory"; if not, a default EntityManagerFactory will
	 * be retrieved through finding a single unique bean of type EntityManagerFactory.
	 * @see #setEntityManagerFactoryBeanName
	 * @see #DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME
	 */
	public void setPersistenceUnitName(String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	/**
	 * Return the name of the persistence unit to access the EntityManagerFactory for, if any.
	 */
	protected String getPersistenceUnitName() {
		return this.persistenceUnitName;
	}

	/**
	 * The default value is "false" so that the filter may re-bind the opened
	 * {@code EntityManager} to each asynchronously dispatched thread and postpone
	 * closing it until the very last asynchronous dispatch.
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		EntityManagerFactory emf = lookupEntityManagerFactory(request);
		boolean participate = false;

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		boolean isFirstRequest = !asyncManager.hasConcurrentResult();
		String key = getAlreadyFilteredAttributeName();

		if (TransactionSynchronizationManager.hasResource(emf)) {
			// Do not modify the EntityManager: just set the participate flag.
			participate = true;
		}
		else {
			if (isFirstRequest || !applyEntityManagerBindingInterceptor(asyncManager, key)) {
				logger.debug("Opening JPA EntityManager in OpenEntityManagerInViewFilter");
				try {
					EntityManager em = createEntityManager(emf);
					EntityManagerHolder emHolder = new EntityManagerHolder(em);
					TransactionSynchronizationManager.bindResource(emf, emHolder);

					asyncManager.registerCallableInterceptor(key, new EntityManagerBindingCallableInterceptor(emf, emHolder));
				}
				catch (PersistenceException ex) {
					throw new DataAccessResourceFailureException("Could not create JPA EntityManager", ex);
				}
			}
		}

		try {
			filterChain.doFilter(request, response);
		}

		finally {
			if (!participate) {
				EntityManagerHolder emHolder = (EntityManagerHolder)
						TransactionSynchronizationManager.unbindResource(emf);
				if (!asyncManager.isConcurrentHandlingStarted()) {
					logger.debug("Closing JPA EntityManager in OpenEntityManagerInViewFilter");
					EntityManagerFactoryUtils.closeEntityManager(emHolder.getEntityManager());
				}
			}
		}
	}

	/**
	 * Look up the EntityManagerFactory that this filter should use,
	 * taking the current HTTP request as argument.
	 * <p>The default implementation delegates to the <code>lookupEntityManagerFactory</code>
	 * without arguments, caching the EntityManagerFactory reference once obtained.
	 * @return the EntityManagerFactory to use
	 * @see #lookupEntityManagerFactory()
	 */
	protected EntityManagerFactory lookupEntityManagerFactory(HttpServletRequest request) {
		if (this.entityManagerFactory == null) {
			this.entityManagerFactory = lookupEntityManagerFactory();
		}
		return this.entityManagerFactory;
	}

	/**
	 * Look up the EntityManagerFactory that this filter should use.
	 * <p>The default implementation looks for a bean with the specified name
	 * in Spring's root application context.
	 * @return the EntityManagerFactory to use
	 * @see #getEntityManagerFactoryBeanName
	 */
	protected EntityManagerFactory lookupEntityManagerFactory() {
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		String emfBeanName = getEntityManagerFactoryBeanName();
		String puName = getPersistenceUnitName();
		if (StringUtils.hasLength(emfBeanName)) {
			return wac.getBean(emfBeanName, EntityManagerFactory.class);
		}
		else if (!StringUtils.hasLength(puName) && wac.containsBean(DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME)) {
			return wac.getBean(DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME, EntityManagerFactory.class);
		}
		else {
			// Includes fallback search for single EntityManagerFactory bean by type.
			return EntityManagerFactoryUtils.findEntityManagerFactory(wac, puName);
		}
	}

	/**
	 * Create a JPA EntityManager to be bound to a request.
	 * <p>Can be overridden in subclasses.
	 * @param emf the EntityManagerFactory to use
	 * @see javax.persistence.EntityManagerFactory#createEntityManager()
	 */
	protected EntityManager createEntityManager(EntityManagerFactory emf) {
		return emf.createEntityManager();
	}

	private boolean applyEntityManagerBindingInterceptor(WebAsyncManager asyncManager, String key) {
		if (asyncManager.getCallableInterceptor(key) == null) {
			return false;
		}
		((EntityManagerBindingCallableInterceptor) asyncManager.getCallableInterceptor(key)).initializeThread();
		return true;
	}

	/**
	 * Bind and unbind the {@code EntityManager} to the current thread.
	 */
	private static class EntityManagerBindingCallableInterceptor extends CallableProcessingInterceptorAdapter {

		private final EntityManagerFactory emFactory;

		private final EntityManagerHolder emHolder;


		public EntityManagerBindingCallableInterceptor(EntityManagerFactory emFactory, EntityManagerHolder emHolder) {
			this.emFactory = emFactory;
			this.emHolder = emHolder;
		}

		@Override
		public <T> void preProcess(NativeWebRequest request, Callable<T> task) {
			initializeThread();
		}

		@Override
		public <T> void postProcess(NativeWebRequest request, Callable<T> task, Object concurrentResult) {
			TransactionSynchronizationManager.unbindResource(this.emFactory);
		}

		private void initializeThread() {
			TransactionSynchronizationManager.bindResource(this.emFactory, this.emHolder);
		}
	}

}
