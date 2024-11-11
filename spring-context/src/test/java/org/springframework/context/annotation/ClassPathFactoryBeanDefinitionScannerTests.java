/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.aop.scope.ScopedObject;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.annotation4.DependencyBean;
import org.springframework.context.annotation4.FactoryMethodComponent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.testfixture.SimpleMapScope;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 * @author Juergen Hoeller
 */
class ClassPathFactoryBeanDefinitionScannerTests {

	private static final String BASE_PACKAGE = FactoryMethodComponent.class.getPackage().getName();


	@Test
	void testSingletonScopedFactoryMethod() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);

		context.getBeanFactory().registerScope("request", new SimpleMapScope());

		scanner.scan(BASE_PACKAGE);
		context.registerBeanDefinition("clientBean", new RootBeanDefinition(QualifiedClientBean.class));
		context.refresh();

		FactoryMethodComponent fmc = context.getBean("factoryMethodComponent", FactoryMethodComponent.class);
		assertThat(fmc.getClass().getName()).doesNotContain(ClassUtils.CGLIB_CLASS_SEPARATOR);

		TestBean tb = (TestBean) context.getBean("publicInstance"); //2
		assertThat(tb.getName()).isEqualTo("publicInstance");
		TestBean tb2 = (TestBean) context.getBean("publicInstance"); //2
		assertThat(tb2.getName()).isEqualTo("publicInstance");
		assertThat(tb).isSameAs(tb2);

		tb = (TestBean) context.getBean("protectedInstance"); //3
		assertThat(tb.getName()).isEqualTo("protectedInstance");
		assertThat(context.getBean("protectedInstance")).isSameAs(tb);
		assertThat(tb.getCountry()).isEqualTo("0");
		tb2 = context.getBean("protectedInstance", TestBean.class); //3
		assertThat(tb2.getName()).isEqualTo("protectedInstance");
		assertThat(tb).isSameAs(tb2);

		tb = context.getBean("privateInstance", TestBean.class); //4
		assertThat(tb.getName()).isEqualTo("privateInstance");
		assertThat(tb.getAge()).isEqualTo(1);
		tb2 = context.getBean("privateInstance", TestBean.class); //4
		assertThat(tb2.getAge()).isEqualTo(2);
		assertThat(tb).isNotSameAs(tb2);

		Object bean = context.getBean("requestScopedInstance"); //5
		assertThat(AopUtils.isCglibProxy(bean)).isTrue();
		boolean condition = bean instanceof ScopedObject;
		assertThat(condition).isTrue();

		QualifiedClientBean clientBean = context.getBean("clientBean", QualifiedClientBean.class);
		assertThat(clientBean.testBean).isSameAs(context.getBean("publicInstance"));
		assertThat(clientBean.dependencyBean).isSameAs(context.getBean("dependencyBean"));
		assertThat(clientBean.applicationContext).isSameAs(context);
	}


	public static class QualifiedClientBean {

		@Autowired @Qualifier("public")
		public TestBean testBean;

		@Autowired
		public DependencyBean dependencyBean;

		@Autowired
		AbstractApplicationContext applicationContext;
	}

}
