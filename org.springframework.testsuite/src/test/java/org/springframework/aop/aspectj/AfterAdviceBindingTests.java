/*
 * Copyright 2002-2006 the original author or authors.
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

import org.easymock.MockControl;

import org.springframework.aop.aspectj.AdviceBindingTestAspect.AdviceBindingCollaborator;

/**
 * Tests for various parameter binding scenarios with before advice.
 *
 * @author Adrian Colyer
 * @author Rod Johnson
 */
public class AfterAdviceBindingTests extends AbstractAdviceBindingTests {

	private AdviceBindingTestAspect afterAdviceAspect;

	private MockControl mockControl;

	private AdviceBindingCollaborator mockCollaborator;


	public void setAfterAdviceAspect(AdviceBindingTestAspect anAspect) {
		this.afterAdviceAspect = anAspect;
	}

	protected String getConfigPath() {
		return "after-advice-tests.xml";
	}

	protected void onSetUp() throws Exception {
		super.onSetUp();
		mockControl = MockControl.createNiceControl(AdviceBindingCollaborator.class);
		mockCollaborator = (AdviceBindingCollaborator) mockControl.getMock();
		afterAdviceAspect.setCollaborator(mockCollaborator);
	}

	
	public void testOneIntArg() {
		mockCollaborator.oneIntArg(5);
		mockControl.replay();
		testBeanProxy.setAge(5);
		mockControl.verify();
	}
	
	public void testOneObjectArgBindingProxyWithThis() {
		mockCollaborator.oneObjectArg(this.testBeanProxy);
		mockControl.replay();
		testBeanProxy.getAge();
		mockControl.verify();
	}
	
	public void testOneObjectArgBindingTarget() {
		mockCollaborator.oneObjectArg(this.testBeanTarget);
		mockControl.replay();
		testBeanProxy.getDoctor();
		mockControl.verify();
	}
	
	public void testOneIntAndOneObjectArgs() {
		mockCollaborator.oneIntAndOneObject(5,this.testBeanProxy);
		mockControl.replay();
		testBeanProxy.setAge(5);
		mockControl.verify();
	}
	
	public void testNeedsJoinPoint() {
		mockCollaborator.needsJoinPoint("getAge");
		mockControl.replay();
		testBeanProxy.getAge();
		mockControl.verify();
	}
	
	public void testNeedsJoinPointStaticPart() {
		mockCollaborator.needsJoinPointStaticPart("getAge");
		mockControl.replay();
		testBeanProxy.getAge();
		mockControl.verify();
	}
	
}
