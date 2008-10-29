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

import org.springframework.aop.aspectj.AfterThrowingAdviceBindingTestAspect.AfterThrowingAdviceBindingCollaborator;
import org.springframework.beans.ITestBean;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Tests for various parameter binding scenarios with before advice.
 *
 * @author Adrian Colyer
 */
public class AfterThrowingAdviceBindingTests extends AbstractDependencyInjectionSpringContextTests {

	private AfterThrowingAdviceBindingTestAspect afterThrowingAdviceAspect;

	private ITestBean testBean;

	private MockControl mockControl;

	private AfterThrowingAdviceBindingCollaborator mockCollaborator;


	public void setAfterAdviceAspect(AfterThrowingAdviceBindingTestAspect anAspect) {
		this.afterThrowingAdviceAspect = anAspect;
	}

	public void setTestBean(ITestBean aBean) throws Exception {
		this.testBean = aBean;
	}

	protected String getConfigPath() {
		return "afterThrowing-advice-tests.xml";
	}

	protected void onSetUp() throws Exception {
		mockControl = MockControl.createNiceControl(AfterThrowingAdviceBindingCollaborator.class);
		mockCollaborator = (AfterThrowingAdviceBindingCollaborator) mockControl.getMock();
		afterThrowingAdviceAspect.setCollaborator(mockCollaborator);
	}


	// Simple test to ensure all is well with the XML file.
	// Note that this implicitly tests that the arg-names binding is working.
	public void testParse() {
	}

	public void testSimpleAfterThrowing() {
		mockCollaborator.noArgs();
		mockControl.replay();
		try {
			this.testBean.exceptional(new Throwable());
			fail("should throw exception");
		} catch (Throwable t) {
			// no-op
		}
		mockControl.verify();
	}
	
	public void testAfterThrowingWithBinding() {
		Throwable t = new Throwable();
		mockCollaborator.oneThrowable(t);
		mockControl.replay();
		try {
			this.testBean.exceptional(t);
			fail("should throw exception");
		} catch (Throwable x) {
			// no-op
		}
		mockControl.verify();
	}
	
	public void testAfterThrowingWithNamedTypeRestriction() {
		Throwable t = new Throwable();
		// need a strict mock for this test...
		mockControl = MockControl.createControl(AfterThrowingAdviceBindingCollaborator.class);
		mockCollaborator = (AfterThrowingAdviceBindingCollaborator) mockControl.getMock();
		afterThrowingAdviceAspect.setCollaborator(mockCollaborator);
		
		mockCollaborator.noArgs();
		mockCollaborator.oneThrowable(t);
		mockCollaborator.noArgsOnThrowableMatch();
		mockControl.replay();
		try {
			this.testBean.exceptional(t);
			fail("should throw exception");
		} catch (Throwable x) {
			// no-op
		}
		mockControl.verify();		
	}

	public void testAfterThrowingWithRuntimeExceptionBinding() {
		RuntimeException ex = new RuntimeException();
		mockCollaborator.oneRuntimeException(ex);
		mockControl.replay();
		try {
			this.testBean.exceptional(ex);
			fail("should throw exception");
		} catch (Throwable x) {
			// no-op
		}
		mockControl.verify();
	}

	public void testAfterThrowingWithTypeSpecified() {
		mockCollaborator.noArgsOnThrowableMatch();
		mockControl.replay();
		try {
			this.testBean.exceptional(new Throwable());
			fail("should throw exception");
		} catch (Throwable t) {
			// no-op
		}
		mockControl.verify();
	}

	public void testAfterThrowingWithRuntimeTypeSpecified() {
		mockCollaborator.noArgsOnRuntimeExceptionMatch();
		mockControl.replay();
		try {
			this.testBean.exceptional(new RuntimeException());
			fail("should throw exception");
		} catch (Throwable t) {
			// no-op
		}
		mockControl.verify();
	}

}
