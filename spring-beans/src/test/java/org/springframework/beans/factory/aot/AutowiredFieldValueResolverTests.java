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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AutowiredFieldValueResolver}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class AutowiredFieldValueResolverTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Test
	void forFieldWhenFieldNameIsEmptyThrowsException() {
		String message = "'fieldName' must not be empty";
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AutowiredFieldValueResolver.forField(null))
				.withMessage(message);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AutowiredFieldValueResolver.forField(""))
				.withMessage(message);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AutowiredFieldValueResolver.forRequiredField(null))
				.withMessage(message);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AutowiredFieldValueResolver.forRequiredField(" "))
				.withMessage(message);
	}

	@Test
	void resolveWhenRegisteredBeanIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() ->
						AutowiredFieldValueResolver.forField("string").resolve(null))
				.withMessage("'registeredBean' must not be null");
	}

	@Test
	void resolveWhenFieldIsMissingThrowsException() {
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AutowiredFieldValueResolver.forField("missing")
						.resolve(registeredBean))
				.withMessage("No field 'missing' found on " + TestBean.class.getName());
	}

	@Test
	void resolveReturnsValue() {
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		Object resolved = AutowiredFieldValueResolver.forField("string")
				.resolve(registeredBean);
		assertThat(resolved).isEqualTo("1");
	}

	@Test
	void resolveWhenRequiredFieldAndBeanReturnsValue() {
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		Object resolved = AutowiredFieldValueResolver.forRequiredField("string")
				.resolve(registeredBean);
		assertThat(resolved).isEqualTo("1");
	}

	@Test
	void resolveWhenRequiredFieldAndNoBeanReturnsNull() {
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		Object resolved = AutowiredFieldValueResolver.forField("string")
				.resolve(registeredBean);
		assertThat(resolved).isNull();
	}

	@Test
	void resolveWhenRequiredFieldAndNoBeanThrowsException() {
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		AutowiredFieldValueResolver resolver = AutowiredFieldValueResolver
				.forRequiredField("string");
		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(() -> resolver.resolve(registeredBean)).satisfies(ex -> {
					assertThat(ex.getBeanName()).isEqualTo("testBean");
					assertThat(ex.getInjectionPoint()).isNotNull();
					assertThat(ex.getInjectionPoint().getField().getName())
							.isEqualTo("string");
				});
	}

	@Test
	void resolveAndSetWhenInstanceIsNullThrowsException() {
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AutowiredFieldValueResolver.forField("string")
						.resolveAndSet(registeredBean, null))
				.withMessage("'instance' must not be null");
	}

	@Test
	void resolveAndSetSetsValue() {
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		TestBean testBean = new TestBean();
		AutowiredFieldValueResolver.forField("string").resolveAndSet(registeredBean,
				testBean);
		assertThat(testBean).extracting("string").isEqualTo("1");
	}

	@Test
	void resolveWithActionWhenActionIsNullThrowsException() {
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AutowiredFieldValueResolver.forField("string")
						.resolve(registeredBean, (ThrowingConsumer<Object>) null))
				.withMessage("'action' must not be null");
	}

	@Test
	void resolveWithActionCallsAction() {
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		List<Object> result = new ArrayList<>();
		AutowiredFieldValueResolver.forField("string").resolve(registeredBean,
				result::add);
		assertThat(result).containsExactly("1");
	}

	@Test
	void resolveWithActionWhenDeducedGenericCallsAction() {
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		TestBean testBean = new TestBean();
		testBean.string = AutowiredFieldValueResolver.forField("string")
				.resolve(registeredBean);
	}

	@Test
	void resolveObjectWhenUsingShortcutInjectsDirectly() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory() {

			@Override
			protected Map<String, Object> findAutowireCandidates(String beanName,
					Class<?> requiredType, DependencyDescriptor descriptor) {
				throw new AssertionError("Should be shortcut");
			}

		};
		beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = registerTestBean(beanFactory);
		AutowiredFieldValueResolver resolver = AutowiredFieldValueResolver
				.forField("string");
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> resolver.resolve(registeredBean));
		assertThat(resolver.withShortcut("one").resolveObject(registeredBean))
				.isEqualTo("1");
	}

	@Test
	void resolveRegistersDependantBeans() {
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		AutowiredFieldValueResolver.forField("string").resolve(registeredBean);
		assertThat(this.beanFactory.getDependentBeans("one")).containsExactly("testBean");
	}

	private RegisteredBean registerTestBean(DefaultListableBeanFactory beanFactory) {
		beanFactory.registerBeanDefinition("testBean",
				new RootBeanDefinition(TestBean.class));
		return RegisteredBean.of(beanFactory, "testBean");
	}

	static class TestBean {

		String string;

	}

}
