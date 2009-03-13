/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.context.annotation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import junit.framework.TestCase;

import org.springframework.aop.scope.ScopedObject;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SimpleMapScope;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.context.annotation4.FactoryMethodComponent;
import org.springframework.context.support.GenericApplicationContext;


public class ClassPathFactoryBeanDefinitionScannerTests extends TestCase {

	private static final String BASE_PACKAGE = FactoryMethodComponent.class.getPackage().getName();
	
	private static final int NUM_DEFAULT_BEAN_DEFS = 4;
	
	private static final int NUM_FACTORY_METHODS = 5;  // @ScopedProxy creates another
	
	private static final int NUM_COMPONENT_DEFS = 1;
	
		
	public void testSingletonScopedFactoryMethod()
	{
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		
		SimpleMapScope scope = new SimpleMapScope();
		context.getBeanFactory().registerScope("request", scope);		
		
		int beanCount = scanner.scan(BASE_PACKAGE);
				
		assertEquals(NUM_FACTORY_METHODS + NUM_COMPONENT_DEFS + NUM_DEFAULT_BEAN_DEFS, beanCount);
		assertTrue(context.containsBean("factoryMethodComponent"));		
		assertTrue(context.containsBean("factoryMethodComponent$staticInstance"));
		assertTrue(context.containsBean("factoryMethodComponent$getPublicInstance"));
		

		

		TestBean staticTestBean = (TestBean)context.getBean("factoryMethodComponent$staticInstance");//1
		assertEquals("staticInstance", staticTestBean.getName());
		TestBean staticTestBean2 =  (TestBean)context.getBean("factoryMethodComponent$staticInstance");//1
		assertSame(staticTestBean, staticTestBean2);
		
		TestBean tb = (TestBean)context.getBean("factoryMethodComponent$getPublicInstance"); //2
		assertEquals("publicInstance", tb.getName());
		TestBean tb2 = (TestBean)context.getBean("factoryMethodComponent$getPublicInstance"); //2
		assertEquals("publicInstance", tb2.getName());
		assertSame(tb2, tb);
		
		//Were qualifiers applied to bean definition
		ConfigurableListableBeanFactory cbf = (ConfigurableListableBeanFactory)context.getAutowireCapableBeanFactory();
		AbstractBeanDefinition abd = (AbstractBeanDefinition)cbf.getBeanDefinition("factoryMethodComponent$getPublicInstance"); //2
		Set<AutowireCandidateQualifier> qualifierSet = abd.getQualifiers();
		assertEquals(1, qualifierSet.size());
		
		
		tb = (TestBean)context.getBean("factoryMethodComponent$getProtectedInstance"); //3
		assertEquals("protectedInstance", tb.getName());
		tb2 = (TestBean)context.getBean("factoryMethodComponent$getProtectedInstance"); //3
		assertEquals("protectedInstance", tb2.getName());
		assertSame(tb2, tb);
		
		tb = (TestBean)context.getBean("factoryMethodComponent$getPrivateInstance"); //4
		assertEquals("privateInstance", tb.getName());
		assertEquals(0, tb.getAge());
		tb2 = (TestBean)context.getBean("factoryMethodComponent$getPrivateInstance"); //4
		assertEquals(1, tb2.getAge());
		assertNotSame(tb2, tb);
		
		Object bean = context.getBean("scopedTarget.factoryMethodComponent$requestScopedInstance"); //5
		assertNotNull(bean);
		assertTrue(bean instanceof ScopedObject);
	
		//Scope assertions
		assertTrue(AopUtils.isCglibProxy(bean));
		
		
		
		
	
	}
}
