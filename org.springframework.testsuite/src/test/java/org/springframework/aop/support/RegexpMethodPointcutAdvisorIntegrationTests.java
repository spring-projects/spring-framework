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

package org.springframework.aop.support;

import junit.framework.TestCase;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.interceptor.NopInterceptor;
import org.springframework.aop.interceptor.SerializableNopInterceptor;
import org.springframework.beans.ITestBean;
import org.springframework.beans.Person;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.SerializationTestUtils;

/**
 * @author Rod Johnson
 */
public class RegexpMethodPointcutAdvisorIntegrationTests extends TestCase {

	public void testSinglePattern() throws Throwable {
		BeanFactory bf = new ClassPathXmlApplicationContext("org/springframework/aop/support/regexpSetterTests.xml"); 
		ITestBean advised = (ITestBean) bf.getBean("settersAdvised");
		// Interceptor behind regexp advisor
		NopInterceptor nop = (NopInterceptor) bf.getBean("nopInterceptor");
		assertEquals(0, nop.getCount());
		
		int newAge = 12;
		// Not advised
		advised.exceptional(null);
		assertEquals(0, nop.getCount());
		advised.setAge(newAge);
		assertEquals(newAge, advised.getAge());
		// Only setter fired
		assertEquals(1, nop.getCount());
	}
	
	public void testMultiplePatterns() throws Throwable {
		BeanFactory bf = new ClassPathXmlApplicationContext("org/springframework/aop/support/regexpSetterTests.xml"); 
		// This is a CGLIB proxy, so we can proxy it to the target class
		TestBean advised = (TestBean) bf.getBean("settersAndAbsquatulateAdvised");
		// Interceptor behind regexp advisor
		NopInterceptor nop = (NopInterceptor) bf.getBean("nopInterceptor");
		assertEquals(0, nop.getCount());
	
		int newAge = 12;
		// Not advised
		advised.exceptional(null);
		assertEquals(0, nop.getCount());
		
		// This is proxied
		advised.absquatulate();
		assertEquals(1, nop.getCount());
		advised.setAge(newAge);
		assertEquals(newAge, advised.getAge());
		// Only setter fired
		assertEquals(2, nop.getCount());
	}
	
	public void testSerialization() throws Throwable {
		BeanFactory bf = new ClassPathXmlApplicationContext("org/springframework/aop/support/regexpSetterTests.xml"); 
		// This is a CGLIB proxy, so we can proxy it to the target class
		Person p = (Person) bf.getBean("serializableSettersAdvised");
		// Interceptor behind regexp advisor
		NopInterceptor nop = (NopInterceptor) bf.getBean("nopInterceptor");
		assertEquals(0, nop.getCount());
	
		int newAge = 12;
		// Not advised
		assertEquals(0, p.getAge());
		assertEquals(0, nop.getCount());
		
		// This is proxied
		p.setAge(newAge);
		assertEquals(1, nop.getCount());
		p.setAge(newAge);
		assertEquals(newAge, p.getAge());
		// Only setter fired
		assertEquals(2, nop.getCount());
		
		// Serialize and continue...
		p = (Person) SerializationTestUtils.serializeAndDeserialize(p);
		assertEquals(newAge, p.getAge());
		// Remembers count, but we need to get a new reference to nop...
		nop = (SerializableNopInterceptor) ((Advised) p).getAdvisors()[0].getAdvice();
		assertEquals(2, nop.getCount());
		assertEquals("serializableSettersAdvised", p.getName());
		p.setAge(newAge + 1);
		assertEquals(3, nop.getCount());
		assertEquals(newAge + 1, p.getAge());
	}

}
