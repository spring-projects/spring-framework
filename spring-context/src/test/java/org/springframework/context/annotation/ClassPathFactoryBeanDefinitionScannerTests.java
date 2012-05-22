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

import junit.framework.TestCase;

import org.springframework.aop.scope.ScopedObject;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.SimpleMapScope;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation4.DependencyBean;
import org.springframework.context.annotation4.FactoryMethodComponent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.ClassUtils;

/**
 * @author Mark Pollack
 * @author Juergen Hoeller
 */
public class ClassPathFactoryBeanDefinitionScannerTests extends TestCase {

	private static final String BASE_PACKAGE = FactoryMethodComponent.class.getPackage().getName();


	public void testSingletonScopedFactoryMethod() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);

		context.getBeanFactory().registerScope("request", new SimpleMapScope());

		scanner.scan(BASE_PACKAGE);
		context.registerBeanDefinition("clientBean", new RootBeanDefinition(QualifiedClientBean.class));
		context.refresh();

		FactoryMethodComponent fmc = context.getBean("factoryMethodComponent", FactoryMethodComponent.class);
		assertFalse(fmc.getClass().getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR));

		TestBean tb = (TestBean)context.getBean("publicInstance"); //2
		assertEquals("publicInstance", tb.getName());
		TestBean tb2 = (TestBean)context.getBean("publicInstance"); //2
		assertEquals("publicInstance", tb2.getName());
		assertSame(tb2, tb);

		tb = (TestBean)context.getBean("protectedInstance"); //3
		assertEquals("protectedInstance", tb.getName());
		assertSame(tb, context.getBean("protectedInstance"));
		assertEquals("0", tb.getCountry());
		tb2 = context.getBean("protectedInstance", TestBean.class); //3
		assertEquals("protectedInstance", tb2.getName());
		assertSame(tb2, tb);

		tb = context.getBean("privateInstance", TestBean.class); //4
		assertEquals("privateInstance", tb.getName());
		assertEquals(1, tb.getAge());
		tb2 = context.getBean("privateInstance", TestBean.class); //4
		assertEquals(2, tb2.getAge());
		assertNotSame(tb2, tb);

		Object bean = context.getBean("requestScopedInstance"); //5
		assertTrue(AopUtils.isCglibProxy(bean));
		assertTrue(bean instanceof ScopedObject);

		QualifiedClientBean clientBean = context.getBean("clientBean", QualifiedClientBean.class);
		assertSame(clientBean.testBean, context.getBean("publicInstance"));
		assertSame(clientBean.dependencyBean, context.getBean("dependencyBean"));
	}


	public static class QualifiedClientBean {

		@Autowired @Qualifier("public")
		public TestBean testBean;

		@Autowired
		public DependencyBean dependencyBean;
	}

}
