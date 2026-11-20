/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.util.subpackage;

/**
 * Interface representing a <em>person</em> entity; intended for use in unit tests.
 *
 * <p>The introduction of an interface is necessary in order to test support for
 * JDK dynamic proxies.
 *
 * @author Sam Brannen
 * @since 4.3
 */
public interface Person {

	long getId();

	String getName();

	int getAge();

	String getEyeColor();

	boolean likesPets();

	Number getFavoriteNumber();

}
