/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.testfixture.io;

import org.springframework.core.io.ClassPathResource;

/**
 * Convenience utilities for common operations with test resources.
 *
 * @author Chris Beams
 */
public abstract class ResourceTestUtils {

	/**
	 * Load a {@link ClassPathResource} qualified by the simple name of clazz,
	 * and relative to the package for clazz.
	 * <p>Example: given a clazz 'com.foo.BarTests' and a resourceSuffix of 'context.xml',
	 * this method will return a ClassPathResource representing com/foo/BarTests-context.xml
	 * <p>Intended for use loading context configuration XML files within JUnit tests.
	 */
	public static ClassPathResource qualifiedResource(Class<?> clazz, String resourceSuffix) {
		return new ClassPathResource(String.format("%s-%s", clazz.getSimpleName(), resourceSuffix), clazz);
	}

}
