/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.context.testfixture.jndi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Copy of the standard {@link org.springframework.mock.jndi.SimpleNamingContextBuilder}
 * for testing purposes.
 *
 * <p>Simple implementation of a JNDI naming context builder.
 *
 * <p>Mainly targeted at test environments, where each test case can
 * configure JNDI appropriately, so that {@code new InitialContext()}
 * will expose the required objects. Also usable for standalone applications,
 * e.g. for binding a JDBC DataSource to a well-known JNDI location, to be
 * able to use traditional Jakarta EE data access code outside a Jakarta EE
 * container.
 *
 * <p>There are various choices for DataSource implementations:
 * <ul>
 * <li>{@code SingleConnectionDataSource} (using the same Connection for all getConnection calls)
 * <li>{@code DriverManagerDataSource} (creating a new Connection on each getConnection call)
 * <li>Apache's Commons DBCP offers {@code org.apache.commons.dbcp.BasicDataSource} (a real pool)
 * </ul>
 *
 * <p>Typical usage in bootstrap code:
 *
 * <pre class="code">
 * SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
 * DataSource ds = new DriverManagerDataSource(...);
 * builder.bind("java:comp/env/jdbc/myds", ds);
 * builder.activate();</pre>
 *
 * Note that it's impossible to activate multiple builders within the same JVM,
 * due to JNDI restrictions. Thus, to configure a fresh builder repeatedly, use
 * the following code to get a reference to either an already activated builder
 * or a newly activated one:
 *
 * <pre class="code">
 * SimpleNamingContextBuilder builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
 * DataSource ds = new DriverManagerDataSource(...);
 * builder.bind("java:comp/env/jdbc/myds", ds);</pre>
 *
 * Note that you <i>should not</i> call {@code activate()} on a builder from
 * this factory method, as there will already be an activated one in any case.
 *
 * <p>An instance of this class is only necessary at setup time.
 * An application does not need to keep a reference to it after activation.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @see #emptyActivatedContextBuilder()
 * @see #bind(String, Object)
 * @see #activate()
 * @see SimpleNamingContext
 * @see org.springframework.jdbc.datasource.SingleConnectionDataSource
 * @see org.springframework.jdbc.datasource.DriverManagerDataSource
 */
public class SimpleNamingContextBuilder implements InitialContextFactoryBuilder {

	/** An instance of this class bound to JNDI. */
	@Nullable
	private static volatile SimpleNamingContextBuilder activated;

	private static boolean initialized = false;

	private static final Object initializationLock = new Object();


	/**
	 * Checks if a SimpleNamingContextBuilder is active.
	 * @return the current SimpleNamingContextBuilder instance,
	 * or {@code null} if none
	 */
	@Nullable
	public static SimpleNamingContextBuilder getCurrentContextBuilder() {
		return activated;
	}

	/**
	 * If no SimpleNamingContextBuilder is already configuring JNDI,
	 * create and activate one. Otherwise, take the existing activated
	 * SimpleNamingContextBuilder, clear it and return it.
	 * <p>This is mainly intended for test suites that want to
	 * reinitialize JNDI bindings from scratch repeatedly.
	 * @return an empty SimpleNamingContextBuilder that can be used
	 * to control JNDI bindings
	 */
	public static SimpleNamingContextBuilder emptyActivatedContextBuilder() throws NamingException {
		SimpleNamingContextBuilder builder = activated;
		if (builder != null) {
			// Clear already activated context builder.
			builder.clear();
		}
		else {
			// Create and activate new context builder.
			builder = new SimpleNamingContextBuilder();
			// The activate() call will cause an assignment to the activated variable.
			builder.activate();
		}
		return builder;
	}


	private final Log logger = LogFactory.getLog(getClass());

	private final Hashtable<String,Object> boundObjects = new Hashtable<>();


	/**
	 * Register the context builder by registering it with the JNDI NamingManager.
	 * Note that once this has been done, {@code new InitialContext()} will always
	 * return a context from this factory. Use the {@code emptyActivatedContextBuilder()}
	 * static method to get an empty context (for example, in test methods).
	 * @throws IllegalStateException if there's already a naming context builder
	 * registered with the JNDI NamingManager
	 */
	public void activate() throws IllegalStateException, NamingException {
		logger.info("Activating simple JNDI environment");
		synchronized (initializationLock) {
			if (!initialized) {
				Assert.state(!NamingManager.hasInitialContextFactoryBuilder(),
							"Cannot activate SimpleNamingContextBuilder: there is already a JNDI provider registered. " +
							"Note that JNDI is a JVM-wide service, shared at the JVM system class loader level, " +
							"with no reset option. As a consequence, a JNDI provider must only be registered once per JVM.");
				NamingManager.setInitialContextFactoryBuilder(this);
				initialized = true;
			}
		}
		activated = this;
	}

	/**
	 * Temporarily deactivate this context builder. It will remain registered with
	 * the JNDI NamingManager but will delegate to the standard JNDI InitialContextFactory
	 * (if configured) instead of exposing its own bound objects.
	 * <p>Call {@code activate()} again in order to expose this context builder's own
	 * bound objects again. Such activate/deactivate sequences can be applied any number
	 * of times (e.g. within a larger integration test suite running in the same VM).
	 * @see #activate()
	 */
	public void deactivate() {
		logger.info("Deactivating simple JNDI environment");
		activated = null;
	}

	/**
	 * Clear all bindings in this context builder, while keeping it active.
	 */
	public void clear() {
		this.boundObjects.clear();
	}

	/**
	 * Bind the given object under the given name, for all naming contexts
	 * that this context builder will generate.
	 * @param name the JNDI name of the object (e.g. "java:comp/env/jdbc/myds")
	 * @param obj the object to bind (e.g. a DataSource implementation)
	 */
	public void bind(String name, Object obj) {
		if (logger.isInfoEnabled()) {
			logger.info("Static JNDI binding: [" + name + "] = [" + obj + "]");
		}
		this.boundObjects.put(name, obj);
	}


	/**
	 * Simple InitialContextFactoryBuilder implementation,
	 * creating a new SimpleNamingContext instance.
	 * @see SimpleNamingContext
	 */
	@Override
	@SuppressWarnings("unchecked")
	public InitialContextFactory createInitialContextFactory(@Nullable Hashtable<?,?> environment) {
		if (activated == null && environment != null) {
			Object icf = environment.get(Context.INITIAL_CONTEXT_FACTORY);
			if (icf != null) {
				Class<?> icfClass;
				if (icf instanceof Class) {
					icfClass = (Class<?>) icf;
				}
				else if (icf instanceof String) {
					icfClass = ClassUtils.resolveClassName((String) icf, getClass().getClassLoader());
				}
				else {
					throw new IllegalArgumentException("Invalid value type for environment key [" +
							Context.INITIAL_CONTEXT_FACTORY + "]: " + icf.getClass().getName());
				}
				if (!InitialContextFactory.class.isAssignableFrom(icfClass)) {
					throw new IllegalArgumentException(
							"Specified class does not implement [" + InitialContextFactory.class.getName() + "]: " + icf);
				}
				try {
					return (InitialContextFactory) ReflectionUtils.accessibleConstructor(icfClass).newInstance();
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Unable to instantiate specified InitialContextFactory: " + icf, ex);
				}
			}
		}

		// Default case...
		return env -> new SimpleNamingContext("", this.boundObjects, (Hashtable<String, Object>) env);
	}

}
