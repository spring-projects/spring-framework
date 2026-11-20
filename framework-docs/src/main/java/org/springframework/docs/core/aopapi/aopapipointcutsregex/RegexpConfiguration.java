/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.aopapi.aopapipointcutsregex;

import org.aopalliance.aop.Advice;

import org.springframework.aop.support.RegexpMethodPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// tag::snippet[]
@Configuration
public class RegexpConfiguration {

	@Bean
	public RegexpMethodPointcutAdvisor settersAndAbsquatulateAdvisor(Advice beanNameOfAopAllianceInterceptor) {
		RegexpMethodPointcutAdvisor advisor = new RegexpMethodPointcutAdvisor();
		advisor.setAdvice(beanNameOfAopAllianceInterceptor);
		advisor.setPatterns(".*set.*", ".*absquatulate");
		return advisor;
	}
}
// end::snippet[]

