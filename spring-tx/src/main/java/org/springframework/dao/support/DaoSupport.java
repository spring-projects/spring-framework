/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.dao.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

/**
 * Generic base class for DAOs, defining template methods for DAO initialization.
 *
 * <p>Extended by Spring's specific DAO support classes, such as:
 * JdbcDaoSupport, JdoDaoSupport, etc.
 *
 * @author Juergen Hoeller
 * @since 1.2.2
 * @see org.springframework.jdbc.core.support.JdbcDaoSupport
 * @deprecated as of 7.0, in favor of direct injection of client dependencies
 */
@Deprecated(since = "7.0", forRemoval = true)
public abstract class DaoSupport implements InitializingBean {

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public final void afterPropertiesSet() throws IllegalArgumentException, BeanInitializationException {
		// Let abstract subclasses check their configuration.
		checkDaoConfig();

		// Let concrete implementations initialize themselves.
		try {
			initDao();
		}
		catch (Exception ex) {
			throw new BeanInitializationException("Initialization of DAO failed", ex);
		}
	}

	/**
	 * Abstract subclasses must override this to check their configuration.
	 * <p>Implementors should be marked as {@code final} if concrete subclasses
	 * are not supposed to override this template method themselves.
	 * @throws IllegalArgumentException in case of illegal configuration
	 */
	protected abstract void checkDaoConfig() throws IllegalArgumentException;

	/**
	 * Concrete subclasses can override this for custom initialization behavior.
	 * Gets called after population of this instance's bean properties.
	 * @throws Exception if DAO initialization fails
	 * (will be rethrown as a BeanInitializationException)
	 * @see org.springframework.beans.factory.BeanInitializationException
	 */
	protected void initDao() throws Exception {
	}

}
