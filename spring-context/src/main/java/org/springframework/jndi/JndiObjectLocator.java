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

package org.springframework.jndi;

import javax.naming.NamingException;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Convenient superclass for JNDI-based service locators,
 * providing configurable lookup of a specific JNDI resource.
 *
 * <p>Exposes a {@link #setJndiName "jndiName"} property. This may or may not
 * include the "java:comp/env/" prefix expected by Jakarta EE applications when
 * accessing a locally mapped (Environmental Naming Context) resource. If it
 * doesn't, the "java:comp/env/" prefix will be prepended if the "resourceRef"
 * property is true (the default is <strong>false</strong>) and no other scheme
 * (for example, "java:") is given.
 *
 * <p>Subclasses may invoke the {@link #lookup()} method whenever it is appropriate.
 * Some classes might do this on initialization, while others might do it
 * on demand. The latter strategy is more flexible in that it allows for
 * initialization of the locator before the JNDI object is available.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setJndiName
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 * @see #setResourceRef
 * @see #lookup()
 */
public abstract class JndiObjectLocator extends JndiLocatorSupport implements InitializingBean {

	private @Nullable String jndiName;

	private @Nullable Class<?> expectedType;


	/**
	 * Specify the JNDI name to look up. If it doesn't begin with "java:comp/env/"
	 * this prefix is added automatically if "resourceRef" is set to "true".
	 * @param jndiName the JNDI name to look up
	 * @see #setResourceRef
	 */
	public void setJndiName(@Nullable String jndiName) {
		this.jndiName = jndiName;
	}

	/**
	 * Return the JNDI name to look up.
	 */
	public @Nullable String getJndiName() {
		return this.jndiName;
	}

	/**
	 * Specify the type that the located JNDI object is supposed
	 * to be assignable to, if any.
	 */
	public void setExpectedType(@Nullable Class<?> expectedType) {
		this.expectedType = expectedType;
	}

	/**
	 * Return the type that the located JNDI object is supposed
	 * to be assignable to, if any.
	 */
	public @Nullable Class<?> getExpectedType() {
		return this.expectedType;
	}

	@Override
	public void afterPropertiesSet() throws IllegalArgumentException, NamingException {
		if (!StringUtils.hasLength(getJndiName())) {
			throw new IllegalArgumentException("Property 'jndiName' is required");
		}
	}


	/**
	 * Perform the actual JNDI lookup for this locator's target resource.
	 * @return the located target object
	 * @throws NamingException if the JNDI lookup failed or if the
	 * located JNDI object is not assignable to the expected type
	 * @see #setJndiName
	 * @see #setExpectedType
	 * @see #lookup(String, Class)
	 */
	protected Object lookup() throws NamingException {
		String jndiName = getJndiName();
		Assert.state(jndiName != null, "No JNDI name specified");
		return lookup(jndiName, getExpectedType());
	}

}
