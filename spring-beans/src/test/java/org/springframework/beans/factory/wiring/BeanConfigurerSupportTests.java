/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.beans.factory.wiring;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class BeanConfigurerSupportTests {

	@Test(expected = IllegalArgumentException.class)
	public void supplyIncompatibleBeanFactoryImplementation() throws Exception {
		new StubBeanConfigurerSupport().setBeanFactory(mock(BeanFactory.class));
	}

	@Test
	public void configureBeanDoesNothingIfBeanWiringInfoResolverResolvesToNull() throws Exception {
		TestBean beanInstance = new TestBean();

		BeanWiringInfoResolver resolver = mock(BeanWiringInfoResolver.class);

		BeanConfigurerSupport configurer = new StubBeanConfigurerSupport();
		configurer.setBeanWiringInfoResolver(resolver);
		configurer.setBeanFactory(new DefaultListableBeanFactory());
		configurer.configureBean(beanInstance);
		verify(resolver).resolveWiringInfo(beanInstance);
		assertNull(beanInstance.getName());
	}

	@Test
	public void configureBeanDoesNothingIfNoBeanFactoryHasBeenSet() throws Exception {
		TestBean beanInstance = new TestBean();
		BeanConfigurerSupport configurer = new StubBeanConfigurerSupport();
		configurer.configureBean(beanInstance);
		assertNull(beanInstance.getName());
	}

	@Test
	public void configureBeanReallyDoesDefaultToUsingTheFullyQualifiedClassNameOfTheSuppliedBeanInstance() throws Exception {
		TestBean beanInstance = new TestBean();
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class);
		builder.addPropertyValue("name", "Harriet Wheeler");

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition(beanInstance.getClass().getName(), builder.getBeanDefinition());

		BeanConfigurerSupport configurer = new StubBeanConfigurerSupport();
		configurer.setBeanFactory(factory);
		configurer.afterPropertiesSet();
		configurer.configureBean(beanInstance);
		assertEquals("Bean is evidently not being configured (for some reason)", "Harriet Wheeler", beanInstance.getName());
	}

	@Test
	public void configureBeanPerformsAutowiringByNameIfAppropriateBeanWiringInfoResolverIsPluggedIn() throws Exception {
		TestBean beanInstance = new TestBean();
		// spouse for autowiring by name...
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class);
		builder.addConstructorArgValue("David Gavurin");

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("spouse", builder.getBeanDefinition());

		BeanWiringInfoResolver resolver = mock(BeanWiringInfoResolver.class);
		given(resolver.resolveWiringInfo(beanInstance)).willReturn(new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_NAME, false));

		BeanConfigurerSupport configurer = new StubBeanConfigurerSupport();
		configurer.setBeanFactory(factory);
		configurer.setBeanWiringInfoResolver(resolver);
		configurer.configureBean(beanInstance);
		assertEquals("Bean is evidently not being configured (for some reason)", "David Gavurin", beanInstance.getSpouse().getName());
	}

	@Test
	public void configureBeanPerformsAutowiringByTypeIfAppropriateBeanWiringInfoResolverIsPluggedIn() throws Exception {
		TestBean beanInstance = new TestBean();
		// spouse for autowiring by type...
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class);
		builder.addConstructorArgValue("David Gavurin");

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("Mmm, I fancy a salad!", builder.getBeanDefinition());

		BeanWiringInfoResolver resolver = mock(BeanWiringInfoResolver.class);
		given(resolver.resolveWiringInfo(beanInstance)).willReturn(new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_TYPE, false));

		BeanConfigurerSupport configurer = new StubBeanConfigurerSupport();
		configurer.setBeanFactory(factory);
		configurer.setBeanWiringInfoResolver(resolver);
		configurer.configureBean(beanInstance);
		assertEquals("Bean is evidently not being configured (for some reason)", "David Gavurin", beanInstance.getSpouse().getName());
	}


	private static class StubBeanConfigurerSupport extends BeanConfigurerSupport {
	}

}
