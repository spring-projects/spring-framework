/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

/**
 * @author Juergen Hoeller
 */
@Configuration
class MyTestBean {

	@Bean
	public org.springframework.beans.testfixture.beans.TestBean myTestBean() {
		return new org.springframework.beans.testfixture.beans.TestBean();
	}

}
