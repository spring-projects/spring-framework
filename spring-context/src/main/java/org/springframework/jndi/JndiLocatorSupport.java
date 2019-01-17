/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jndi;

import javax.naming.NamingException;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Convenient superclass for classes that can locate any number of JNDI objects.
 * Derives from JndiAccessor to inherit the "jndiTemplate" and "jndiEnvironment"
 * bean properties.
 *
 * <p>JNDI names may or may not include the "java:comp/env/" prefix expected
 * by Java EE applications when accessing a locally mapped (ENC - Environmental
 * Naming Context) resource. If it doesn't, the "java:comp/env/" prefix will
 * be prepended if the "resourceRef" property is true (the default is
 * <strong>false</strong>) and no other scheme (e.g. "java:") is given.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 * @see #setResourceRef
 */
public abstract class JndiLocatorSupport extends JndiAccessor {

	/** JNDI prefix used in a Java EE container. */
	public static final String CONTAINER_PREFIX = "java:comp/env/";


	private boolean resourceRef = false;


	/**
	 * Set whether the lookup occurs in a Java EE container, i.e. if the prefix
	 * "java:comp/env/" needs to be added if the JNDI name doesn't already
	 * contain it. Default is "false".
	 * <p>Note: Will only get applied if no other scheme (e.g. "java:") is given.
	 */
	public void setResourceRef(boolean resourceRef) {
		this.resourceRef = resourceRef;
	}

	/**
	 * Return whether the lookup occurs in a Java EE container.
	 */
	public boolean isResourceRef() {
		return this.resourceRef;
	}


	/**
	 * Perform an actual JNDI lookup for the given name via the JndiTemplate.
   * <p>If the name doesn't begin with "java:comp/env/", this prefix is added
	 * if "resourceRef" is set to "true".
	 * @param jndiName the JNDI name to look up
	 * @return the obtained object
	 * @throws NamingException if the JNDI lookup failed
	 * @see #setResourceRef
	 */
	protected Object lookup(String jndiName) throws NamingException {
		return lookup(jndiName, null);
	}

	/**
	 * Perform an actual JNDI lookup for the given name via the JndiTemplate.
	 * <p>If the name doesn't begin with "java:comp/env/", this prefix is added
	 * if "resourceRef" is set to "true".
	 * @param jndiName the JNDI name to look up
	 * @param requiredType the required type of the object
	 * @return the obtained object
	 * @throws NamingException if the JNDI lookup failed
	 * @see #setResourceRef
	 */
	protected <T> T lookup(String jndiName, @Nullable Class<T> requiredType) throws NamingException {
		Assert.notNull(jndiName, "'jndiName' must not be null");
		String convertedName = convertJndiName(jndiName);
		T jndiObject;
		try {
			jndiObject = getJndiTemplate().lookup(convertedName, requiredType);
		}
		catch (NamingException ex) {
			if (!convertedName.equals(jndiName)) {
				// Try fallback to originally specified name...
				if (logger.isDebugEnabled()) {
					logger.debug("Converted JNDI name [" + convertedName +
							"] not found - trying original name [" + jndiName + "]. " + ex);
				}
				jndiObject = getJndiTemplate().lookup(jndiName, requiredType);
			}
			else {
				throw ex;
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Located object with JNDI name [" + convertedName + "]");
		}
		return jndiObject;
	}

	/**
	 * Convert the given JNDI name into the actual JNDI name to use.
	 * <p>The default implementation applies the "java:comp/env/" prefix if
	 * "resourceRef" is "true" and no other scheme (e.g. "java:") is given.
	 * @param jndiName the original JNDI name
	 * @return the JNDI name to use
	 * @see #CONTAINER_PREFIX
	 * @see #setResourceRef
	 */
	protected String convertJndiName(String jndiName) {
		// Prepend container prefix if not already specified and no other scheme given.
		if (isResourceRef() && !jndiName.startsWith(CONTAINER_PREFIX) && jndiName.indexOf(':') == -1) {
			jndiName = CONTAINER_PREFIX + jndiName;
		}
		return jndiName;
	}

}
