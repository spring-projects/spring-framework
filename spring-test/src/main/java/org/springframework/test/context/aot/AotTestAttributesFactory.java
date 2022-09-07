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
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.aot.AotDetector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Factory for {@link AotTestAttributes}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
final class AotTestAttributesFactory {

	@Nullable
	private static volatile Map<String, String> attributes;


	private AotTestAttributesFactory() {
	}

	/**
	 * Get the underlying attributes map.
	 * <p>If the map is not already loaded, this method loads the map from the
	 * generated class when running in {@linkplain AotDetector#useGeneratedArtifacts()
	 * AOT execution mode} and otherwise creates a new map for storing attributes
	 * during the AOT processing phase.
	 */
	static Map<String, String> getAttributes() {
		Map<String, String> attrs = attributes;
		if (attrs == null) {
			synchronized (AotTestAttributesFactory.class) {
				attrs = attributes;
				if (attrs == null) {
					attrs = (AotDetector.useGeneratedArtifacts() ? loadAttributesMap() : new ConcurrentHashMap<>());
					attributes = attrs;
				}
			}
		}
		return attrs;
	}

	/**
	 * Reset AOT test attributes.
	 * <p>Only for internal use.
	 */
	static void reset() {
		synchronized (AotTestAttributesFactory.class) {
			attributes = null;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Map<String, String> loadAttributesMap() {
		String className = AotTestAttributesCodeGenerator.GENERATED_ATTRIBUTES_CLASS_NAME;
		String methodName = AotTestAttributesCodeGenerator.GENERATED_ATTRIBUTES_METHOD_NAME;
		try {
			Class<?> clazz = ClassUtils.forName(className, null);
			Method method = ReflectionUtils.findMethod(clazz, methodName);
			Assert.state(method != null, () -> "No %s() method found in %s".formatted(methodName, clazz.getName()));
			Map<String, String> attributes = (Map<String, String>) ReflectionUtils.invokeMethod(method, null);
			return Collections.unmodifiableMap(attributes);
		}
		catch (IllegalStateException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to invoke %s() method on %s".formatted(methodName, className), ex);
		}
	}

}
