/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
