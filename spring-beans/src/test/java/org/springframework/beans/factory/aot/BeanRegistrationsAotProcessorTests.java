/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aot;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.AnnotatedBean;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanRegistrationsAotProcessor}.
 *
 * @author Phillip Webb
 * @author Sebastien Deleuze
 */
class BeanRegistrationsAotProcessorTests {

	@Test
	void beanRegistrationsAotProcessorIsRegistered() {
		assertThat(AotServices.factoriesAndBeans(new DefaultListableBeanFactory())
				.load(BeanFactoryInitializationAotProcessor.class))
				.anyMatch(BeanRegistrationsAotProcessor.class::isInstance);
	}

	@Test
	void processAheadOfTimeReturnsBeanRegistrationsAotContributionWithRegistrations() {
		BeanRegistrationsAotProcessor processor = new BeanRegistrationsAotProcessor();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("b1", new RootBeanDefinition(TestBean.class));
		beanFactory.registerBeanDefinition("b2",
				new RootBeanDefinition(AnnotatedBean.class));
		BeanRegistrationsAotContribution contribution = processor
				.processAheadOfTime(beanFactory);
		assertThat(contribution).extracting("registrations")
				.asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(2);
	}

	@Test
	void processAheadOfTimeReturnsBeanRegistrationsAotContributionWithAliases() {
		BeanRegistrationsAotProcessor processor = new BeanRegistrationsAotProcessor();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		beanFactory.registerAlias("test", "testAlias");
		BeanRegistrationsAotContribution contribution = processor
				.processAheadOfTime(beanFactory);
		assertThat(contribution).extracting("registrations").asInstanceOf(InstanceOfAssertFactories.LIST)
				.singleElement().satisfies(registration ->
						assertThat(registration).extracting("aliases").asInstanceOf(InstanceOfAssertFactories.ARRAY)
								.singleElement().isEqualTo("testAlias"));
	}

}
