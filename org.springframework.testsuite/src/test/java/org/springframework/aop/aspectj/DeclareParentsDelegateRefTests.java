/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * @author Ramnivas Laddad
 */
public class DeclareParentsDelegateRefTests extends AbstractDependencyInjectionSpringContextTests {

	protected NoMethodsBean noMethodsBean;

	protected CounterImpl counter;
	

	public DeclareParentsDelegateRefTests() {
		setPopulateProtectedVariables(true);
	}
	
	protected void onSetUp() throws Exception {
		counter.reset();
	}

	protected String getConfigPath() {
		return "declare-parents-delegate-ref-tests.xml";
	}


	public void testIntroductionWasMade() {
		assertTrue("Introduction must have been made", noMethodsBean instanceof Counter);
	}
	
	public void testIntroductionDelegation() {
		((Counter)noMethodsBean).increment();
		assertEquals("Delegate's counter should be updated", 1, counter.count);
	}

	public static interface NoMethodsBean {
	}

	public static class NoMethodsBeanImpl implements NoMethodsBean {
	}

	public static interface Counter {
		public void increment();
	}
	

	public static class CounterImpl implements Counter {

		int count;
		
		public void increment() {
			count++;
		}

		public void reset() {
			count = 0;
		}
	}

}
