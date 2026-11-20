/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.annotation;

import org.springframework.util.Assert;

/**
 * Implementation of {@link ProfileValueSource} which uses system properties as
 * the underlying source.
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 2.0
 * @deprecated since Spring Framework 7.0 in favor of the
 * {@link org.springframework.test.context.junit.jupiter.SpringExtension SpringExtension}
 * and JUnit Jupiter
 */
@Deprecated(since = "7.0")
public final class SystemProfileValueSource implements ProfileValueSource {

	private static final SystemProfileValueSource INSTANCE = new SystemProfileValueSource();


	/**
	 * Obtain the canonical instance of this ProfileValueSource.
	 */
	public static SystemProfileValueSource getInstance() {
		return INSTANCE;
	}


	/**
	 * Private constructor, enforcing the singleton pattern.
	 */
	private SystemProfileValueSource() {
	}

	/**
	 * Get the <em>profile value</em> indicated by the specified key from the
	 * system properties.
	 * @see System#getProperty(String)
	 */
	@Override
	public String get(String key) {
		Assert.hasText(key, "'key' must not be empty");
		return System.getProperty(key);
	}

}
