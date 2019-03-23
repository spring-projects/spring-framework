/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.aspectj.autoproxy;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import static org.junit.Assert.*;

/**
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public final class AtAspectJAnnotationBindingTests {

	private AnnotatedTestBean testBean;
	private ClassPathXmlApplicationContext ctx;


	@Before
	public void setUp() {
		ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
		testBean = (AnnotatedTestBean) ctx.getBean("testBean");
	}


	@Test
	public void testAnnotationBindingInAroundAdvice() {
		assertEquals("this value doThis", testBean.doThis());
		assertEquals("that value doThat", testBean.doThat());
		assertEquals(2, testBean.doArray().length);
	}

	@Test
	public void testNoMatchingWithoutAnnotationPresent() {
		assertEquals("doTheOther", testBean.doTheOther());
	}

	@Test
	public void testPointcutEvaulatedAgainstArray() {
		ctx.getBean("arrayFactoryBean");
	}

}


@Aspect
class AtAspectJAnnotationBindingTestAspect {

	@Around("execution(* *(..)) && @annotation(testAnn)")
	public Object doWithAnnotation(ProceedingJoinPoint pjp, TestAnnotation testAnn)
	throws Throwable {
		String annValue = testAnn.value();
		Object result = pjp.proceed();
		return (result instanceof String ? annValue + " " + result : result);
	}

}


class ResourceArrayFactoryBean implements FactoryBean<Object> {

	@Override
	@TestAnnotation("some value")
	public Object getObject() throws Exception {
		return new Resource[0];
	}

	@Override
	@TestAnnotation("some value")
	public Class<?> getObjectType() {
		return Resource[].class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}