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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBeanForBeanFactoryIntegrationTests.TestBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test {@link MockitoSpyBean @MockitoSpyBean} for a factory bean configuration.
 *
 * @author Simon Baslé
 */
@SpringJUnitConfig
class MockitoSpyBeanForBeanFactoryIntegrationTests {

	@MockitoSpyBean
	private TestBean testBean;

	@Autowired
	private TestFactoryBean testFactoryBean;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void beanReturnedByFactoryIsSpied() {
		TestBean bean = this.applicationContext.getBean(TestBean.class);
		assertThat(this.testBean).as("injected same").isSameAs(bean);
		assertThat(bean.hello()).isEqualTo("hi");

		Mockito.verify(bean).hello();
	}

	@Test
	void factoryItselfIsNotSpied() {
		assertThat(this.testFactoryBean.getObject()).isNotSameAs(this.testBean);
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		TestFactoryBean testFactoryBean() {
			return new TestFactoryBean();
		}
	}

	static class TestBeanImpl implements TestBean {

		@Override
		public String hello() {
			return "hi";
		}
	}

	static class TestFactoryBean implements FactoryBean<TestBean> {

		@Override
		public TestBean getObject() {
			return new TestBeanImpl();
		}

		@Override
		public Class<?> getObjectType() {
			return TestBean.class;
		}
	}

}
