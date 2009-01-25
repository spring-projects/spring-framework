/*
 * Copyright 2002-2009 the original author or authors.
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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
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
 * Supports a "entityManagerFactoryBeanName" filter init-param in <code>web.xml</code>;
 * the default bean name is "entityManagerFactory". Looks up the EntityManagerFactory
 * on each request, to avoid initialization order issues (when using ContextLoaderServlet,
 * the root application context will get initialized <i>after</i> this filter).
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

	public static final String DEFAULT_PERSISTENCE_MANAGER_FACTORY_BEAN_NAME = "entityManagerFactory";

	private String entityManagerFactoryBeanName = DEFAULT_PERSISTENCE_MANAGER_FACTORY_BEAN_NAME;


	/**
	 * Set the bean name of the EntityManagerFactory to fetch from Spring's
	 * root application context. Default is "entityManagerFactory".
	 * @see #DEFAULT_PERSISTENCE_MANAGER_FACTORY_BEAN_NAME
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


	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		EntityManagerFactory emf = lookupEntityManagerFactory(request);
		boolean participate = false;

		if (TransactionSynchronizationManager.hasResource(emf)) {
			// Do not modify the EntityManager: just set the participate flag.
			participate = true;
		}
		else {
			logger.debug("Opening JPA EntityManager in OpenEntityManagerInViewFilter");
			try {
				EntityManager em = createEntityManager(emf);
				TransactionSynchronizationManager.bindResource(emf, new EntityManagerHolder(em));
			}
			catch (PersistenceException ex) {
				throw new DataAccessResourceFailureException("Could not create JPA EntityManager", ex);
			}
		}

		try {
			filterChain.doFilter(request, response);
		}

		finally {
			if (!participate) {
				EntityManagerHolder emHolder = (EntityManagerHolder)
						TransactionSynchronizationManager.unbindResource(emf);
				logger.debug("Closing JPA EntityManager in OpenEntityManagerInViewFilter");
				EntityManagerFactoryUtils.closeEntityManager(emHolder.getEntityManager());
			}
		}
	}

	/**
	 * Look up the EntityManagerFactory that this filter should use,
	 * taking the current HTTP request as argument.
	 * <p>Default implementation delegates to the <code>lookupEntityManagerFactory</code>
	 * without arguments.
	 * @return the EntityManagerFactory to use
	 * @see #lookupEntityManagerFactory()
	 */
	protected EntityManagerFactory lookupEntityManagerFactory(HttpServletRequest request) {
		return lookupEntityManagerFactory();
	}

	/**
	 * Look up the EntityManagerFactory that this filter should use.
	 * The default implementation looks for a bean with the specified name
	 * in Spring's root application context.
	 * @return the EntityManagerFactory to use
	 * @see #getEntityManagerFactoryBeanName
	 */
	protected EntityManagerFactory lookupEntityManagerFactory() {
		if (logger.isDebugEnabled()) {
			logger.debug("Using EntityManagerFactory '" + getEntityManagerFactoryBeanName() +
					"' for OpenEntityManagerInViewFilter");
		}
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		return wac.getBean(getEntityManagerFactoryBeanName(), EntityManagerFactory.class);
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

}
