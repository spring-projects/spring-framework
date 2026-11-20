/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.annotation.subpackage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class that declares an annotated package-private method which cannot be
 * overridden directly by a subclass in a different package.
 *
 * @author Sam Brannen
 * @since 7.1
 */
public class PackagePrivateMethodSuperclass {

	@Marker
	void process(@Marker String value) {
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.PARAMETER})
	public @interface Marker {
	}

}
