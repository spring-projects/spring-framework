/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.validation.hibernatevalidator5;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncAnnotationAdvisor;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import static org.junit.Assert.*;

/**
 * Copy of {@link org.springframework.validation.beanvalidation.MethodValidationTests},
 * here to be tested against Hibernate Validator 5.
 *
 * @author Juergen Hoeller
 * @since 4.1
 */
@SuppressWarnings("rawtypes")
public class MethodValidationTests {

	@Test
	public void testMethodValidationInterceptor() {
		MyValidBean bean = new MyValidBean();
		ProxyFactory proxyFactory = new ProxyFactory(bean);
		proxyFactory.addAdvice(new MethodValidationInterceptor());
		proxyFactory.addAdvisor(new AsyncAnnotationAdvisor());
		doTestProxyValidation((MyValidInterface) proxyFactory.getProxy());
	}

	@Test
	public void testMethodValidationPostProcessor() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("mvpp", MethodValidationPostProcessor.class);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("beforeExistingAdvisors", false);
		ac.registerSingleton("aapp", AsyncAnnotationBeanPostProcessor.class, pvs);
		ac.registerSingleton("bean", MyValidBean.class);
		ac.refresh();
		doTestProxyValidation(ac.getBean("bean", MyValidInterface.class));
		ac.close();
	}


	@SuppressWarnings("unchecked")
	private void doTestProxyValidation(MyValidInterface proxy) {
		assertNotNull(proxy.myValidMethod("value", 5));
		try {
			assertNotNull(proxy.myValidMethod("value", 15));
			fail("Should have thrown ValidationException");
		}
		catch (javax.validation.ValidationException ex) {
			// expected
		}
		try {
			assertNotNull(proxy.myValidMethod(null, 5));
			fail("Should have thrown ValidationException");
		}
		catch (javax.validation.ValidationException ex) {
			// expected
		}
		try {
			assertNotNull(proxy.myValidMethod("value", 0));
			fail("Should have thrown ValidationException");
		}
		catch (javax.validation.ValidationException ex) {
			// expected
		}

		proxy.myValidAsyncMethod("value", 5);
		try {
			proxy.myValidAsyncMethod("value", 15);
			fail("Should have thrown ValidationException");
		}
		catch (javax.validation.ValidationException ex) {
			// expected
		}
		try {
			proxy.myValidAsyncMethod(null, 5);
			fail("Should have thrown ValidationException");
		}
		catch (javax.validation.ValidationException ex) {
			// expected
		}

		assertEquals("myValue", proxy.myGenericMethod("myValue"));
		try {
			proxy.myGenericMethod(null);
			fail("Should have thrown ValidationException");
		}
		catch (javax.validation.ValidationException ex) {
			// expected
		}
	}


	@MyStereotype
	public static class MyValidBean implements MyValidInterface<String> {

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


	public interface MyValidInterface<T> {

		@NotNull Object myValidMethod(@NotNull(groups = MyGroup.class) String arg1, @Max(10) int arg2);

		@MyValid
		@Async void myValidAsyncMethod(@NotNull(groups = OtherGroup.class) String arg1, @Max(10) int arg2);

		T myGenericMethod(@NotNull T value);
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

}
