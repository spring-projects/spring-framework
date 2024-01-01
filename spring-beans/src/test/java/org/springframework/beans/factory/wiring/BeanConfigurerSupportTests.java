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

package org.springframework.beans.factory.wiring;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class BeanConfigurerSupportTests {

	@Test
	void supplyIncompatibleBeanFactoryImplementation() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new StubBeanConfigurerSupport().setBeanFactory(mock()));
	}

	@Test
	void configureBeanDoesNothingIfBeanWiringInfoResolverResolvesToNull() {
		TestBean beanInstance = new TestBean();

		BeanWiringInfoResolver resolver = mock();

		BeanConfigurerSupport configurer = new StubBeanConfigurerSupport();
		configurer.setBeanWiringInfoResolver(resolver);
		configurer.setBeanFactory(new DefaultListableBeanFactory());
		configurer.configureBean(beanInstance);
		verify(resolver).resolveWiringInfo(beanInstance);
		assertThat(beanInstance.getName()).isNull();
	}

	@Test
	void configureBeanDoesNothingIfNoBeanFactoryHasBeenSet() {
		TestBean beanInstance = new TestBean();
		BeanConfigurerSupport configurer = new StubBeanConfigurerSupport();
		configurer.configureBean(beanInstance);
		assertThat(beanInstance.getName()).isNull();
	}

	@Test
	void configureBeanReallyDoesDefaultToUsingTheFullyQualifiedClassNameOfTheSuppliedBeanInstance() {
		TestBean beanInstance = new TestBean();
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class);
		builder.addPropertyValue("name", "Harriet Wheeler");

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition(beanInstance.getClass().getName(), builder.getBeanDefinition());

		BeanConfigurerSupport configurer = new StubBeanConfigurerSupport();
		configurer.setBeanFactory(factory);
		configurer.afterPropertiesSet();
		configurer.configureBean(beanInstance);
		assertThat(beanInstance.getName()).as("Bean is evidently not being configured (for some reason)").isEqualTo("Harriet Wheeler");
	}

	@Test
	void configureBeanPerformsAutowiringByNameIfAppropriateBeanWiringInfoResolverIsPluggedIn() {
		TestBean beanInstance = new TestBean();
		// spouse for autowiring by name...
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class);
		builder.addConstructorArgValue("David Gavurin");

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("spouse", builder.getBeanDefinition());

		BeanWiringInfoResolver resolver = mock();
		given(resolver.resolveWiringInfo(beanInstance)).willReturn(new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_NAME, false));

		BeanConfigurerSupport configurer = new StubBeanConfigurerSupport();
		configurer.setBeanFactory(factory);
		configurer.setBeanWiringInfoResolver(resolver);
		configurer.configureBean(beanInstance);
		assertThat(beanInstance.getSpouse().getName()).as("Bean is evidently not being configured (for some reason)").isEqualTo("David Gavurin");
	}

	@Test
	void configureBeanPerformsAutowiringByTypeIfAppropriateBeanWiringInfoResolverIsPluggedIn() {
		TestBean beanInstance = new TestBean();
		// spouse for autowiring by type...
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class);
		builder.addConstructorArgValue("David Gavurin");

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("Mmm, I fancy a salad!", builder.getBeanDefinition());

		BeanWiringInfoResolver resolver = mock();
		given(resolver.resolveWiringInfo(beanInstance)).willReturn(new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_TYPE, false));

		BeanConfigurerSupport configurer = new StubBeanConfigurerSupport();
		configurer.setBeanFactory(factory);
		configurer.setBeanWiringInfoResolver(resolver);
		configurer.configureBean(beanInstance);
		assertThat(beanInstance.getSpouse().getName()).as("Bean is evidently not being configured (for some reason)").isEqualTo("David Gavurin");
	}


	private static class StubBeanConfigurerSupport extends BeanConfigurerSupport {
	}

}
