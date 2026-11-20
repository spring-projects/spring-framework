/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
