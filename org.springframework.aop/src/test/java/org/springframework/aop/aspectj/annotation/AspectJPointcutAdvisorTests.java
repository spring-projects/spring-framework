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

package org.springframework.aop.aspectj.annotation;

import junit.framework.TestCase;

import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcutTests;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.beans.TestBean;

/**
 * @author Rod Johnson 
 */
public class AspectJPointcutAdvisorTests extends TestCase {
	
	private AspectJAdvisorFactory af = new ReflectiveAspectJAdvisorFactory();

	public void testSingleton() throws SecurityException, NoSuchMethodException {
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
		ajexp.setExpression(AspectJExpressionPointcutTests.MATCH_ALL_METHODS);
		
		InstantiationModelAwarePointcutAdvisorImpl ajpa = new InstantiationModelAwarePointcutAdvisorImpl(af, ajexp, 
				new SingletonMetadataAwareAspectInstanceFactory(new AbstractAspectJAdvisorFactoryTests.ExceptionAspect(null),"someBean"), 
				TestBean.class.getMethod("getAge", (Class[]) null),1,"someBean");
		assertSame(Pointcut.TRUE, ajpa.getAspectMetadata().getPerClausePointcut());
		assertFalse(ajpa.isPerInstance());
	}
	
	public void testPerTarget() throws SecurityException, NoSuchMethodException {
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
		ajexp.setExpression(AspectJExpressionPointcutTests.MATCH_ALL_METHODS);
		
		InstantiationModelAwarePointcutAdvisorImpl ajpa = new InstantiationModelAwarePointcutAdvisorImpl(af, ajexp, 
				new SingletonMetadataAwareAspectInstanceFactory(new AbstractAspectJAdvisorFactoryTests.PerTargetAspect(),"someBean"), null, 1, "someBean");
		assertNotSame(Pointcut.TRUE, ajpa.getAspectMetadata().getPerClausePointcut());
		assertTrue(ajpa.getAspectMetadata().getPerClausePointcut() instanceof AspectJExpressionPointcut);
		assertTrue(ajpa.isPerInstance());
		
		assertTrue(ajpa.getAspectMetadata().getPerClausePointcut().getClassFilter().matches(TestBean.class));
		assertFalse(ajpa.getAspectMetadata().getPerClausePointcut().getMethodMatcher().matches(
				TestBean.class.getMethod("getAge", (Class[]) null),
				TestBean.class));
		
		assertTrue(ajpa.getAspectMetadata().getPerClausePointcut().getMethodMatcher().matches(
				TestBean.class.getMethod("getSpouse", (Class[]) null),
				TestBean.class));
	}
	
	
	public void testPerCflowTarget() {
		testIllegalInstantiationModel(AbstractAspectJAdvisorFactoryTests.PerCflowAspect.class);
	}
	
	public void testPerCflowBelowTarget() {
		testIllegalInstantiationModel(AbstractAspectJAdvisorFactoryTests.PerCflowBelowAspect.class);
	}
	
	private void testIllegalInstantiationModel(Class c) {
		try {
			new AspectMetadata(c,"someBean");
			fail();
		}
		catch (AopConfigException ex) {
			// OK
		}
	}

}
