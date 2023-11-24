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

package org.springframework.test.context.aot;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Utilities for loading generated maps.
 *
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 6.0
 */
final class GeneratedMapUtils {

	private GeneratedMapUtils() {
	}

	/**
	 * Load a generated map.
	 * @param className the name of the class in which the static method resides
	 * @param methodName the name of the static method to invoke
	 * @return an unmodifiable map retrieved from a static method
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static Map loadMap(String className, String methodName) {
		try {
			Class<?> clazz = ClassUtils.forName(className, null);
			Method method = ReflectionUtils.findMethod(clazz, methodName);
			Assert.state(method != null, () -> "No %s() method found in %s".formatted(methodName, className));
			Map map = (Map) ReflectionUtils.invokeMethod(method, null);
			return Collections.unmodifiableMap(map);
		}
		catch (IllegalStateException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to invoke %s() method on %s".formatted(methodName, className), ex);
		}
	}

}
