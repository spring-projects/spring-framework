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

import static org.easymock.EasyMock.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.aspectj.AfterThrowingAdviceBindingTestAspect.AfterThrowingAdviceBindingCollaborator;
import org.springframework.beans.ITestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Tests for various parameter binding scenarios with before advice.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 */
public final class AfterThrowingAdviceBindingTests {

	private ITestBean testBean;

	private AfterThrowingAdviceBindingTestAspect afterThrowingAdviceAspect;

	private AfterThrowingAdviceBindingCollaborator mockCollaborator;

	@Before
	public void setUp() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		
		testBean = (ITestBean) ctx.getBean("testBean");
		afterThrowingAdviceAspect = (AfterThrowingAdviceBindingTestAspect) ctx.getBean("testAspect");
		
		mockCollaborator = createNiceMock(AfterThrowingAdviceBindingCollaborator.class);
		afterThrowingAdviceAspect.setCollaborator(mockCollaborator);
	}
	
	@After
	public void tearDown() {
		verify(mockCollaborator);
	}

	@Test(expected=Throwable.class)
	public void testSimpleAfterThrowing() throws Throwable {
		mockCollaborator.noArgs();
		replay(mockCollaborator);
		this.testBean.exceptional(new Throwable());
	}
	
	@Test(expected=Throwable.class)
	public void testAfterThrowingWithBinding() throws Throwable {
		Throwable t = new Throwable();
		mockCollaborator.oneThrowable(t);
		replay(mockCollaborator);
		this.testBean.exceptional(t);
	}
	
	@Test(expected=Throwable.class)
	public void testAfterThrowingWithNamedTypeRestriction() throws Throwable {
		Throwable t = new Throwable();
		// need a strict mock for this test...
		mockCollaborator = createMock(AfterThrowingAdviceBindingCollaborator.class);
		afterThrowingAdviceAspect.setCollaborator(mockCollaborator);
		
		mockCollaborator.noArgs();
		mockCollaborator.oneThrowable(t);
		mockCollaborator.noArgsOnThrowableMatch();
		replay(mockCollaborator);
		this.testBean.exceptional(t);
	}

	@Test(expected=Throwable.class)
	public void testAfterThrowingWithRuntimeExceptionBinding() throws Throwable {
		RuntimeException ex = new RuntimeException();
		mockCollaborator.oneRuntimeException(ex);
		replay(mockCollaborator);
		this.testBean.exceptional(ex);
	}

	@Test(expected=Throwable.class)
	public void testAfterThrowingWithTypeSpecified() throws Throwable {
		mockCollaborator.noArgsOnThrowableMatch();
		replay(mockCollaborator);
		this.testBean.exceptional(new Throwable());
	}

	@Test(expected=Throwable.class)
	public void testAfterThrowingWithRuntimeTypeSpecified() throws Throwable {
		mockCollaborator.noArgsOnRuntimeExceptionMatch();
		replay(mockCollaborator);
		this.testBean.exceptional(new RuntimeException());
		verify(mockCollaborator);
	}

}


final class AfterThrowingAdviceBindingTestAspect {

	// collaborator interface that makes it easy to test this aspect is 
	// working as expected through mocking.
	public interface AfterThrowingAdviceBindingCollaborator {
		void noArgs();
		void oneThrowable(Throwable t);
		void oneRuntimeException(RuntimeException re);
		void noArgsOnThrowableMatch();
		void noArgsOnRuntimeExceptionMatch();
	}
	
	protected AfterThrowingAdviceBindingCollaborator collaborator = null;
	
	public void setCollaborator(AfterThrowingAdviceBindingCollaborator aCollaborator) {
		this.collaborator = aCollaborator;
	}
	
	public void noArgs() {
		this.collaborator.noArgs();
	}
	
	public void oneThrowable(Throwable t) {
		this.collaborator.oneThrowable(t);
	}
	
	public void oneRuntimeException(RuntimeException ex) {
		this.collaborator.oneRuntimeException(ex);
	}
	
	public void noArgsOnThrowableMatch() {
		this.collaborator.noArgsOnThrowableMatch();
	}
	
	public void noArgsOnRuntimeExceptionMatch() {
		this.collaborator.noArgsOnRuntimeExceptionMatch();
	}

}
