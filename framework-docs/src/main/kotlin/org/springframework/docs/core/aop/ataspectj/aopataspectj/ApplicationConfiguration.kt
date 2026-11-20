/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.aop.ataspectj.aopataspectj

import org.springframework.context.annotation.Bean

// tag::snippet[]
class ApplicationConfiguration {

	@Bean
	fun myAspect() = NotVeryUsefulAspect().apply {
		// Configure properties of the aspect here
	}
}
// end::snippet[]
