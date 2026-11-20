/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.aopapi.aopapipointcutsregex;

import org.springframework.aop.support.JdkRegexpMethodPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// tag::snippet[]
@Configuration
public class JdkRegexpConfiguration {

	@Bean
	public JdkRegexpMethodPointcut settersAndAbsquatulatePointcut() {
		JdkRegexpMethodPointcut pointcut = new JdkRegexpMethodPointcut();
		pointcut.setPatterns(".*set.*", ".*absquatulate");
		return pointcut;
	}
}
// end::snippet[]
