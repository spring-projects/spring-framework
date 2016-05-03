/*
 * Copyright 2002-2013 the original author or authors.
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

	private static Map<ClassLoader, WebApplicationContext> currentContextPerThread = getCurrentContextPerThreadFromContextLoader();

	public static void setCurrentWebApplicationContext(WebApplicationContext applicationContext) {
		setCurrentWebApplicationContext(Thread.currentThread().getContextClassLoader(), applicationContext);
	}

	public static void setCurrentWebApplicationContext(ClassLoader classLoader, WebApplicationContext applicationContext) {
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
