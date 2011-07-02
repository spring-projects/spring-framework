/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.springframework.core.env.PropertySource;

/**
 * {@link PropertySource} implementation that reads properties from a JNDI
 * {@link Context}. All properties retrieved through {@link #getProperty(String)} will
 * automatically be prefixed with "java:comp/env/" when executing the actual
 * {@link Context#lookup(String)} call. This default can be overridden using
 * {@link #setJndiPrefix(String)} property.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see Context#lookup(String)
 * @see org.springframework.web.context.support.StandardServletEnvironment
 */
public class JndiPropertySource extends PropertySource<Context> {

	/** JNDI context property source name: {@value} */
	public static final String JNDI_PROPERTY_SOURCE_NAME = "jndiPropertySource";

	/**
	 * Name of property used to determine if a {@link JndiPropertySource}
	 * should be registered by default: {@value}
	 */
	public static final String JNDI_PROPERTY_SOURCE_ENABLED_FLAG = "jndiPropertySourceEnabled";

	/**
	 * Create a new {@code JndiPropertySource} with the default name
	 * {@value #JNDI_PROPERTY_SOURCE_NAME} and create a new {@link InitialContext}
	 * as the source object.
	 * @throws JndiLookupFailureException if a new {@link InitialContext}
	 * cannot be created.
	 */
	public JndiPropertySource() throws JndiLookupFailureException {
		this(JNDI_PROPERTY_SOURCE_NAME, createInitialContext(null));
	}

	/**
	 * Create a new {@code JndiPropertySource} with the given name and
	 * create a new {@link InitialContext} as the source object.
	 * @throws JndiLookupFailureException if a new {@link InitialContext}
	 * cannot be created.
	 */
	public JndiPropertySource(String name) throws JndiLookupFailureException {
		this(name, createInitialContext(null));
	}

	/**
	 * Create a new {@code JndiPropertySource} with the given name and
	 * use the given jndiEnvironment properties to create a new
	 * {@link InitialContext} as the source object.
	 * @throws JndiLookupFailureException if a new {@link InitialContext}
	 * cannot be created.
	 */
	public JndiPropertySource(String name, Properties jndiEnvironment) throws JndiLookupFailureException {
		this(name, createInitialContext(jndiEnvironment));
	}

	/**
	 * Create a new {@code JndiPropertySource} with the given name and
	 * JNDI {@link Context}.
	 */
	public JndiPropertySource(String name, Context source) {
		super(name, source);
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation looks up and returns the given name from the source JNDI
	 * {@link Context}. If a {@link NamingException} is thrown during the call to
	 * {@link Context#lookup(String)}, returns {@code null} and issue a DEBUG-level log
	 * statement with the exception message.
	 */
	@Override
	public Object getProperty(String name) {
		try {
			Object value = this.source.lookup(name);
			logger.debug("Context#lookup(" + name + ") returned: [" + value + "]");
			return value;
		} catch (NamingException ex) {
			logger.debug("Context#lookup(" + name + ") threw NamingException with message: " + ex.getMessage());
			return null;
		}
	}

	private static Context createInitialContext(Properties jndiEnvironment) {
		try {
			return new InitialContext(jndiEnvironment);
		} catch (NamingException ex) {
			throw new JndiLookupFailureException("unable to create InitialContext", ex);
		}
	}

}
