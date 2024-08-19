/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.bean.override.mockito;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.when;

/**
 * Test {@link MockitoBean @MockitoBean} for a factory bean configuration.
 *
 * @author Simon Baslé
 */
@SpringJUnitConfig
@TestMethodOrder(OrderAnnotation.class)
class MockitoBeanForBeanFactoryIntegrationTests {

	@MockitoBean
	private TestBean testBean;

	@Autowired
	private ApplicationContext applicationContext;

	@Order(1)
	@Test
	void beanReturnedByFactoryIsMocked() {
		TestBean bean = this.applicationContext.getBean(TestBean.class);
		assertThat(bean).isSameAs(this.testBean);

		when(this.testBean.hello()).thenReturn("amock");
		assertThat(bean.hello()).isEqualTo("amock");

		assertThat(TestFactoryBean.USED).isFalse();
	}

	@Order(2)
	@Test
	void beanReturnedByFactoryIsReset() {
		assertThat(this.testBean.hello()).isNull();
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		TestFactoryBean testFactoryBean() {
			return new TestFactoryBean();
		}

	}

	static class TestFactoryBean implements FactoryBean<TestBean> {

		static final AtomicBoolean USED = new AtomicBoolean(false);

		@Override
		public TestBean getObject() {
			USED.set(true);
			return () -> "normal";
		}

		@Override
		public Class<?> getObjectType() {
			return TestBean.class;
		}

		@Override
		public boolean isSingleton() {
			return false;
		}

	}

	interface TestBean {

		String hello();

	}

}
