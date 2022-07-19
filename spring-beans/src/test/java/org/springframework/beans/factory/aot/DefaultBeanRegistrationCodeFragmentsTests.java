/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.aot;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.InjectAnnotationBeanPostProcessorTests.StringFactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.DummyFactory;
import org.springframework.beans.testfixture.beans.factory.aot.MockBeanRegistrationsCode;
import org.springframework.core.testfixture.aot.generate.TestGenerationContext;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultBeanRegistrationCodeFragments}.
 *
 * @author Stephane Nicoll
 */
class DefaultBeanRegistrationCodeFragmentsTests {

	private final BeanRegistrationsCode beanRegistrationsCode = new MockBeanRegistrationsCode(new TestGenerationContext());

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Test
	void getTargetOnConstructor() {
		RegisteredBean registeredBean = registerTestBean(TestBean.class);
		assertThat(createInstance(registeredBean).getTarget(registeredBean,
				TestBean.class.getDeclaredConstructors()[0])).isEqualTo(TestBean.class);
	}

	@Test
	void getTargetOnConstructorToPublicFactoryBean() {
		RegisteredBean registeredBean = registerTestBean(TestBean.class);
		assertThat(createInstance(registeredBean).getTarget(registeredBean,
				TestBeanFactoryBean.class.getDeclaredConstructors()[0])).isEqualTo(TestBean.class);
	}

	@Test
	void getTargetOnConstructorToProtectedFactoryBean() {
		RegisteredBean registeredBean = registerTestBean(TestBean.class);
		assertThat(createInstance(registeredBean).getTarget(registeredBean,
				PrivilegedTestBeanFactoryBean.class.getDeclaredConstructors()[0])).isEqualTo(PrivilegedTestBeanFactoryBean.class);
	}

	@Test
	void getTargetOnMethod() {
		RegisteredBean registeredBean = registerTestBean(TestBean.class);
		Method method = ReflectionUtils.findMethod(TestBeanFactoryBean.class, "getObject");
		assertThat(method).isNotNull();
		assertThat(createInstance(registeredBean).getTarget(registeredBean,
				method)).isEqualTo(TestBeanFactoryBean.class);
	}

	@Test
	void getTargetOnMethodWithInnerBeanInJavaPackage() {
		RegisteredBean registeredBean = registerTestBean(TestBean.class);
		RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean", new RootBeanDefinition(String.class));
		Method method = ReflectionUtils.findMethod(getClass(), "createString");
		assertThat(method).isNotNull();
		assertThat(createInstance(innerBean).getTarget(innerBean,
				method)).isEqualTo(getClass());
	}

	@Test
	void getTargetOnConstructorWithInnerBeanInJavaPackage() {
		RegisteredBean registeredBean = registerTestBean(TestBean.class);
		RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean", new RootBeanDefinition(String.class));
		assertThat(createInstance(innerBean).getTarget(innerBean,
				String.class.getDeclaredConstructors()[0])).isEqualTo(TestBean.class);
	}

	@Test
	void getTargetOnConstructorWithInnerBeanOnTypeInJavaPackage() {
		RegisteredBean registeredBean = registerTestBean(TestBean.class);
		RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
				new RootBeanDefinition(StringFactoryBean.class));
		assertThat(createInstance(innerBean).getTarget(innerBean,
				StringFactoryBean.class.getDeclaredConstructors()[0])).isEqualTo(TestBean.class);
	}

	@Test
	void getTargetOnMethodWithInnerBeanInRegularPackage() {
		RegisteredBean registeredBean = registerTestBean(DummyFactory.class);
		RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean", new RootBeanDefinition(TestBean.class));
		Method method = ReflectionUtils.findMethod(TestBeanFactoryBean.class, "getObject");
		assertThat(method).isNotNull();
		assertThat(createInstance(innerBean).getTarget(innerBean, method)).isEqualTo(TestBeanFactoryBean.class);
	}

	@Test
	void getTargetOnConstructorWithInnerBeanInRegularPackage() {
		RegisteredBean registeredBean = registerTestBean(DummyFactory.class);
		RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean", new RootBeanDefinition(TestBean.class));
		assertThat(createInstance(innerBean).getTarget(innerBean,
				TestBean.class.getDeclaredConstructors()[0])).isEqualTo(TestBean.class);
	}

	@Test
	void getTargetOnConstructorWithInnerBeanOnFactoryBeanOnTypeInRegularPackage() {
		RegisteredBean registeredBean = registerTestBean(DummyFactory.class);
		RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
				new RootBeanDefinition(TestBean.class));
		assertThat(createInstance(innerBean).getTarget(innerBean,
				TestBeanFactoryBean.class.getDeclaredConstructors()[0])).isEqualTo(TestBean.class);
	}


	private RegisteredBean registerTestBean(Class<?> beanType) {
		this.beanFactory.registerBeanDefinition("testBean",
				new RootBeanDefinition(beanType));
		return RegisteredBean.of(this.beanFactory, "testBean");
	}

	private BeanRegistrationCodeFragments createInstance(RegisteredBean registeredBean) {
		return new DefaultBeanRegistrationCodeFragments(this.beanRegistrationsCode, registeredBean, new BeanDefinitionMethodGeneratorFactory(this.beanFactory));
	}

	@SuppressWarnings("unused")
	static String createString() {
		return "Test";
	}

	static class PrivilegedTestBeanFactoryBean implements FactoryBean<TestBean> {

		@Override
		public TestBean getObject() throws Exception {
			return new TestBean();
		}

		@Override
		public Class<?> getObjectType() {
			return TestBean.class;
		}
	}

}
