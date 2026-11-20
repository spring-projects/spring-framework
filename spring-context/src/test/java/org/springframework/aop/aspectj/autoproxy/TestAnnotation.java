/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.autoproxy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Adrian Colyer
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@interface TestAnnotation {
	String value() ;
}
