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

package org.springframework.aop.aspectj;

import org.springframework.beans.ITestBean;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class AspectJExpressionPointcutAdvisorTests extends AbstractDependencyInjectionSpringContextTests {

	private ITestBean testBean;

	private CallCountingInterceptor interceptor;


	public void setTestBean(ITestBean testBean) {
		this.testBean = testBean;
	}

	public void setInterceptor(CallCountingInterceptor interceptor) {
		this.interceptor = interceptor;
	}

	protected String getConfigPath() {
		return "aspectj.xml";
	}

	protected void onSetUp() throws Exception {
		interceptor.reset();
	}


	public void testPointcutting() throws Exception {
		assertEquals("Count should be 0", 0, interceptor.getCount());
		testBean.getSpouses();
		assertEquals("Count should be 1", 1, interceptor.getCount());
		testBean.getSpouse();
		assertEquals("Count should be 1", 1, interceptor.getCount());
	}

}
