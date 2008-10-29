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

import org.easymock.MockControl;

import org.springframework.aop.aspectj.AfterReturningAdviceBindingTestAspect.AfterReturningAdviceBindingCollaborator;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Tests for various parameter binding scenarios with before advice.
 *
 * @author Adrian Colyer
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class AfterReturningAdviceBindingTests extends AbstractDependencyInjectionSpringContextTests {

	private AfterReturningAdviceBindingTestAspect afterAdviceAspect;

	private ITestBean testBeanProxy;

	private TestBean testBeanTarget;

	private MockControl mockControl;

	private AfterReturningAdviceBindingCollaborator mockCollaborator;
	

	public void setAfterReturningAdviceAspect(AfterReturningAdviceBindingTestAspect anAspect) {
		this.afterAdviceAspect = anAspect;
	}

	public void setTestBean(ITestBean aBean) throws Exception {
		assertTrue(AopUtils.isAopProxy(aBean));
		this.testBeanProxy = aBean;
		// we need the real target too, not just the proxy...
		this.testBeanTarget = (TestBean) ((Advised)aBean).getTargetSource().getTarget();
	}

	protected String getConfigPath() {
		return "afterReturning-advice-tests.xml";
	}
	
	protected void onSetUp() throws Exception {
		super.onSetUp();
		mockControl = MockControl.createNiceControl(AfterReturningAdviceBindingCollaborator.class);
		mockCollaborator = (AfterReturningAdviceBindingCollaborator) mockControl.getMock();
		afterAdviceAspect.setCollaborator(mockCollaborator);
	}


	// simple test to ensure all is well with the xml file
	// note that this implicitly tests that the arg-names binding is working
	public void testParse() {
	}
	
	public void testOneIntArg() {
		mockCollaborator.oneIntArg(5);
		mockControl.replay();
		testBeanProxy.setAge(5);
		mockControl.verify();
	}
	
	public void testOneObjectArg() {
		mockCollaborator.oneObjectArg(this.testBeanProxy);
		mockControl.replay();
		testBeanProxy.getAge();
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

	public void testReturningString() {
		mockCollaborator.oneString("adrian");
		mockControl.replay();
		testBeanProxy.setName("adrian");
		testBeanProxy.getName();
		mockControl.verify();
	}
	
	public void testReturningObject() {
		mockCollaborator.oneObjectArg(this.testBeanTarget);
		mockControl.replay();
		testBeanProxy.returnsThis();
		mockControl.verify();
	}
	
	public void testReturningBean() {
		mockCollaborator.oneTestBeanArg(this.testBeanTarget);
		mockControl.replay();
		testBeanProxy.returnsThis();
		mockControl.verify();
	}
	
	public void testReturningBeanArray() {
		this.testBeanTarget.setSpouse(new TestBean());
		ITestBean[] spouses = (ITestBean[]) this.testBeanTarget.getSpouses();
		mockCollaborator.testBeanArrayArg(spouses);
		mockControl.replay();
		testBeanProxy.getSpouses();
		mockControl.verify();
	}

	public void testNoInvokeWhenReturningParameterTypeDoesNotMatch() {
		// we need a strict mock for this...
		mockControl = MockControl.createControl(AfterReturningAdviceBindingCollaborator.class);
		mockCollaborator = (AfterReturningAdviceBindingCollaborator) mockControl.getMock();
		afterAdviceAspect.setCollaborator(mockCollaborator);
		
		mockControl.replay();
		testBeanProxy.setSpouse(this.testBeanProxy);
		testBeanProxy.getSpouse();
		mockControl.verify();
	}
	
	public void testReturningByType() {
		mockCollaborator.objectMatchNoArgs();
		mockControl.replay();
		testBeanProxy.returnsThis();
		mockControl.verify();
	}
	
	public void testReturningPrimitive() {
		mockCollaborator.oneInt(20);
		mockControl.replay();
		testBeanProxy.setAge(20);
		testBeanProxy.haveBirthday();
		mockControl.verify();
	}

}
