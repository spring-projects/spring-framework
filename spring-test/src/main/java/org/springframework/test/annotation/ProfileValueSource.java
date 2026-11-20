/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.annotation;

import org.jspecify.annotations.Nullable;

/**
 * <p>
 * Strategy interface for retrieving <em>profile values</em> for a given
 * testing environment.
 * </p>
 * <p>
 * Concrete implementations must provide a {@code public} no-args
 * constructor.
 * </p>
 * <p>
 * Spring provides the following out-of-the-box implementations:
 * </p>
 * <ul>
 * <li>{@link SystemProfileValueSource}</li>
 * </ul>
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 2.0
 * @see ProfileValueSourceConfiguration
 * @see IfProfileValue
 * @see ProfileValueUtils
 * @deprecated since Spring Framework 7.0 in favor of the
 * {@link org.springframework.test.context.junit.jupiter.SpringExtension SpringExtension}
 * and JUnit Jupiter
 */
@Deprecated(since = "7.0")
public interface ProfileValueSource {

	/**
	 * Get the <em>profile value</em> indicated by the specified key.
	 * @param key the name of the <em>profile value</em>
	 * @return the String value of the <em>profile value</em>, or {@code null}
	 * if there is no <em>profile value</em> with that key
	 */
	@Nullable String get(String key);

}
