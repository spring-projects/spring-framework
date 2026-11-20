/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.testfixture.aspectj;

import org.aspectj.lang.annotation.Pointcut;

/**
 * @author Sam Brannen
 * @since 6.0.6
 */
public class CommonPointcuts {

	@Pointcut("execution(* getAge())")
	public void getAgeExecution() {}

}
