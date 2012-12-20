/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.wiring;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import test.beans.TestBean;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class BeanConfigurerSupportTests extends TestCase {

	public void testSupplyIncompatibleBeanFactoryImplementation() throws Exception {
		MockControl mock = MockControl.createControl(BeanFactory.class);
		mock.replay();
		try {
			new StubBeanConfigurerSupport().setBeanFactory((BeanFactory) mock.getMock());
			fail("Must have thrown an IllegalArgumentException by this point (incompatible BeanFactory implementation supplied)");
		}
		catch (IllegalArgumentException expected) {
		}
		mock.verify();
	}

	public void testConfigureBeanDoesNothingIfBeanWiringInfoResolverResolvesToNull() throws Exception {
		TestBean beanInstance = new TestBean();

		MockControl mock = MockControl.createControl(BeanWiringInfoResolver.class);
		BeanWiringInfoResolver resolver = (BeanWiringInfoResolver) mock.getMock();
		resolver.resolveWiringInfo(beanInstance);
		mock.setReturnValue(null);
		mock.replay();

		BeanConfigurerSupport configurer = new StubBeanConfigurerSupport();
		configurer.setBeanWiringInfoResolver(resolver);
		configurer.setBeanFactory(new DefaultListableBeanFactory());
		configurer.configureBean(beanInstance);
		mock.verify();
		assertNull(beanInstance.getName());
	}

	public void testConfigureBeanDoesNothingIfNoBeanFactoryHasBeenSet() throws Exception {
		TestBean beanInstance = new TestBean();
		BeanConfigurerSupport configurer = new StubBeanConfigurerSupport();
		configurer.configureBean(beanInstance);
		assertNull(beanInstance.getName());
	}

	public void testConfigureBeanReallyDoesDefaultToUsingTheFullyQualifiedClassNameOfTheSuppliedBeanInstance() throws Exception {
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

	public void testConfigureBeanPerformsAutowiringByNameIfAppropriateBeanWiringInfoResolverIsPluggedIn() throws Exception {
		TestBean beanInstance = new TestBean();
		// spouse for autowiring by name...
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class);
		builder.addConstructorArgValue("David Gavurin");

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("spouse", builder.getBeanDefinition());

		MockControl mock = MockControl.createControl(BeanWiringInfoResolver.class);
		BeanWiringInfoResolver resolver = (BeanWiringInfoResolver) mock.getMock();
		resolver.resolveWiringInfo(beanInstance);
		mock.setReturnValue(new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_NAME, false));
		mock.replay();

		BeanConfigurerSupport configurer = new StubBeanConfigurerSupport();
		configurer.setBeanFactory(factory);
		configurer.setBeanWiringInfoResolver(resolver);
		configurer.configureBean(beanInstance);
		assertEquals("Bean is evidently not being configured (for some reason)", "David Gavurin", beanInstance.getSpouse().getName());

		mock.verify();
	}

	public void testConfigureBeanPerformsAutowiringByTypeIfAppropriateBeanWiringInfoResolverIsPluggedIn() throws Exception {
		TestBean beanInstance = new TestBean();
		// spouse for autowiring by type...
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class);
		builder.addConstructorArgValue("David Gavurin");

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("Mmm, I fancy a salad!", builder.getBeanDefinition());

		MockControl mock = MockControl.createControl(BeanWiringInfoResolver.class);
		BeanWiringInfoResolver resolver = (BeanWiringInfoResolver) mock.getMock();
		resolver.resolveWiringInfo(beanInstance);
		mock.setReturnValue(new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_TYPE, false));
		mock.replay();

		BeanConfigurerSupport configurer = new StubBeanConfigurerSupport();
		configurer.setBeanFactory(factory);
		configurer.setBeanWiringInfoResolver(resolver);
		configurer.configureBean(beanInstance);
		assertEquals("Bean is evidently not being configured (for some reason)", "David Gavurin", beanInstance.getSpouse().getName());

		mock.verify();
	}


	private static class StubBeanConfigurerSupport extends BeanConfigurerSupport {
	}

}
