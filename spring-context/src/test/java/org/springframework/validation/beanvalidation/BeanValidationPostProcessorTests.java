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

package org.springframework.validation.beanvalidation;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncAnnotationAdvisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 */
class BeanValidationPostProcessorTests {

	@Test
	void testNotNullConstraint() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("bvpp", new RootBeanDefinition(BeanValidationPostProcessor.class));
		ac.registerBeanDefinition("capp", new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class));
		ac.registerBeanDefinition("bean", new RootBeanDefinition(NotNullConstrainedBean.class));
		assertThatExceptionOfType(BeanCreationException.class)
			.isThrownBy(ac::refresh)
			.havingRootCause()
			.withMessageContainingAll("testBean", "invalid");
		ac.close();
	}

	@Test
	void testNotNullConstraintSatisfied() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("bvpp", new RootBeanDefinition(BeanValidationPostProcessor.class));
		ac.registerBeanDefinition("capp", new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class));
		RootBeanDefinition bd = new RootBeanDefinition(NotNullConstrainedBean.class);
		bd.getPropertyValues().add("testBean", new TestBean());
		ac.registerBeanDefinition("bean", bd);
		ac.refresh();
		ac.close();
	}

	@Test
	void testNotNullConstraintAfterInitialization() {
		GenericApplicationContext ac = new GenericApplicationContext();
		RootBeanDefinition bvpp = new RootBeanDefinition(BeanValidationPostProcessor.class);
		bvpp.getPropertyValues().add("afterInitialization", true);
		ac.registerBeanDefinition("bvpp", bvpp);
		ac.registerBeanDefinition("capp", new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class));
		ac.registerBeanDefinition("bean", new RootBeanDefinition(AfterInitConstraintBean.class));
		ac.refresh();
		ac.close();
	}

	@Test
	void testNotNullConstraintAfterInitializationWithProxy() {
		GenericApplicationContext ac = new GenericApplicationContext();
		RootBeanDefinition bvpp = new RootBeanDefinition(BeanValidationPostProcessor.class);
		bvpp.getPropertyValues().add("afterInitialization", true);
		ac.registerBeanDefinition("bvpp", bvpp);
		ac.registerBeanDefinition("capp", new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class));
		ac.registerBeanDefinition("bean", new RootBeanDefinition(AfterInitConstraintBean.class));
		ac.registerBeanDefinition("autoProxyCreator", new RootBeanDefinition(DefaultAdvisorAutoProxyCreator.class));
		ac.registerBeanDefinition("asyncAdvisor", new RootBeanDefinition(AsyncAnnotationAdvisor.class));
		ac.refresh();
		ac.close();
	}

	@Test
	void testSizeConstraint() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("bvpp", new RootBeanDefinition(BeanValidationPostProcessor.class));
		RootBeanDefinition bd = new RootBeanDefinition(NotNullConstrainedBean.class);
		bd.getPropertyValues().add("testBean", new TestBean());
		bd.getPropertyValues().add("stringValue", "s");
		ac.registerBeanDefinition("bean", bd);
		assertThatExceptionOfType(BeanCreationException.class)
			.isThrownBy(ac::refresh)
			.havingRootCause()
			.withMessageContainingAll("stringValue", "invalid");
		ac.close();
	}

	@Test
	void testSizeConstraintSatisfied() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("bvpp", new RootBeanDefinition(BeanValidationPostProcessor.class));
		RootBeanDefinition bd = new RootBeanDefinition(NotNullConstrainedBean.class);
		bd.getPropertyValues().add("testBean", new TestBean());
		bd.getPropertyValues().add("stringValue", "ss");
		ac.registerBeanDefinition("bean", bd);
		ac.refresh();
		ac.close();
	}


	public static class NotNullConstrainedBean {

		@NotNull
		private TestBean testBean;

		@Size(min = 2)
		private String stringValue;

		public TestBean getTestBean() {
			return testBean;
		}

		public void setTestBean(TestBean testBean) {
			this.testBean = testBean;
		}

		public String getStringValue() {
			return stringValue;
		}

		public void setStringValue(String stringValue) {
			this.stringValue = stringValue;
		}

		@PostConstruct
		public void init() {
			assertThat(this.testBean).as("Shouldn't be here after constraint checking").isNotNull();
		}
	}


	public static class AfterInitConstraintBean {

		@NotNull
		private TestBean testBean;

		public TestBean getTestBean() {
			return testBean;
		}

		public void setTestBean(TestBean testBean) {
			this.testBean = testBean;
		}

		@PostConstruct
		public void init() {
			this.testBean = new TestBean();
		}

		@Async
		void asyncMethod() {
		}
	}

}
