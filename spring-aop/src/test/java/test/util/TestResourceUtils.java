/*
 * Copyright 2002-2008 the original author or authors.
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

package test.util;

import static java.lang.String.format;

import org.springframework.core.io.ClassPathResource;

/**
 * Convenience utilities for common operations with test resources.
 *
 * @author Chris Beams
 */
public class TestResourceUtils {

	/**
	 * Loads a {@link ClassPathResource} qualified by the simple name of clazz,
	 * and relative to the package for clazz.
	 *
	 * <p>Example: given a clazz 'com.foo.BarTests' and a resourceSuffix of 'context.xml',
	 * this method will return a ClassPathResource representing com/foo/BarTests-context.xml
	 *
	 * <p>Intended for use loading context configuration XML files within JUnit tests.
	 *
	 * @param clazz
	 * @param resourceSuffix
	 */
	public static ClassPathResource qualifiedResource(Class<?> clazz, String resourceSuffix) {
		return new ClassPathResource(format("%s-%s", clazz.getSimpleName(), resourceSuffix), clazz);
	}

}
