/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
import static org.mockito.Mockito.when;

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
