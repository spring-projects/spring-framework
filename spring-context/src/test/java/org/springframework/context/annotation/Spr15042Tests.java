/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.target.CommonsPool2TargetSource;

/**
 * @author Juergen Hoeller
 */
class Spr15042Tests {

	@Test
	void poolingTargetSource() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(PoolingTargetSourceConfig.class);
		context.close();
	}


	@Configuration
	static class PoolingTargetSourceConfig {

		@Bean
		@Scope(scopeName = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
		public ProxyFactoryBean myObject() {
			ProxyFactoryBean pfb = new ProxyFactoryBean();
			pfb.setTargetSource(poolTargetSource());
			return pfb;
		}

		@Bean
		public CommonsPool2TargetSource poolTargetSource() {
			CommonsPool2TargetSource pool = new CommonsPool2TargetSource();
			pool.setMaxSize(3);
			pool.setTargetBeanName("myObjectTarget");
			return pool;
		}

		@Bean(name = "myObjectTarget")
		@Scope(scopeName = "prototype")
		public Object myObjectTarget() {
			return new Object();
		}
	}

}
