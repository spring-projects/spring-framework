/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.annotation.subpackage;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.core.annotation.AliasFor;

/**
 * Non-public mock of {@code org.springframework.web.bind.annotation.RequestMapping}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
@Retention(RetentionPolicy.RUNTIME)
@interface NonPublicAliasedAnnotation {

	String name();

	@AliasFor("path")
	String value() default "";

	@AliasFor("value")
	String path() default "";
}
