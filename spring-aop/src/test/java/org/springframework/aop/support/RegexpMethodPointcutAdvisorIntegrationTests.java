/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.aop.support;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.testfixture.interceptor.NopInterceptor;
import org.springframework.aop.testfixture.interceptor.SerializableNopInterceptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.Person;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.Resource;
import org.springframework.core.testfixture.io.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class RegexpMethodPointcutAdvisorIntegrationTests {

	private static final Resource CONTEXT =
			qualifiedResource(RegexpMethodPointcutAdvisorIntegrationTests.class, "context.xml");


	@Test
	public void testSinglePattern() throws Throwable {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(CONTEXT);
		ITestBean advised = (ITestBean) bf.getBean("settersAdvised");
		// Interceptor behind regexp advisor
		NopInterceptor nop = (NopInterceptor) bf.getBean("nopInterceptor");
		assertThat(nop.getCount()).isEqualTo(0);

		int newAge = 12;
		// Not advised
		advised.exceptional(null);
		assertThat(nop.getCount()).isEqualTo(0);
		advised.setAge(newAge);
		assertThat(advised.getAge()).isEqualTo(newAge);
		// Only setter fired
		assertThat(nop.getCount()).isEqualTo(1);
	}

	@Test
	public void testMultiplePatterns() throws Throwable {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(CONTEXT);
		// This is a CGLIB proxy, so we can proxy it to the target class
		TestBean advised = (TestBean) bf.getBean("settersAndAbsquatulateAdvised");
		// Interceptor behind regexp advisor
		NopInterceptor nop = (NopInterceptor) bf.getBean("nopInterceptor");
		assertThat(nop.getCount()).isEqualTo(0);

		int newAge = 12;
		// Not advised
		advised.exceptional(null);
		assertThat(nop.getCount()).isEqualTo(0);

		// This is proxied
		advised.absquatulate();
		assertThat(nop.getCount()).isEqualTo(1);
		advised.setAge(newAge);
		assertThat(advised.getAge()).isEqualTo(newAge);
		// Only setter fired
		assertThat(nop.getCount()).isEqualTo(2);
	}

	@Test
	public void testSerialization() throws Throwable {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(CONTEXT);
		// This is a CGLIB proxy, so we can proxy it to the target class
		Person p = (Person) bf.getBean("serializableSettersAdvised");
		// Interceptor behind regexp advisor
		NopInterceptor nop = (NopInterceptor) bf.getBean("nopInterceptor");
		assertThat(nop.getCount()).isEqualTo(0);

		int newAge = 12;
		// Not advised
		assertThat(p.getAge()).isEqualTo(0);
		assertThat(nop.getCount()).isEqualTo(0);

		// This is proxied
		p.setAge(newAge);
		assertThat(nop.getCount()).isEqualTo(1);
		p.setAge(newAge);
		assertThat(p.getAge()).isEqualTo(newAge);
		// Only setter fired
		assertThat(nop.getCount()).isEqualTo(2);

		// Serialize and continue...
		p = (Person) SerializationTestUtils.serializeAndDeserialize(p);
		assertThat(p.getAge()).isEqualTo(newAge);
		// Remembers count, but we need to get a new reference to nop...
		nop = (SerializableNopInterceptor) ((Advised) p).getAdvisors()[0].getAdvice();
		assertThat(nop.getCount()).isEqualTo(2);
		assertThat(p.getName()).isEqualTo("serializableSettersAdvised");
		p.setAge(newAge + 1);
		assertThat(nop.getCount()).isEqualTo(3);
		assertThat(p.getAge()).isEqualTo((newAge + 1));
	}

}
