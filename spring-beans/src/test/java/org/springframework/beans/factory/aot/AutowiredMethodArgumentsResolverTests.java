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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AutowiredMethodArgumentsResolver}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class AutowiredMethodArgumentsResolverTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Test
	void forMethodWhenMethodNameIsEmptyThrowsException() {
		String message = "'methodName' must not be empty";
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AutowiredMethodArgumentsResolver.forMethod(null))
				.withMessage(message);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AutowiredMethodArgumentsResolver.forMethod(""))
				.withMessage(message);
		assertThatIllegalArgumentException()
				.isThrownBy(
						() -> AutowiredMethodArgumentsResolver.forRequiredMethod(null))
				.withMessage(message);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AutowiredMethodArgumentsResolver.forRequiredMethod(" "))
				.withMessage(message);
	}

	@Test
	void resolveWhenRegisteredBeanIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AutowiredMethodArgumentsResolver
						.forMethod("injectString", String.class).resolve(null))
				.withMessage("'registeredBean' must not be null");
	}

	@Test
	void resolveWhenMethodIsMissingThrowsException() {
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		AutowiredMethodArgumentsResolver resolver = AutowiredMethodArgumentsResolver.forMethod("missing", InputStream.class);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> resolver.resolve(registeredBean))
				.withMessage("Method 'missing' with parameter types [java.io.InputStream] declared on %s could not be found.",
						TestBean.class.getName());
	}

	@Test
	void resolveRequiredWithSingleDependencyReturnsValue() {
		this.beanFactory.registerSingleton("test", "testValue");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		AutowiredMethodArgumentsResolver resolver = AutowiredMethodArgumentsResolver
				.forRequiredMethod("injectString", String.class);
		AutowiredArguments resolved = resolver.resolve(registeredBean);
		assertThat(resolved.toArray()).containsExactly("testValue");
	}

	@Test
	void resolveRequiredWhenNoSuchBeanThrowsUnsatisfiedDependencyException() {
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		AutowiredMethodArgumentsResolver resolver = AutowiredMethodArgumentsResolver
				.forRequiredMethod("injectString", String.class);
		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(() -> resolver.resolve(registeredBean)).satisfies(ex -> {
					assertThat(ex.getBeanName()).isEqualTo("testBean");
					assertThat(ex.getInjectionPoint()).isNotNull();
					assertThat(ex.getInjectionPoint().getMember().getName())
							.isEqualTo("injectString");
				});
	}

	@Test
	void resolveNonRequiredWhenNoSuchBeanReturnsNull() {
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		AutowiredMethodArgumentsResolver resolver = AutowiredMethodArgumentsResolver
				.forMethod("injectString", String.class);
		assertThat(resolver.resolve(registeredBean)).isNull();
	}

	@Test
	void resolveRequiredWithMultipleDependenciesReturnsValue() {
		Environment environment = mock();
		this.beanFactory.registerSingleton("test", "testValue");
		this.beanFactory.registerSingleton("environment", environment);
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		AutowiredMethodArgumentsResolver resolver = AutowiredMethodArgumentsResolver
				.forRequiredMethod("injectStringAndEnvironment", String.class,
						Environment.class);
		AutowiredArguments resolved = resolver.resolve(registeredBean);
		assertThat(resolved.toArray()).containsExactly("testValue", environment);
	}

	@Test
	void resolveAndInvokeWhenInstanceIsNullThrowsException() {
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AutowiredMethodArgumentsResolver
						.forMethod("injectString", String.class)
						.resolveAndInvoke(registeredBean, null))
				.withMessage("'instance' must not be null");
	}

	@Test
	void resolveAndInvokeInvokesMethod() {
		this.beanFactory.registerSingleton("test", "testValue");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		AutowiredMethodArgumentsResolver resolver = AutowiredMethodArgumentsResolver
				.forRequiredMethod("injectString", String.class);
		TestBean instance = new TestBean();
		resolver.resolveAndInvoke(registeredBean, instance);
		assertThat(instance.getString()).isEqualTo("testValue");
	}

	@Test
	void resolveWithActionWhenActionIsNullThrowsException() {
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AutowiredMethodArgumentsResolver
						.forMethod("injectString", String.class)
						.resolve(registeredBean, null))
				.withMessage("'action' must not be null");
	}

	@Test
	void resolveWithActionCallsAction() {
		this.beanFactory.registerSingleton("test", "testValue");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		List<Object> result = new ArrayList<>();
		AutowiredMethodArgumentsResolver.forMethod("injectString", String.class)
				.resolve(registeredBean, result::add);
		assertThat(result).hasSize(1);
		assertThat(((AutowiredArguments) result.get(0)).toArray())
				.containsExactly("testValue");
	}

	@Test
	void resolveWhenUsingShortcutsInjectsDirectly() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory() {

			@Override
			protected Map<String, Object> findAutowireCandidates(String beanName,
					Class<?> requiredType, DependencyDescriptor descriptor) {
				throw new AssertionError("Should be shortcut");
			}

		};
		beanFactory.registerSingleton("test", "testValue");
		RegisteredBean registeredBean = registerTestBean(beanFactory);
		AutowiredMethodArgumentsResolver resolver = AutowiredMethodArgumentsResolver
				.forRequiredMethod("injectString", String.class);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> resolver.resolve(registeredBean));
		assertThat(resolver.withShortcut("test").resolve(registeredBean).getObject(0))
				.isEqualTo("testValue");
	}

	@Test
	void resolveRegistersDependantBeans() {
		this.beanFactory.registerSingleton("test", "testValue");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		AutowiredMethodArgumentsResolver.forMethod("injectString", String.class)
				.resolve(registeredBean);
		assertThat(this.beanFactory.getDependentBeans("test"))
				.containsExactly("testBean");
	}

	private RegisteredBean registerTestBean(DefaultListableBeanFactory beanFactory) {
		beanFactory.registerBeanDefinition("testBean",
				new RootBeanDefinition(TestBean.class));
		return RegisteredBean.of(beanFactory, "testBean");
	}

	@SuppressWarnings("unused")
	static class TestBean {

		private String string;

		void injectString(String string) {
			this.string = string;
		}

		void injectStringAndEnvironment(String string, Environment environment) {
		}

		String getString() {
			return this.string;
		}

	}

}
