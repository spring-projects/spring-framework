/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
