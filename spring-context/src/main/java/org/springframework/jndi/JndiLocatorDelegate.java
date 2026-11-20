/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jndi;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jspecify.annotations.Nullable;

import org.springframework.core.SpringProperties;

/**
 * {@link JndiLocatorSupport} subclass with public lookup methods,
 * for convenient use as a delegate.
 *
 * @author Juergen Hoeller
 * @since 3.0.1
 */
public class JndiLocatorDelegate extends JndiLocatorSupport {

	/**
	 * System property that instructs Spring to ignore a default JNDI environment, i.e.
	 * to always return {@code false} from {@link #isDefaultJndiEnvironmentAvailable()}.
	 * <p>The default is "false", allowing for regular default JNDI access, for example, in
	 * {@link JndiPropertySource}. Switching this flag to {@code true} is an optimization
	 * for scenarios where nothing is ever to be found for such JNDI fallback searches
	 * to begin with, avoiding the repeated JNDI lookup overhead.
	 * <p>Note that this flag just affects JNDI fallback searches, not explicitly configured
	 * JNDI lookups such as for a {@code DataSource} or some other environment resource.
	 * The flag literally just affects code which attempts JNDI searches based on the
	 * {@code JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable()} check: in particular,
	 * {@code StandardServletEnvironment}.
	 * @since 4.3
	 * @see #isDefaultJndiEnvironmentAvailable()
	 * @see JndiPropertySource
	 */
	public static final String IGNORE_JNDI_PROPERTY_NAME = "spring.jndi.ignore";


	private static final boolean shouldIgnoreDefaultJndiEnvironment =
			SpringProperties.getFlag(IGNORE_JNDI_PROPERTY_NAME);


	@Override
	public Object lookup(String jndiName) throws NamingException {
		return super.lookup(jndiName);
	}

	@Override
	public <T> T lookup(String jndiName, @Nullable Class<T> requiredType) throws NamingException {
		return super.lookup(jndiName, requiredType);
	}


	/**
	 * Configure a {@code JndiLocatorDelegate} with its "resourceRef" property set to
	 * {@code true}, meaning that all names will be prefixed with "java:comp/env/".
	 * @see #setResourceRef
	 */
	public static JndiLocatorDelegate createDefaultResourceRefLocator() {
		JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();
		jndiLocator.setResourceRef(true);
		return jndiLocator;
	}

	/**
	 * Check whether a default JNDI environment, as in a Jakarta EE environment,
	 * is available on this JVM.
	 * @return {@code true} if a default InitialContext can be used,
	 * {@code false} if not
	 */
	public static boolean isDefaultJndiEnvironmentAvailable() {
		if (shouldIgnoreDefaultJndiEnvironment) {
			return false;
		}
		try {
			new InitialContext().getEnvironment();
			return true;
		}
		catch (Throwable ex) {
			return false;
		}
	}

}
