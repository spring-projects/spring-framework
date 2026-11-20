/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.beans.dependencies.beansfactorylazyinit

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy

@Configuration
class ApplicationConfiguration {

	// tag::snippet[]
	@Bean
	@Lazy
	fun lazy(): ExpensiveToCreateBean {
		return ExpensiveToCreateBean()
	}

	@Bean
	fun notLazy(): AnotherBean {
		return AnotherBean()
	}
	// end::snippet[]
}