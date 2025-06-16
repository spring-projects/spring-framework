/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context.litemode;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify proper scoping of beans created in
 * <em>{@code @Bean} Lite Mode</em>.
 *
 * @author Sam Brannen
 * @since 3.2
 */
@SpringJUnitConfig(AtBeanLiteModeScopeTests.LiteBeans.class)
class AtBeanLiteModeScopeTests {

	/**
	 * This is intentionally <b>not</b> annotated with {@code @Configuration}.
	 */
	static class LiteBeans {

		@Bean
		LifecycleBean singleton() {
			LifecycleBean bean = new LifecycleBean("singleton");
			assertThat(bean.isInitialized()).isFalse();
			return bean;
		}

		@Bean
		@Scope("prototype")
		LifecycleBean prototype() {
			LifecycleBean bean = new LifecycleBean("prototype");
			assertThat(bean.isInitialized()).isFalse();
			return bean;
		}
	}


	@Autowired
	ApplicationContext applicationContext;

	@Autowired
	@Qualifier("singleton")
	LifecycleBean injectedSingletonBean;

	@Autowired
	@Qualifier("prototype")
	LifecycleBean injectedPrototypeBean;


	@Test
	void singletonLiteBean() {
		assertThat(injectedSingletonBean).isNotNull();
		assertThat(injectedSingletonBean.isInitialized()).isTrue();

		LifecycleBean retrievedSingletonBean = applicationContext.getBean("singleton", LifecycleBean.class);
		assertThat(retrievedSingletonBean).isNotNull();
		assertThat(retrievedSingletonBean.isInitialized()).isTrue();

		assertThat(retrievedSingletonBean).isSameAs(injectedSingletonBean);
	}

	@Test
	void prototypeLiteBean() {
		assertThat(injectedPrototypeBean).isNotNull();
		assertThat(injectedPrototypeBean.isInitialized()).isTrue();

		LifecycleBean retrievedPrototypeBean = applicationContext.getBean("prototype", LifecycleBean.class);
		assertThat(retrievedPrototypeBean).isNotNull();
		assertThat(retrievedPrototypeBean.isInitialized()).isTrue();

		assertThat(retrievedPrototypeBean).isNotSameAs(injectedPrototypeBean);
	}

}
