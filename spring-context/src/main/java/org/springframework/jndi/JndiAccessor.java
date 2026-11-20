/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jndi;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

/**
 * Convenient superclass for JNDI accessors, providing "jndiTemplate"
 * and "jndiEnvironment" bean properties.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 */
public class JndiAccessor {

	/**
	 * Logger, available to subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	private JndiTemplate jndiTemplate = new JndiTemplate();


	/**
	 * Set the JNDI template to use for JNDI lookups.
	 * <p>You can also specify JNDI environment settings via "jndiEnvironment".
	 * @see #setJndiEnvironment
	 */
	public void setJndiTemplate(@Nullable JndiTemplate jndiTemplate) {
		this.jndiTemplate = (jndiTemplate != null ? jndiTemplate : new JndiTemplate());
	}

	/**
	 * Return the JNDI template to use for JNDI lookups.
	 */
	public JndiTemplate getJndiTemplate() {
		return this.jndiTemplate;
	}

	/**
	 * Set the JNDI environment to use for JNDI lookups.
	 * <p>Creates a JndiTemplate with the given environment settings.
	 * @see #setJndiTemplate
	 */
	public void setJndiEnvironment(@Nullable Properties jndiEnvironment) {
		this.jndiTemplate = new JndiTemplate(jndiEnvironment);
	}

	/**
	 * Return the JNDI environment to use for JNDI lookups.
	 */
	public @Nullable Properties getJndiEnvironment() {
		return this.jndiTemplate.getEnvironment();
	}

}
