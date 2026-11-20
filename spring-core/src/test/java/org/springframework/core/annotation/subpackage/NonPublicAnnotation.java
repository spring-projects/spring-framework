/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.annotation.subpackage;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Non-public (i.e., package private) custom annotation.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@interface NonPublicAnnotation {

	int value() default -1;
}
