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

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.orm.jdo.PersistenceManagerFactoryUtils;
import org.springframework.orm.jdo.PersistenceManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

/**
 * Spring web request interceptor that binds a JDO PersistenceManager to the
 * thread for the entire processing of the request. Intended for the "Open
 * PersistenceManager in View" pattern, i.e. to allow for lazy loading in
 * web views despite the original transactions already being completed.
 *
 * <p>This interceptor makes JDO PersistenceManagers available via the current thread,
 * which will be autodetected by transaction managers. It is suitable for service
 * layer transactions via {@link org.springframework.orm.jdo.JdoTransactionManager}
 * or {@link org.springframework.transaction.jta.JtaTransactionManager} as well
 * as for non-transactional read-only execution.
 *
 * <p>In contrast to {@link OpenPersistenceManagerInViewFilter}, this interceptor
 * is set up in a Spring application context and can thus take advantage of
 * bean wiring. It inherits common JDO configuration properties from
 * {@link org.springframework.orm.jdo.JdoAccessor}, to be configured in a
 * bean definition.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see OpenPersistenceManagerInViewFilter
 * @see org.springframework.orm.jdo.JdoInterceptor
 * @see org.springframework.orm.jdo.JdoTransactionManager
 * @see org.springframework.orm.jdo.PersistenceManagerFactoryUtils#getPersistenceManager
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 */
public class OpenPersistenceManagerInViewInterceptor implements WebRequestInterceptor {

	/**
	 * Suffix that gets appended to the PersistenceManagerFactory toString
	 * representation for the "participate in existing persistence manager
	 * handling" request attribute.
	 * @see #getParticipateAttributeName
	 */
	public static final String PARTICIPATE_SUFFIX = ".PARTICIPATE";


	protected final Log logger = LogFactory.getLog(getClass());

	private PersistenceManagerFactory persistenceManagerFactory;


	/**
	 * Set the JDO PersistenceManagerFactory that should be used to create
	 * PersistenceManagers.
	 */
	public void setPersistenceManagerFactory(PersistenceManagerFactory pmf) {
		this.persistenceManagerFactory = pmf;
	}

	/**
	 * Return the JDO PersistenceManagerFactory that should be used to create
	 * PersistenceManagers.
	 */
	public PersistenceManagerFactory getPersistenceManagerFactory() {
		return persistenceManagerFactory;
	}


	public void preHandle(WebRequest request) throws DataAccessException {
		if (TransactionSynchronizationManager.hasResource(getPersistenceManagerFactory())) {
			// Do not modify the PersistenceManager: just mark the request accordingly.
			String participateAttributeName = getParticipateAttributeName();
			Integer count = (Integer) request.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
			int newCount = (count != null ? count + 1 : 1);
			request.setAttribute(getParticipateAttributeName(), newCount, WebRequest.SCOPE_REQUEST);
		}
		else {
			logger.debug("Opening JDO PersistenceManager in OpenPersistenceManagerInViewInterceptor");
			PersistenceManager pm =
					PersistenceManagerFactoryUtils.getPersistenceManager(getPersistenceManagerFactory(), true);
			TransactionSynchronizationManager.bindResource(
					getPersistenceManagerFactory(), new PersistenceManagerHolder(pm));
		}
	}

	public void postHandle(WebRequest request, ModelMap model) {
	}

	public void afterCompletion(WebRequest request, Exception ex) throws DataAccessException {
		String participateAttributeName = getParticipateAttributeName();
		Integer count = (Integer) request.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
		if (count != null) {
			// Do not modify the PersistenceManager: just clear the marker.
			if (count > 1) {
				request.setAttribute(participateAttributeName, count - 1, WebRequest.SCOPE_REQUEST);
			}
			else {
				request.removeAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
			}
		}
		else {
			PersistenceManagerHolder pmHolder = (PersistenceManagerHolder)
					TransactionSynchronizationManager.unbindResource(getPersistenceManagerFactory());
			logger.debug("Closing JDO PersistenceManager in OpenPersistenceManagerInViewInterceptor");
			PersistenceManagerFactoryUtils.releasePersistenceManager(
					pmHolder.getPersistenceManager(), getPersistenceManagerFactory());
		}
	}

	/**
	 * Return the name of the request attribute that identifies that a request is
	 * already filtered. Default implementation takes the toString representation
	 * of the PersistenceManagerFactory instance and appends ".FILTERED".
	 * @see #PARTICIPATE_SUFFIX
	 */
	protected String getParticipateAttributeName() {
		return getPersistenceManagerFactory().toString() + PARTICIPATE_SUFFIX;
	}

}
