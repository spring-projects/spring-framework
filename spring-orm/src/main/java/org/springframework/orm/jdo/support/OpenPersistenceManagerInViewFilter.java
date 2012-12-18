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

package org.springframework.orm.jdo.support;

import java.io.IOException;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.orm.jdo.PersistenceManagerFactoryUtils;
import org.springframework.orm.jdo.PersistenceManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet 2.3 Filter that binds a JDO PersistenceManager to the thread for the
 * entire processing of the request. Intended for the "Open PersistenceManager in
 * View" pattern, i.e. to allow for lazy loading in web views despite the
 * original transactions already being completed.
 *
 * <p>This filter makes JDO PersistenceManagers available via the current thread,
 * which will be autodetected by transaction managers. It is suitable for service
 * layer transactions via {@link org.springframework.orm.jdo.JdoTransactionManager}
 * or {@link org.springframework.transaction.jta.JtaTransactionManager} as well
 * as for non-transactional read-only execution.
 *
 * <p>Looks up the PersistenceManagerFactory in Spring's root web application context.
 * Supports a "persistenceManagerFactoryBeanName" filter init-param in {@code web.xml};
 * the default bean name is "persistenceManagerFactory". Looks up the PersistenceManagerFactory
 * on each request, to avoid initialization order issues (when using ContextLoaderServlet,
 * the root application context will get initialized <i>after</i> this filter).
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see OpenPersistenceManagerInViewInterceptor
 * @see org.springframework.orm.jdo.JdoInterceptor
 * @see org.springframework.orm.jdo.JdoTransactionManager
 * @see org.springframework.orm.jdo.PersistenceManagerFactoryUtils#getPersistenceManager
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 */
public class OpenPersistenceManagerInViewFilter extends OncePerRequestFilter {

	public static final String DEFAULT_PERSISTENCE_MANAGER_FACTORY_BEAN_NAME = "persistenceManagerFactory";

	private String persistenceManagerFactoryBeanName = DEFAULT_PERSISTENCE_MANAGER_FACTORY_BEAN_NAME;


	/**
	 * Set the bean name of the PersistenceManagerFactory to fetch from Spring's
	 * root application context. Default is "persistenceManagerFactory".
	 * @see #DEFAULT_PERSISTENCE_MANAGER_FACTORY_BEAN_NAME
	 */
	public void setPersistenceManagerFactoryBeanName(String persistenceManagerFactoryBeanName) {
		this.persistenceManagerFactoryBeanName = persistenceManagerFactoryBeanName;
	}

	/**
	 * Return the bean name of the PersistenceManagerFactory to fetch from Spring's
	 * root application context.
	 */
	protected String getPersistenceManagerFactoryBeanName() {
		return this.persistenceManagerFactoryBeanName;
	}


	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		PersistenceManagerFactory pmf = lookupPersistenceManagerFactory(request);
		boolean participate = false;

		if (TransactionSynchronizationManager.hasResource(pmf)) {
			// Do not modify the PersistenceManager: just set the participate flag.
			participate = true;
		}
		else {
			logger.debug("Opening JDO PersistenceManager in OpenPersistenceManagerInViewFilter");
			PersistenceManager pm = PersistenceManagerFactoryUtils.getPersistenceManager(pmf, true);
			TransactionSynchronizationManager.bindResource(pmf, new PersistenceManagerHolder(pm));
		}

		try {
			filterChain.doFilter(request, response);
		}

		finally {
			if (!participate) {
				PersistenceManagerHolder pmHolder = (PersistenceManagerHolder)
						TransactionSynchronizationManager.unbindResource(pmf);
				logger.debug("Closing JDO PersistenceManager in OpenPersistenceManagerInViewFilter");
				PersistenceManagerFactoryUtils.releasePersistenceManager(pmHolder.getPersistenceManager(), pmf);
			}
		}
	}

	/**
	 * Look up the PersistenceManagerFactory that this filter should use,
	 * taking the current HTTP request as argument.
	 * <p>Default implementation delegates to the {@code lookupPersistenceManagerFactory}
	 * without arguments.
	 * @return the PersistenceManagerFactory to use
	 * @see #lookupPersistenceManagerFactory()
	 */
	protected PersistenceManagerFactory lookupPersistenceManagerFactory(HttpServletRequest request) {
		return lookupPersistenceManagerFactory();
	}

	/**
	 * Look up the PersistenceManagerFactory that this filter should use.
	 * The default implementation looks for a bean with the specified name
	 * in Spring's root application context.
	 * @return the PersistenceManagerFactory to use
	 * @see #getPersistenceManagerFactoryBeanName
	 */
	protected PersistenceManagerFactory lookupPersistenceManagerFactory() {
		if (logger.isDebugEnabled()) {
			logger.debug("Using PersistenceManagerFactory '" + getPersistenceManagerFactoryBeanName() +
					"' for OpenPersistenceManagerInViewFilter");
		}
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		return wac.getBean(getPersistenceManagerFactoryBeanName(), PersistenceManagerFactory.class);
	}

}
