/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * This class is only used within tests in the spring-orm module.
 *
 * <p>Superclass for JUnit 3.8 test cases using Spring
 * {@link org.springframework.context.ApplicationContext ApplicationContexts}.
 *
 * <p>Maintains a static cache of contexts by key. This has significant performance
 * benefit if initializing the context would take time. While initializing a
 * Spring context itself is very quick, some beans in a context, such as a
 * LocalSessionFactoryBean for working with Hibernate, may take some time to
 * initialize. Hence it often makes sense to do that initializing once.
 *
 * <p>Any ApplicationContext created by this class will be asked to register a JVM
 * shutdown hook for itself. Unless the context gets closed early, all context
 * instances will be automatically closed on JVM shutdown. This allows for
 * freeing external resources held by beans within the context, e.g. temporary
 * files.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.1.1
 * @see AbstractSingleSpringContextTests
 * @see AbstractDependencyInjectionSpringContextTests
 * @see AbstractTransactionalSpringContextTests
 * @see AbstractTransactionalDataSourceSpringContextTests
 * @deprecated as of Spring 3.0, in favor of using the listener-based test context framework
 * ({@link org.springframework.test.context.junit38.AbstractJUnit38SpringContextTests})
 */
@Deprecated
abstract class AbstractSpringContextTests extends TestCase {

	protected final Log logger = LogFactory.getLog(getClass());

	private static Map<String, ConfigurableApplicationContext> contextKeyToContextMap =
			new HashMap<String, ConfigurableApplicationContext>();


	/**
	 * Explicitly add an ApplicationContext instance under a given key.
	 * <p>This is not meant to be used by subclasses. It is rather exposed for
	 * special test suite environments.
	 * @param locations the context key
	 * @param context the ApplicationContext instance
	 */
	public final void addContext(String[] locations, ConfigurableApplicationContext context) {
		Assert.notNull(context, "ApplicationContext must not be null");
		contextKeyToContextMap.put(contextKey(locations), context);
	}

	/**
	 * Obtain an ApplicationContext for the given key, potentially cached.
	 * @param locations the context key; may be {@code null}.
	 * @return the corresponding ApplicationContext instance (potentially cached),
	 * or {@code null} if the provided {@code key} is <em>empty</em>
	 */
	protected final ConfigurableApplicationContext getContext(String... locations) throws Exception {
		if (ObjectUtils.isEmpty(locations)) {
			return null;
		}
		String key = contextKey(locations);
		ConfigurableApplicationContext ctx = contextKeyToContextMap.get(key);
		if (ctx == null) {
			ctx = loadContext(locations);
			ctx.registerShutdownHook();
			contextKeyToContextMap.put(key, ctx);
		}
		return ctx;
	}

	private final String contextKey(String... locations) {
		return ObjectUtils.nullSafeToString(locations);
	}

	/**
	 * Load a new ApplicationContext for the given key.
	 * <p>To be implemented by subclasses.
	 * @param key the context key
	 * @return the corresponding ApplicationContext instance (new)
	 */
	protected abstract ConfigurableApplicationContext loadContext(String... locations) throws Exception;

}
