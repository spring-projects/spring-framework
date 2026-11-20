/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket;

import java.lang.reflect.Field;
import java.util.Map;

import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

/**
 * General test utilities for manipulating the {@link ContextLoader}.
 *
 * @author Phillip Webb
 */
public class ContextLoaderTestUtils {

	private static Map<ClassLoader, WebApplicationContext> currentContextPerThread =
			getCurrentContextPerThreadFromContextLoader();

	public static void setCurrentWebApplicationContext(WebApplicationContext applicationContext) {
		setCurrentWebApplicationContext(Thread.currentThread().getContextClassLoader(), applicationContext);
	}

	public static void setCurrentWebApplicationContext(ClassLoader classLoader,
			WebApplicationContext applicationContext) {

		if (applicationContext != null) {
			currentContextPerThread.put(classLoader, applicationContext);
		}
		else {
			currentContextPerThread.remove(classLoader);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<ClassLoader, WebApplicationContext> getCurrentContextPerThreadFromContextLoader() {
		try {
			Field field = ContextLoader.class.getDeclaredField("currentContextPerThread");
			field.setAccessible(true);
			return (Map<ClassLoader, WebApplicationContext>) field.get(null);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
