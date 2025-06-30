/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scheduling.concurrent;

import java.util.Properties;
import java.util.concurrent.ThreadFactory;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiTemplate;

/**
 * JNDI-based variant of {@link CustomizableThreadFactory}, performing a default lookup
 * for JSR-236's "java:comp/DefaultManagedThreadFactory" in a Jakarta EE environment,
 * falling back to the local {@link CustomizableThreadFactory} setup if not found.
 *
 * <p>This is a convenient way to use managed threads when running in a Jakarta EE
 * environment, simply using regular local threads otherwise - without conditional
 * setup (i.e. without profiles).
 *
 * <p>Note: This class is not strictly JSR-236 based; it can work with any regular
 * {@link java.util.concurrent.ThreadFactory} that can be found in JNDI. Therefore,
 * the default JNDI name "java:comp/DefaultManagedThreadFactory" can be customized
 * through the {@link #setJndiName "jndiName"} bean property.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
public class DefaultManagedAwareThreadFactory extends CustomizableThreadFactory implements InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private final JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();

	private @Nullable String jndiName = "java:comp/DefaultManagedThreadFactory";

	private @Nullable ThreadFactory threadFactory;


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
	 * Specify a JNDI name of the {@link java.util.concurrent.ThreadFactory} to delegate to,
	 * replacing the default JNDI name "java:comp/DefaultManagedThreadFactory".
	 * <p>This can either be a fully qualified JNDI name, or the JNDI name relative
	 * to the current environment naming context if "resourceRef" is set to "true".
	 * @see #setResourceRef
	 */
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		if (this.jndiName != null) {
			try {
				this.threadFactory = this.jndiLocator.lookup(this.jndiName, ThreadFactory.class);
			}
			catch (NamingException ex) {
				if (logger.isTraceEnabled()) {
					logger.trace("Failed to retrieve [" + this.jndiName + "] from JNDI", ex);
				}
				logger.info("Could not find default managed thread factory in JNDI - " +
						"proceeding with default local thread factory");
			}
		}
	}


	@Override
	public Thread newThread(Runnable runnable) {
		if (this.threadFactory != null) {
			return this.threadFactory.newThread(runnable);
		}
		else {
			return super.newThread(runnable);
		}
	}

}
