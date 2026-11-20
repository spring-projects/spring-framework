/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.context.annotation;

import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration(proxyBeanMethods = false)
public class InjectionPointConfiguration {

	@Bean
	public String classToString(Class<?> callingClass) {
		return callingClass.getName();
	}


	@Configuration(proxyBeanMethods = false)
	public static class BeansConfiguration {

		@Bean
		@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
		public Class<?> callingClass(InjectionPoint injectionPoint) {
			return injectionPoint.getMember().getDeclaringClass();
		}
	}

}
