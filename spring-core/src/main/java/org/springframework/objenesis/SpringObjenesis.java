/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.objenesis;

import org.springframework.core.SpringProperties;
import org.springframework.objenesis.instantiator.ObjectInstantiator;
import org.springframework.objenesis.strategy.InstantiatorStrategy;
import org.springframework.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Spring-specific variant of {@link ObjenesisStd} / {@link ObjenesisBase},
 * providing a cache based on {@code Class} keys instead of class names,
 * and allowing for selective use of the cache.
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see #isWorthTrying()
 * @see #newInstance(Class, boolean)
 */
public class SpringObjenesis implements Objenesis {

	/**
	 * System property that instructs Spring to ignore Objenesis, not even attempting
	 * to use it. Setting this flag to "true" is equivalent to letting Spring find
	 * out that Objenesis isn't working at runtime, triggering the fallback code path
	 * immediately: Most importantly, this means that all CGLIB AOP proxies will be
	 * created through regular instantiation via a default constructor.
	 */
	public static final String IGNORE_OBJENESIS_PROPERTY_NAME = "spring.objenesis.ignore";


	private final InstantiatorStrategy strategy;

	private final ConcurrentReferenceHashMap<Class<?>, ObjectInstantiator<?>> cache =
			new ConcurrentReferenceHashMap<Class<?>, ObjectInstantiator<?>>();

	private volatile Boolean worthTrying;


	/**
	 * Create a new {@code SpringObjenesis} instance with the
	 * standard instantiator strategy.
	 */
	public SpringObjenesis() {
		this(null);
	}

	/**
	 * Create a new {@code SpringObjenesis} instance with the
	 * given standard instantiator strategy.
	 * @param strategy the instantiator strategy to use
	 */
	public SpringObjenesis(InstantiatorStrategy strategy) {
		this.strategy = (strategy != null ? strategy : new StdInstantiatorStrategy());

		// Evaluate the "spring.objenesis.ignore" property upfront...
		if (SpringProperties.getFlag(SpringObjenesis.IGNORE_OBJENESIS_PROPERTY_NAME)) {
			this.worthTrying = Boolean.FALSE;
		}
	}


	/**
	 * Return whether this Objenesis instance is worth trying for instance creation,
	 * i.e. whether it hasn't been used yet or is known to work.
	 * <p>If the configured Objenesis instantiator strategy has been identified to not
	 * work on the current JVM at all or if the "spring.objenesis.ignore" property has
	 * been set to "true", this method returns {@code false}.
	 */
	public boolean isWorthTrying() {
		return (this.worthTrying != Boolean.FALSE);
	}

	/**
	 * Create a new instance of the given class via Objenesis.
	 * @param clazz the class to create an instance of
	 * @param useCache whether to use the instantiator cache
	 * (typically {@code true} but can be set to {@code false}
	 * e.g. for reloadable classes)
	 * @return the new instance (never {@code null})
	 * @throws ObjenesisException if instance creation failed
	 */
	public <T> T newInstance(Class<T> clazz, boolean useCache) {
		if (!useCache) {
			return newInstantiatorOf(clazz).newInstance();
		}
		return getInstantiatorOf(clazz).newInstance();
	}

	public <T> T newInstance(Class<T> clazz) {
		return getInstantiatorOf(clazz).newInstance();
	}

	@SuppressWarnings("unchecked")
	public <T> ObjectInstantiator<T> getInstantiatorOf(Class<T> clazz) {
		ObjectInstantiator<?> instantiator = this.cache.get(clazz);
		if (instantiator == null) {
			ObjectInstantiator<T> newInstantiator = newInstantiatorOf(clazz);
			instantiator = this.cache.putIfAbsent(clazz, newInstantiator);
			if (instantiator == null) {
				instantiator = newInstantiator;
			}
		}
		return (ObjectInstantiator<T>) instantiator;
	}

	protected <T> ObjectInstantiator<T> newInstantiatorOf(Class<T> clazz) {
		Boolean currentWorthTrying = this.worthTrying;
		try {
			ObjectInstantiator<T> instantiator = this.strategy.newInstantiatorOf(clazz);
			if (currentWorthTrying == null) {
				this.worthTrying = Boolean.TRUE;
			}
			return instantiator;
		}
		catch (ObjenesisException ex) {
			if (currentWorthTrying == null) {
				Throwable cause = ex.getCause();
				if (cause instanceof ClassNotFoundException || cause instanceof IllegalAccessException) {
					// Indicates that the chosen instantiation strategy does not work on the given JVM.
					// Typically a failure to initialize the default SunReflectionFactoryInstantiator.
					// Let's assume that any subsequent attempts to use Objenesis will fail as well...
					this.worthTrying = Boolean.FALSE;
				}
			}
			throw ex;
		}
		catch (NoClassDefFoundError err) {
			// Happening on the production version of Google App Engine, coming out of the
			// restricted "sun.reflect.ReflectionFactory" class...
			if (currentWorthTrying == null) {
				this.worthTrying = Boolean.FALSE;
			}
			throw new ObjenesisException(err);
		}
	}

}
