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

package org.springframework.validation.beanvalidation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.Default;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncAnnotationAdvisor;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for proxy-based method validation via {@link MethodValidationInterceptor}
 * and/or {@link MethodValidationPostProcessor}.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchevß
 */
public class MethodValidationProxyTests {

	@Test
	@SuppressWarnings("unchecked")
	public void testMethodValidationInterceptor() {
		MyValidBean bean = new MyValidBean();
		ProxyFactory factory = new ProxyFactory(bean);
		factory.addAdvice(new MethodValidationInterceptor());
		factory.addAdvisor(new AsyncAnnotationAdvisor());
		doTestProxyValidation((MyValidInterface<String>) factory.getProxy());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMethodValidationPostProcessor() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("mvpp", MethodValidationPostProcessor.class);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("beforeExistingAdvisors", false);
		context.registerSingleton("aapp", AsyncAnnotationBeanPostProcessor.class, pvs);
		context.registerSingleton("bean", MyValidBean.class);
		context.refresh();
		doTestProxyValidation(context.getBean("bean", MyValidInterface.class));
		context.close();
	}

	@Test // gh-29782
	@SuppressWarnings("unchecked")
	public void testMethodValidationPostProcessorForInterfaceOnlyProxy() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(MethodValidationPostProcessor.class);
		context.registerBean(MyValidInterface.class, () ->
				ProxyFactory.getProxy(MyValidInterface.class, new MyValidClientInterfaceMethodInterceptor()));
		context.refresh();
		doTestProxyValidation(context.getBean(MyValidInterface.class));
		context.close();
	}

	@SuppressWarnings("DataFlowIssue")
	private void doTestProxyValidation(MyValidInterface<String> proxy) {
		assertThat(proxy.myValidMethod("value", 5)).isNotNull();
		assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> proxy.myValidMethod("value", 15));
		assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> proxy.myValidMethod(null, 5));
		assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> proxy.myValidMethod("value", 0));
		proxy.myValidAsyncMethod("value", 5);
		assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> proxy.myValidAsyncMethod("value", 15));
		assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> proxy.myValidAsyncMethod(null, 5));
		assertThat(proxy.myGenericMethod("myValue")).isEqualTo("myValue");
		assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> proxy.myGenericMethod(null));
	}

	@Test
	public void testLazyValidatorForMethodValidation() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				LazyMethodValidationConfig.class, CustomValidatorBean.class,
				MyValidBean.class, MyValidFactoryBean.class);
		context.getBeansOfType(MyValidInterface.class).values().forEach(bean -> bean.myValidMethod("value", 5));
		context.close();
	}

	@Test
	public void testLazyValidatorForMethodValidationWithProxyTargetClass() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				LazyMethodValidationConfigWithProxyTargetClass.class, CustomValidatorBean.class,
				MyValidBean.class, MyValidFactoryBean.class);
		context.getBeansOfType(MyValidInterface.class).values().forEach(bean -> bean.myValidMethod("value", 5));
		context.close();
	}


	@MyStereotype
	public static class MyValidBean implements MyValidInterface<String> {

		@SuppressWarnings("DataFlowIssue")
		@NotNull
		@Override
		public Object myValidMethod(String arg1, int arg2) {
			return (arg2 == 0 ? null : "value");
		}

		@Override
		public void myValidAsyncMethod(String arg1, int arg2) {
		}

		@Override
		public String myGenericMethod(String value) {
			return value;
		}
	}


	@MyStereotype
	public static class MyValidFactoryBean implements FactoryBean<String>, MyValidInterface<String> {

		@Override
		public String getObject() {
			return null;
		}

		@Override
		public Class<?> getObjectType() {
			return String.class;
		}

		@SuppressWarnings("DataFlowIssue")
		@NotNull
		@Override
		public Object myValidMethod(String arg1, int arg2) {
			return (arg2 == 0 ? null : "value");
		}

		@Override
		public void myValidAsyncMethod(String arg1, int arg2) {
		}

		@Override
		public String myGenericMethod(String value) {
			return value;
		}
	}


	@MyStereotype
	public interface MyValidInterface<T> {

		@NotNull
		Object myValidMethod(@NotNull(groups = MyGroup.class) String arg1, @Max(10) int arg2);

		@MyValid
		@Async
		void myValidAsyncMethod(@NotNull(groups = OtherGroup.class) String arg1, @Max(10) int arg2);

		T myGenericMethod(@NotNull T value);
	}


	static class MyValidClientInterfaceMethodInterceptor implements MethodInterceptor {

		private final MyValidBean myValidBean = new MyValidBean();

		@Nullable
		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			Method method;
			try {
				method = ClassUtils.getMethod(MyValidBean.class, invocation.getMethod().getName(), (Class<?>[]) null);
			}
			catch (IllegalStateException ex) {
				method = BridgeMethodResolver.findBridgedMethod(
						ClassUtils.getMostSpecificMethod(invocation.getMethod(), MyValidBean.class));
			}
			return ReflectionUtils.invokeMethod(method, this.myValidBean, invocation.getArguments());
		}
	}


	public interface MyGroup {
	}


	public interface OtherGroup {
	}


	@Validated({MyGroup.class, Default.class})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MyStereotype {
	}


	@Validated({OtherGroup.class, Default.class})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MyValid {
	}


	@Configuration
	public static class LazyMethodValidationConfig {

		@Bean
		public static MethodValidationPostProcessor methodValidationPostProcessor(@Lazy Validator validator) {
			MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
			postProcessor.setValidator(validator);
			return postProcessor;
		}
	}


	@Configuration
	public static class LazyMethodValidationConfigWithProxyTargetClass {

		@Bean
		public static MethodValidationPostProcessor methodValidationPostProcessor(@Lazy Validator validator) {
			MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
			postProcessor.setValidator(validator);
			postProcessor.setProxyTargetClass(true);
			return postProcessor;
		}
	}

}
