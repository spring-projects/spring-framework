/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.annotation.subpackage;

/**
 * Subclass of {@link PackagePrivateMethodSuperclass} in the same package which
 * overrides the package-private method with another package-private method,
 * which therefore cannot be overridden by subclasses in different packages.
 *
 * @author Sam Brannen
 * @since 7.1
 */
public class PackagePrivateMethodOverridingSubclass extends PackagePrivateMethodSuperclass {

	@Override
	void process(String value) {
	}

}
