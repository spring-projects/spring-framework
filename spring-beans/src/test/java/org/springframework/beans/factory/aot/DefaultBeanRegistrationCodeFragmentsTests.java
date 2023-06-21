/*
 * Copyright 2002-2023 the original author or authors.
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

import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.InjectAnnotationBeanPostProcessorTests.StringFactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.DummyFactory;
import org.springframework.beans.testfixture.beans.factory.aot.GenericFactoryBean;
import org.springframework.beans.testfixture.beans.factory.aot.MockBeanRegistrationsCode;
import org.springframework.beans.testfixture.beans.factory.aot.NumberFactoryBean;
import org.springframework.beans.testfixture.beans.factory.aot.SimpleBean;
import org.springframework.beans.testfixture.beans.factory.aot.SimpleBeanConfiguration;
import org.springframework.beans.testfixture.beans.factory.aot.SimpleBeanFactoryBean;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.ClassName;
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
		RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
		assertTarget(createInstance(registeredBean).getTarget(registeredBean,
				SimpleBean.class.getDeclaredConstructors()[0]), SimpleBean.class);
	}

	@Test
	void getTargetOnConstructorToPublicFactoryBean() {
		RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
		assertTarget(createInstance(registeredBean).getTarget(registeredBean,
				SimpleBeanFactoryBean.class.getDeclaredConstructors()[0]), SimpleBean.class);
	}

	@Test
	void getTargetOnConstructorToPublicGenericFactoryBeanExtractTargetFromFactoryBeanType() {
		RegisteredBean registeredBean = registerTestBean(ResolvableType
				.forClassWithGenerics(GenericFactoryBean.class, SimpleBean.class));
		assertTarget(createInstance(registeredBean).getTarget(registeredBean,
				GenericFactoryBean.class.getDeclaredConstructors()[0]), SimpleBean.class);
	}

	@Test
	void getTargetOnConstructorToPublicGenericFactoryBeanWithBoundExtractTargetFromFactoryBeanType() {
		RegisteredBean registeredBean = registerTestBean(ResolvableType
				.forClassWithGenerics(NumberFactoryBean.class, Integer.class));
		assertTarget(createInstance(registeredBean).getTarget(registeredBean,
				NumberFactoryBean.class.getDeclaredConstructors()[0]), Integer.class);
	}

	@Test
	void getTargetOnConstructorToPublicGenericFactoryBeanUseBeanTypeAsFallback() {
		RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
		assertTarget(createInstance(registeredBean).getTarget(registeredBean,
				GenericFactoryBean.class.getDeclaredConstructors()[0]), SimpleBean.class);
	}

	@Test
	void getTargetOnConstructorToProtectedFactoryBean() {
		RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
		assertTarget(createInstance(registeredBean).getTarget(registeredBean,
						PrivilegedTestBeanFactoryBean.class.getDeclaredConstructors()[0]),
				PrivilegedTestBeanFactoryBean.class);
	}

	@Test
	void getTargetOnMethod() {
		RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
		Method method = ReflectionUtils.findMethod(SimpleBeanConfiguration.class, "simpleBean");
		assertThat(method).isNotNull();
		assertTarget(createInstance(registeredBean).getTarget(registeredBean, method),
				SimpleBeanConfiguration.class);
	}

	@Test
	void getTargetOnMethodWithInnerBeanInJavaPackage() {
		RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
		RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
				new RootBeanDefinition(String.class));
		Method method = ReflectionUtils.findMethod(getClass(), "createString");
		assertThat(method).isNotNull();
		assertTarget(createInstance(innerBean).getTarget(innerBean, method), getClass());
	}

	@Test
	void getTargetOnConstructorWithInnerBeanInJavaPackage() {
		RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
		RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean", new RootBeanDefinition(String.class));
		assertTarget(createInstance(innerBean).getTarget(innerBean,
				String.class.getDeclaredConstructors()[0]), SimpleBean.class);
	}

	@Test
	void getTargetOnConstructorWithInnerBeanOnTypeInJavaPackage() {
		RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
		RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
				new RootBeanDefinition(StringFactoryBean.class));
		assertTarget(createInstance(innerBean).getTarget(innerBean,
				StringFactoryBean.class.getDeclaredConstructors()[0]), SimpleBean.class);
	}

	@Test
	void getTargetOnMethodWithInnerBeanInRegularPackage() {
		RegisteredBean registeredBean = registerTestBean(DummyFactory.class);
		RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
				new RootBeanDefinition(SimpleBean.class));
		Method method = ReflectionUtils.findMethod(SimpleBeanConfiguration.class, "simpleBean");
		assertThat(method).isNotNull();
		assertTarget(createInstance(innerBean).getTarget(innerBean, method),
				SimpleBeanConfiguration.class);
	}

	@Test
	void getTargetOnConstructorWithInnerBeanInRegularPackage() {
		RegisteredBean registeredBean = registerTestBean(DummyFactory.class);
		RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
				new RootBeanDefinition(SimpleBean.class));
		assertTarget(createInstance(innerBean).getTarget(innerBean,
				SimpleBean.class.getDeclaredConstructors()[0]), SimpleBean.class);
	}

	@Test
	void getTargetOnConstructorWithInnerBeanOnFactoryBeanOnTypeInRegularPackage() {
		RegisteredBean registeredBean = registerTestBean(DummyFactory.class);
		RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
				new RootBeanDefinition(SimpleBean.class));
		assertTarget(createInstance(innerBean).getTarget(innerBean,
				SimpleBeanFactoryBean.class.getDeclaredConstructors()[0]), SimpleBean.class);
	}

	private void assertTarget(ClassName target, Class<?> expected) {
		assertThat(target).isEqualTo(ClassName.get(expected));
	}


	private RegisteredBean registerTestBean(Class<?> beanType) {
		this.beanFactory.registerBeanDefinition("testBean",
				new RootBeanDefinition(beanType));
		return RegisteredBean.of(this.beanFactory, "testBean");
	}

	private RegisteredBean registerTestBean(ResolvableType beanType) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setTargetType(beanType);
		this.beanFactory.registerBeanDefinition("testBean", beanDefinition);
		return RegisteredBean.of(this.beanFactory, "testBean");
	}

	private BeanRegistrationCodeFragments createInstance(RegisteredBean registeredBean) {
		return new DefaultBeanRegistrationCodeFragments(this.beanRegistrationsCode, registeredBean,
				new BeanDefinitionMethodGeneratorFactory(this.beanFactory));
	}

	@SuppressWarnings("unused")
	static String createString() {
		return "Test";
	}

	static class PrivilegedTestBeanFactoryBean implements FactoryBean<SimpleBean> {

		@Override
		public SimpleBean getObject() throws Exception {
			return new SimpleBean();
		}

		@Override
		public Class<?> getObjectType() {
			return SimpleBean.class;
		}
	}

}
