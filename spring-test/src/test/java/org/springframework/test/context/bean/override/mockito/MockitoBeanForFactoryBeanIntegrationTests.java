/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
import static org.mockito.Mockito.when;

/**
 * Test {@link MockitoBean @MockitoBean} for a factory bean configuration.
 *
 * @author Simon Baslé
 */
@SpringJUnitConfig
@TestMethodOrder(OrderAnnotation.class)
public class MockitoBeanForFactoryBeanIntegrationTests {

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
	}

	public interface TestBean {

		String hello();
	}

}
