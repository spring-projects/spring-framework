/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.aop.ataspectj.aopataspectjexample;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

// tag::snippet[]
@Configuration
@EnableAspectJAutoProxy
public class ApplicationConfiguration {

	@Bean
	public ConcurrentOperationExecutor concurrentOperationExecutor() {
		ConcurrentOperationExecutor executor = new ConcurrentOperationExecutor();
		executor.setMaxRetries(3);
		executor.setOrder(100);
		return executor;

	}
}
// end::snippet[]
