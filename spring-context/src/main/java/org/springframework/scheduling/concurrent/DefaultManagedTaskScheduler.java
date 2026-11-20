/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.concurrent;

import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

import javax.naming.NamingException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiTemplate;

/**
 * JNDI-based variant of {@link ConcurrentTaskScheduler}, performing a default lookup for
 * JSR-236's "java:comp/DefaultManagedScheduledExecutorService" in a Jakarta EE environment.
 * Expected to be exposed as a bean, in particular as the default lookup happens in the
 * standard {@link InitializingBean#afterPropertiesSet()} callback.
 *
 * <p>Note: This class is not strictly JSR-236 based; it can work with any regular
 * {@link java.util.concurrent.ScheduledExecutorService} that can be found in JNDI.
 * The actual adapting to {@link jakarta.enterprise.concurrent.ManagedScheduledExecutorService}
 * happens in the base class {@link ConcurrentTaskScheduler} itself.
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see jakarta.enterprise.concurrent.ManagedScheduledExecutorService
 */
public class DefaultManagedTaskScheduler extends ConcurrentTaskScheduler implements InitializingBean {

	private final JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();

	private String jndiName = "java:comp/DefaultManagedScheduledExecutorService";


	public DefaultManagedTaskScheduler() {
		// Executor initialization happens in afterPropertiesSet
		super(null);
	}


	/**
	 * Set the JNDI template to use for JNDI lookups.
	 * @see org.springframework.jndi.JndiAccessor#setJndiTemplate
	 */
	public void setJndiTemplate(JndiTemplate jndiTemplate) {
		this.jndiLocator.setJndiTemplate(jndiTemplate);
	}

	/**
	 * Set the JNDI environment to use for JNDI lookups.
	 * @see org.springframework.jndi.JndiAccessor#setJndiEnvironment
	 */
	public void setJndiEnvironment(Properties jndiEnvironment) {
		this.jndiLocator.setJndiEnvironment(jndiEnvironment);
	}

	/**
	 * Set whether the lookup occurs in a Jakarta EE container, i.e. if the prefix
	 * "java:comp/env/" needs to be added if the JNDI name doesn't already
	 * contain it. PersistenceAnnotationBeanPostProcessor's default is "true".
	 * @see org.springframework.jndi.JndiLocatorSupport#setResourceRef
	 */
	public void setResourceRef(boolean resourceRef) {
		this.jndiLocator.setResourceRef(resourceRef);
	}

	/**
	 * Specify a JNDI name of the {@link java.util.concurrent.Executor} to delegate to,
	 * replacing the default JNDI name "java:comp/DefaultManagedScheduledExecutorService".
	 * <p>This can either be a fully qualified JNDI name, or the JNDI name relative
	 * to the current environment naming context if "resourceRef" is set to "true".
	 * @see #setConcurrentExecutor
	 * @see #setResourceRef
	 */
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		ScheduledExecutorService executor = this.jndiLocator.lookup(this.jndiName, ScheduledExecutorService.class);
		setConcurrentExecutor(executor);
		setScheduledExecutor(executor);
	}

}
