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

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.when;

/**
 * Test {@link MockitoBean @MockitoBean} for a {@link FactoryBean} that is
 * "broken" or not able to be eagerly initialized.
 *
 * @author Sam Brannen
 * @author Simon Baslé
 */
@SpringJUnitConfig
public class MockitoBeanForBrokenFactoryBeanIntegrationTests {

	@MockitoBean
	TestBean testBean;


	@Test
	void beanReturnedByFactoryIsMocked(@Autowired TestBean autowiredTestBean) {
		assertThat(autowiredTestBean).isSameAs(testBean);

		when(testBean.hello()).thenReturn("mock");

		assertThat(testBean.hello()).isEqualTo("mock");
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		TestFactoryBean testFactoryBean() {
			return new TestFactoryBean();
		}
	}

	static class TestFactoryBean implements FactoryBean<TestBean> {

		TestFactoryBean() {
			throw new BeanCreationException("simulating missing dependencies");
		}

		@Override
		public TestBean getObject() {
			return () -> "prod";
		}

		@Override
		public Class<?> getObjectType() {
			return TestBean.class;
		}
	}

	interface TestBean {

		String hello();
	}

}
