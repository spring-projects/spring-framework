/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.util;

import java.beans.Introspector;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.beans.CachedIntrospectionResults;

/**
 * Listener that flushes the JDK's {@link java.beans.Introspector JavaBeans Introspector}
 * cache on web app shutdown. Register this listener in your {@code web.xml} to
 * guarantee proper release of the web application class loader and its loaded classes.
 *
 * <p><b>If the JavaBeans Introspector has been used to analyze application classes,
 * the system-level Introspector cache will hold a hard reference to those classes.
 * Consequently, those classes and the web application class loader will not be
 * garbage-collected on web app shutdown!</b> This listener performs proper cleanup,
 * to allow for garbage collection to take effect.
 *
 * <p>Unfortunately, the only way to clean up the Introspector is to flush
 * the entire cache, as there is no way to specifically determine the
 * application's classes referenced there. This will remove cached
 * introspection results for all other applications in the server too.
 *
 * <p>Note that this listener is <i>not</i> necessary when using Spring's beans
 * infrastructure within the application, as Spring's own introspection results
 * cache will immediately flush an analyzed class from the JavaBeans Introspector
 * cache and only hold a cache within the application's own ClassLoader.
 *
 * <b>Although Spring itself does not create JDK Introspector leaks, note that this
 * listener should nevertheless be used in scenarios where the Spring framework classes
 * themselves reside in a 'common' ClassLoader (such as the system ClassLoader).</b>
 * In such a scenario, this listener will properly clean up Spring's introspection cache.
 *
 * <p>Application classes hardly ever need to use the JavaBeans Introspector
 * directly, so are normally not the cause of Introspector resource leaks.
 * Rather, many libraries and frameworks do not clean up the Introspector:
 * e.g. Struts and Quartz.
 *
 * <p>Note that a single such Introspector leak will cause the entire web
 * app class loader to not get garbage collected! This has the consequence that
 * you will see all the application's static class resources (like singletons)
 * around after web app shutdown, which is not the fault of those classes!
 *
 * <p><b>This listener should be registered as the first one in {@code web.xml},
 * before any application listeners such as Spring's ContextLoaderListener.</b>
 * This allows the listener to take full effect at the right time of the lifecycle.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see java.beans.Introspector#flushCaches()
 * @see org.springframework.beans.CachedIntrospectionResults#acceptClassLoader
 * @see org.springframework.beans.CachedIntrospectionResults#clearClassLoader
 */
public class IntrospectorCleanupListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		CachedIntrospectionResults.acceptClassLoader(Thread.currentThread().getContextClassLoader());
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		CachedIntrospectionResults.clearClassLoader(Thread.currentThread().getContextClassLoader());
		Introspector.flushCaches();
	}

}
