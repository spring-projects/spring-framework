/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.autoproxy;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author Adrian Colyer
 */
class AnnotationBindingTestAspect {

	public String doWithAnnotation(ProceedingJoinPoint pjp, TestAnnotation testAnnotation) {
		return testAnnotation.value();
	}

}
