/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.testfixture.env;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import org.springframework.core.env.StandardEnvironment;

/**
 * Test utilities for {@link StandardEnvironment}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class EnvironmentTestUtils {

	@SuppressWarnings("unchecked")
	public static Map<String, String> getModifiableSystemEnvironment() {
		// for os x / linux
		Class<?>[] classes = Collections.class.getDeclaredClasses();
		Map<String, String> env = System.getenv();
		for (Class<?> cl : classes) {
			if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
				try {
					Field field = cl.getDeclaredField("m");
					field.setAccessible(true);
					Object obj = field.get(env);
					if (obj != null && obj.getClass().getName().equals("java.lang.ProcessEnvironment$StringEnvironment")) {
						return (Map<String, String>) obj;
					}
				}
				catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		}

		// for windows
		Class<?> processEnvironmentClass;
		try {
			processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

		try {
			Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
			theCaseInsensitiveEnvironmentField.setAccessible(true);
			Object obj = theCaseInsensitiveEnvironmentField.get(null);
			return (Map<String, String>) obj;
		}
		catch (NoSuchFieldException ex) {
			// do nothing
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

		try {
			Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
			theEnvironmentField.setAccessible(true);
			Object obj = theEnvironmentField.get(null);
			return (Map<String, String>) obj;
		}
		catch (NoSuchFieldException ex) {
			// do nothing
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

		throw new IllegalStateException();
	}

}
