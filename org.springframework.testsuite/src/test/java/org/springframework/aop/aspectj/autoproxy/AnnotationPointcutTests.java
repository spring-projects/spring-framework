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

package org.springframework.aop.aspectj.autoproxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * @author Juergen Hoeller
 */
public class AnnotationPointcutTests extends AbstractDependencyInjectionSpringContextTests {

	private AnnotatedTestBean testBean;

	@Override
	protected String getConfigPath() {
		return "annotationPointcut.xml";
	}

	public void setTestBean(AnnotatedTestBean testBean) {
		this.testBean = testBean;
	}

	public void testAnnotationBindingInAroundAdvice() {
		assertEquals("this value", testBean.doThis());
	}

	public void testNoMatchingWithoutAnnotationPresent() {
		assertEquals("doTheOther", testBean.doTheOther());
	}


	public static class TestMethodInterceptor implements MethodInterceptor {

		public Object invoke(MethodInvocation methodInvocation) throws Throwable {
			return "this value";
		}
	}

}
