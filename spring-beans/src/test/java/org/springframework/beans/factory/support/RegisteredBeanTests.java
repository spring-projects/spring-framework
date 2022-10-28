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

package org.springframework.beans.factory.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link RegisteredBean}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class RegisteredBeanTests {


	private DefaultListableBeanFactory beanFactory;


	@BeforeEach
	void setup() {
		this.beanFactory = new DefaultListableBeanFactory();
		this.beanFactory.registerBeanDefinition("bd",
				new RootBeanDefinition(TestBean.class));
		this.beanFactory.registerSingleton("sb", new TestBean());
	}

	@Test
	void ofWhenBeanFactoryIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> RegisteredBean.of(null, "bd"))
				.withMessage("'beanFactory' must not be null");
	}

	@Test
	void ofWhenBeanNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> RegisteredBean.of(this.beanFactory, null))
				.withMessage("'beanName' must not be empty");
	}

	@Test
	void ofInnerBeanWhenInnerBeanIsNullThrowsException() {
		RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
		assertThatIllegalArgumentException().isThrownBy(
				() -> RegisteredBean.ofInnerBean(parent, (BeanDefinitionHolder) null))
				.withMessage("'innerBean' must not be null");
	}

	@Test
	void ofInnerBeanWhenParentIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> RegisteredBean.ofInnerBean(null,
						new RootBeanDefinition(TestInnerBean.class)))
				.withMessage("'parent' must not be null");
	}

	@Test
	void ofInnerBeanWhenInnerBeanDefinitionIsNullThrowsException() {
		RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> RegisteredBean.ofInnerBean(parent, "ib", null))
				.withMessage("'innerBeanDefinition' must not be null");
	}

	@Test
	void getBeanNameReturnsBeanName() {
		RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "bd");
		assertThat(registeredBean.getBeanName()).isEqualTo("bd");
	}

	@Test
	void getBeanNameWhenNamedInnerBeanReturnsBeanName() {
		RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
		RegisteredBean registeredBean = RegisteredBean.ofInnerBean(parent, "ib",
				new RootBeanDefinition(TestInnerBean.class));
		assertThat(registeredBean.getBeanName()).isEqualTo("ib");
	}

	@Test
	void getBeanNameWhenUnnamedInnerBeanReturnsBeanName() {
		RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
		RegisteredBean registeredBean = RegisteredBean.ofInnerBean(parent,
				new RootBeanDefinition(TestInnerBean.class));
		assertThat(registeredBean.getBeanName()).startsWith("(inner bean)#");
	}

	@Test
	void getBeanClassReturnsBeanClass() {
		RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "bd");
		assertThat(registeredBean.getBeanClass()).isEqualTo(TestBean.class);
	}

	@Test
	void getBeanTypeReturnsBeanType() {
		RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "bd");
		assertThat(registeredBean.getBeanType().toClass()).isEqualTo(TestBean.class);
	}

	@Test
	void getBeanTypeWhenHasInstanceBackedByLambdaDoesNotReturnLambdaType() {
		this.beanFactory.registerBeanDefinition("bfpp", new RootBeanDefinition(
				BeanFactoryPostProcessor.class, RegisteredBeanTests::getBeanFactoryPostProcessorLambda));
		this.beanFactory.getBean("bfpp");
		RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "bfpp");
		assertThat(registeredBean.getBeanType().toClass()).isEqualTo(BeanFactoryPostProcessor.class);
	}

	static BeanFactoryPostProcessor getBeanFactoryPostProcessorLambda() {
		return bf -> {};
	}

	@Test
	void getMergedBeanDefinitionReturnsMergedBeanDefinition() {
		RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "bd");
		assertThat(registeredBean.getMergedBeanDefinition().getBeanClass())
				.isEqualTo(TestBean.class);
	}

	@Test
	void getMergedBeanDefinitionWhenSingletonThrowsException() {
		RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "sb");
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> registeredBean.getMergedBeanDefinition());
	}

	@Test
	void getMergedBeanDefinitionWhenInnerBeanReturnsMergedBeanDefinition() {
		RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
		RegisteredBean registeredBean = RegisteredBean.ofInnerBean(parent,
				new RootBeanDefinition(TestInnerBean.class));
		assertThat(registeredBean.getMergedBeanDefinition().getBeanClass())
				.isEqualTo(TestInnerBean.class);
	}

	@Test
	void isInnerBeanWhenInnerBeanReturnsTrue() {
		RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
		RegisteredBean registeredBean = RegisteredBean.ofInnerBean(parent,
				new RootBeanDefinition(TestInnerBean.class));
		assertThat(registeredBean.isInnerBean()).isTrue();
	}

	@Test
	void isInnerBeanWhenNotInnerBeanReturnsTrue() {
		RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "bd");
		assertThat(registeredBean.isInnerBean()).isFalse();
	}

	@Test
	void getParentWhenInnerBeanReturnsParent() {
		RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
		RegisteredBean registeredBean = RegisteredBean.ofInnerBean(parent,
				new RootBeanDefinition(TestInnerBean.class));
		assertThat(registeredBean.getParent()).isSameAs(parent);
	}

	@Test
	void getParentWhenNotInnerBeanReturnsNull() {
		RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "bd");
		assertThat(registeredBean.getParent()).isNull();
	}

	@Test
	void isGeneratedBeanNameWhenInnerBeanWithoutNameReturnsTrue() {
		RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
		RegisteredBean registeredBean = RegisteredBean.ofInnerBean(parent,
				new RootBeanDefinition(TestInnerBean.class));
		assertThat(registeredBean.isGeneratedBeanName()).isTrue();
	}

	@Test
	void isGeneratedBeanNameWhenInnerBeanWithNameReturnsFalse() {
		RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
		RegisteredBean registeredBean = RegisteredBean.ofInnerBean(parent,
				new BeanDefinitionHolder(new RootBeanDefinition(TestInnerBean.class),
						"test"));
		assertThat(registeredBean.isGeneratedBeanName()).isFalse();
	}

	@Test
	void isGeneratedBeanNameWhenNotInnerBeanReturnsFalse() {
		RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "bd");
		assertThat(registeredBean.isGeneratedBeanName()).isFalse();
	}

	static class TestBean {

	}

	static class TestInnerBean {

	}

}
