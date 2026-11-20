/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.context.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class QualifierConfiguration {

	@SuppressWarnings("unused")
	private String bean;

	@Autowired
	@Qualifier("1")
	public void setBean(String bean) {
		this.bean = bean;
	}


	public static class BeansConfiguration {

		@Bean
		@Qualifier("1")
		public String one() {
			return "one";
		}

		@Bean
		@Qualifier("2")
		public String two() {
			return "two";
		}

	}

}
