/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.aop.framework.adapter;

import junit.framework.*;

import org.springframework.aop.*;
import org.springframework.aop.support.*;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.ITestBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * TestCase for AdvisorAdapterRegistrationManager mechanism.
 *
 * @author Dmitriy Kopylenko
 */
public class AdvisorAdapterRegistrationTests extends TestCase {

    public AdvisorAdapterRegistrationTests(String name) {
        super(name);
    }

    public void testAdvisorAdapterRegistrationManagerNotPresentInContext() {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("/org/springframework/aop/framework/adapter/withoutBPPContext.xml");
        ITestBean tb = (ITestBean) ctx.getBean("testBean");
		// just invoke any method to see if advice fired
        try {
			tb.getName();
			fail("Should throw UnknownAdviceTypeException");
		}
		catch (UnknownAdviceTypeException ex) {
            // expected
			assertEquals(0, getAdviceImpl(tb).getInvocationCounter());
		}
	}

	public void testAdvisorAdapterRegistrationManagerPresentInContext() {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("/org/springframework/aop/framework/adapter/withBPPContext.xml");
		ITestBean tb = (ITestBean) ctx.getBean("testBean");
		// just invoke any method to see if advice fired
		try {
			tb.getName();
			assertEquals(1, getAdviceImpl(tb).getInvocationCounter());
		}
		catch (UnknownAdviceTypeException ex) {
			fail("Should not throw UnknownAdviceTypeException");
		}
	}

	private SimpleBeforeAdviceImpl getAdviceImpl(ITestBean tb) {
		Advised advised = (Advised) tb;
		Advisor advisor = advised.getAdvisors()[0];
		return (SimpleBeforeAdviceImpl) advisor.getAdvice();
	}

    // temporary suite method to make tests work on JRockit!
    // Alef knows more about this.
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new AdvisorAdapterRegistrationTests("testAdvisorAdapterRegistrationManagerNotPresentInContext"));
        suite.addTest(new AdvisorAdapterRegistrationTests("testAdvisorAdapterRegistrationManagerPresentInContext"));
        return suite;
    }

}
