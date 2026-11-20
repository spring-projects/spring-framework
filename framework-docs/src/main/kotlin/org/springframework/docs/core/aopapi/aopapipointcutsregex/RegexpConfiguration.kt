/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.aopapi.aopapipointcutsregex

import org.aopalliance.aop.Advice
import org.springframework.aop.support.RegexpMethodPointcutAdvisor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// tag::snippet[]
@Configuration
class RegexpConfiguration {

	@Bean
	fun settersAndAbsquatulateAdvisor(beanNameOfAopAllianceInterceptor: Advice) = RegexpMethodPointcutAdvisor().apply {
		advice = beanNameOfAopAllianceInterceptor
		setPatterns(".*set.*", ".*absquatulate")
	}
}
// end::snippet[]
