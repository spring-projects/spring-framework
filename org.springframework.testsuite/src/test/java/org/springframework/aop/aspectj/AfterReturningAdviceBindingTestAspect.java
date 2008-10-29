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
import org.springframework.beans.TestBean;

/**
 * @author Adrian Colyer
 * @author Juergen Hoeller
 */
public class AfterReturningAdviceBindingTestAspect extends AdviceBindingTestAspect {

	private AfterReturningAdviceBindingCollaborator getCollaborator() {
		return (AfterReturningAdviceBindingCollaborator) this.collaborator;
	}
	
	public void oneString(String name) {
		getCollaborator().oneString(name);
	}
	
	public void oneTestBeanArg(TestBean bean) {
		getCollaborator().oneTestBeanArg(bean);
	}
	
	public void testBeanArrayArg(ITestBean[] beans) {
		getCollaborator().testBeanArrayArg(beans);
	}

	public void objectMatchNoArgs() {
		getCollaborator().objectMatchNoArgs();
	}
	
	public void stringMatchNoArgs() {
		getCollaborator().stringMatchNoArgs();
	}
	
	public void oneInt(int result) {
		getCollaborator().oneInt(result);
	}
		

	interface AfterReturningAdviceBindingCollaborator extends AdviceBindingCollaborator {

		void oneString(String s);
		void oneTestBeanArg(TestBean b);
		void testBeanArrayArg(ITestBean[] b);
		void objectMatchNoArgs();
		void stringMatchNoArgs();
		void oneInt(int result);
	}

}
