/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.annotation.subpackage;

/**
 * Subclass of {@link PackagePrivateMethodSuperclass} in the same package which
 * overrides the package-private method with a {@code public} method, thereby
 * increasing the visibility of the method and allowing it to be overridden by
 * subclasses in different packages.
 *
 * @author Sam Brannen
 * @since 7.1
 */
public class PublicMethodOverridingSubclass extends PackagePrivateMethodSuperclass {

	@Override
	public void process(String value) {
	}

}
