/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import java.lang.reflect.Method;

import junit.framework.TestCase;

import org.springframework.aop.aspectj.AspectJAdviceParameterNameDiscoverer;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.test.AssertThrows;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Adrian Colyer
 * @author Juergen Hoeller
 */
public class ArgumentBindingTests extends TestCase {

	public void testBindingInPointcutUsedByAdvice() {
		TestBean tb = new TestBean();
		AspectJProxyFactory proxyFactory = new AspectJProxyFactory(tb);
		proxyFactory.addAspect(NamedPointcutWithArgs.class);
		final ITestBean proxiedTestBean = (ITestBean) proxyFactory.getProxy();
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				proxiedTestBean.setName("Supercalifragalisticexpialidocious");
			}
		}.runTest();
	}

	public void testAnnotationArgumentNameBinding() {
		TransactionalBean tb = new TransactionalBean();
		AspectJProxyFactory proxyFactory = new AspectJProxyFactory(tb);
		proxyFactory.addAspect(PointcutWithAnnotationArgument.class);
		final ITransactionalBean proxiedTestBean = (ITransactionalBean) proxyFactory.getProxy();
		new AssertThrows(IllegalStateException.class) {
			public void test() throws Exception {
				proxiedTestBean.doInTransaction();
			}
		}.runTest();
	}

	public void testParameterNameDiscoverWithReferencePointcut() throws Exception {
		AspectJAdviceParameterNameDiscoverer discoverer =
				new AspectJAdviceParameterNameDiscoverer("somepc(formal) && set(* *)");
		discoverer.setRaiseExceptions(true);
		Method methodUsedForParameterTypeDiscovery =
				getClass().getMethod("methodWithOneParam", String.class);
		String[] pnames = discoverer.getParameterNames(methodUsedForParameterTypeDiscovery);
		assertEquals("one parameter name", 1, pnames.length);
		assertEquals("formal", pnames[0]);
	}

	public void methodWithOneParam(String aParam) {
	}


	public interface ITransactionalBean {

		@Transactional
		void doInTransaction();
	}


	public static class TransactionalBean implements ITransactionalBean {

		@Transactional
		public void doInTransaction() {
		}
	}

}
