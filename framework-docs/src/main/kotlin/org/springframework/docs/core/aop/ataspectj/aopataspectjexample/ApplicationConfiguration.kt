/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.aop.ataspectj.aopataspectjexample

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

// tag::snippet[]
@Configuration
@EnableAspectJAutoProxy
class ApplicationConfiguration {

	@Bean
	fun concurrentOperationExecutor() = ConcurrentOperationExecutor().apply {
		maxRetries = 3
		order = 100
	}
}
// end::snippet[]
