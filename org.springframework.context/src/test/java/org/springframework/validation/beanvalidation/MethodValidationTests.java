/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.validation.beanvalidation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.hibernate.validator.method.MethodConstraintViolationException;
import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.validation.annotation.Validated;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @since 3.1
 */
public class MethodValidationTests {

	@Test
	public void testMethodValidationInterceptor() {
		MyValidBean bean = new MyValidBean();
		ProxyFactory proxyFactory = new ProxyFactory(bean);
		proxyFactory.addAdvice(new MethodValidationInterceptor());
		doTestProxyValidation((MyValidInterface) proxyFactory.getProxy());
	}

	@Test
	public void testMethodValidationPostProcessor() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("mvpp", MethodValidationPostProcessor.class);
		ac.registerSingleton("bean", MyValidBean.class);
		ac.refresh();
		doTestProxyValidation(ac.getBean("bean", MyValidInterface.class));
	}


	private void doTestProxyValidation(MyValidInterface proxy) {
		assertNotNull(proxy.myValidMethod("value", 5));
		try {
			assertNotNull(proxy.myValidMethod("value", 15));
			fail("Should have thrown MethodConstraintViolationException");
		}
		catch (MethodConstraintViolationException ex) {
			// expected
		}
		try {
			assertNotNull(proxy.myValidMethod(null, 5));
			fail("Should have thrown MethodConstraintViolationException");
		}
		catch (MethodConstraintViolationException ex) {
			// expected
		}
		try {
			assertNotNull(proxy.myValidMethod("value", 0));
			fail("Should have thrown MethodConstraintViolationException");
		}
		catch (MethodConstraintViolationException ex) {
			// expected
		}
	}


	@MyStereotype
	public static class MyValidBean implements MyValidInterface {

		public Object myValidMethod(String arg1, int arg2) {
			return (arg2 == 0 ? null : "value");
		}
	}


	public interface MyValidInterface {

		@NotNull Object myValidMethod(@NotNull(groups = MyGroup.class) String arg1, @Max(10) int arg2);
	}


	public interface MyGroup {
	}


	@Validated({MyGroup.class, Default.class})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MyStereotype {

	}

}
