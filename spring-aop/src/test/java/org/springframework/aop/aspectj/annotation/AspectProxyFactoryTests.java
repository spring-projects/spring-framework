/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Test;
import test.aop.PerThisAspect;

import org.springframework.util.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class AspectProxyFactoryTests {

	@Test
	public void testWithNonAspect() {
		AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TestBean());
		assertThatIllegalArgumentException().isThrownBy(() ->
				proxyFactory.addAspect(TestBean.class));
	}

	@Test
	public void testWithSimpleAspect() throws Exception {
		TestBean bean = new TestBean();
		bean.setAge(2);
		AspectJProxyFactory proxyFactory = new AspectJProxyFactory(bean);
		proxyFactory.addAspect(MultiplyReturnValue.class);
		ITestBean proxy = proxyFactory.getProxy();
		assertThat(proxy.getAge()).as("Multiplication did not occur").isEqualTo((bean.getAge() * 2));
	}

	@Test
	public void testWithPerThisAspect() throws Exception {
		TestBean bean1 = new TestBean();
		TestBean bean2 = new TestBean();

		AspectJProxyFactory pf1 = new AspectJProxyFactory(bean1);
		pf1.addAspect(PerThisAspect.class);

		AspectJProxyFactory pf2 = new AspectJProxyFactory(bean2);
		pf2.addAspect(PerThisAspect.class);

		ITestBean proxy1 = pf1.getProxy();
		ITestBean proxy2 = pf2.getProxy();

		assertThat(proxy1.getAge()).isEqualTo(0);
		assertThat(proxy1.getAge()).isEqualTo(1);
		assertThat(proxy2.getAge()).isEqualTo(0);
		assertThat(proxy1.getAge()).isEqualTo(2);
	}

	@Test
	public void testWithInstanceWithNonAspect() throws Exception {
		AspectJProxyFactory pf = new AspectJProxyFactory();
		assertThatIllegalArgumentException().isThrownBy(() ->
				pf.addAspect(new TestBean()));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSerializable() throws Exception {
		AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TestBean());
		proxyFactory.addAspect(LoggingAspectOnVarargs.class);
		ITestBean proxy = proxyFactory.getProxy();
		assertThat(proxy.doWithVarargs(MyEnum.A, MyOtherEnum.C)).isTrue();
		ITestBean tb = (ITestBean) SerializationTestUtils.serializeAndDeserialize(proxy);
		assertThat(tb.doWithVarargs(MyEnum.A, MyOtherEnum.C)).isTrue();
	}

	@Test
	public void testWithInstance() throws Exception {
		MultiplyReturnValue aspect = new MultiplyReturnValue();
		int multiple = 3;
		aspect.setMultiple(multiple);

		TestBean target = new TestBean();
		target.setAge(24);

		AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
		proxyFactory.addAspect(aspect);

		ITestBean proxy = proxyFactory.getProxy();
		assertThat(proxy.getAge()).isEqualTo((target.getAge() * multiple));

		ITestBean serializedProxy = (ITestBean) SerializationTestUtils.serializeAndDeserialize(proxy);
		assertThat(serializedProxy.getAge()).isEqualTo((target.getAge() * multiple));
	}

	@Test
	public void testWithNonSingletonAspectInstance() throws Exception {
		AspectJProxyFactory pf = new AspectJProxyFactory();
		assertThatIllegalArgumentException().isThrownBy(() ->
				pf.addAspect(new PerThisAspect()));
	}

	@Test  // SPR-13328
	@SuppressWarnings("unchecked")
	public void testProxiedVarargsWithEnumArray() throws Exception {
		AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TestBean());
		proxyFactory.addAspect(LoggingAspectOnVarargs.class);
		ITestBean proxy = proxyFactory.getProxy();
		assertThat(proxy.doWithVarargs(MyEnum.A, MyOtherEnum.C)).isTrue();
	}

	@Test  // SPR-13328
	@SuppressWarnings("unchecked")
	public void testUnproxiedVarargsWithEnumArray() throws Exception {
		AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TestBean());
		proxyFactory.addAspect(LoggingAspectOnSetter.class);
		ITestBean proxy = proxyFactory.getProxy();
		assertThat(proxy.doWithVarargs(MyEnum.A, MyOtherEnum.C)).isTrue();
	}


	public interface ITestBean {

		int getAge();

		@SuppressWarnings("unchecked")
		<V extends MyInterface> boolean doWithVarargs(V... args);
	}


	@SuppressWarnings("serial")
	public static class TestBean implements ITestBean, Serializable {

		private int age;

		@Override
		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <V extends MyInterface> boolean doWithVarargs(V... args) {
			return true;
		}
	}


	public interface MyInterface {
	}


	public enum MyEnum implements MyInterface {

		A, B;
	}


	public enum MyOtherEnum implements MyInterface {

		C, D;
	}


	@Aspect
	@SuppressWarnings("serial")
	public static class LoggingAspectOnVarargs implements Serializable {

		@Around("execution(* doWithVarargs(*))")
		public Object doLog(ProceedingJoinPoint pjp) throws Throwable {
			LogFactory.getLog(LoggingAspectOnVarargs.class).debug(Arrays.asList(pjp.getArgs()));
			return pjp.proceed();
		}
	}


	@Aspect
	public static class LoggingAspectOnSetter {

		@Around("execution(* setAge(*))")
		public Object doLog(ProceedingJoinPoint pjp) throws Throwable {
			LogFactory.getLog(LoggingAspectOnSetter.class).debug(Arrays.asList(pjp.getArgs()));
			return pjp.proceed();
		}
	}
}


@Aspect
@SuppressWarnings("serial")
class MultiplyReturnValue implements Serializable {

	private int multiple = 2;

	public int invocations;

	public void setMultiple(int multiple) {
		this.multiple = multiple;
	}

	public int getMultiple() {
		return this.multiple;
	}

	@Around("execution(int *.getAge())")
	public Object doubleReturnValue(ProceedingJoinPoint pjp) throws Throwable {
		++this.invocations;
		int result = (Integer) pjp.proceed();
		return result * this.multiple;
	}

}
