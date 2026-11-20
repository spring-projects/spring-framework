/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.expression;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.testfixture.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

/**
 * Integration tests for {@link EnvironmentAccessor}, which is registered with
 * SpEL for all {@link AbstractApplicationContext} implementations via
 * {@link StandardBeanExpressionResolver}.
 *
 * @author Chris Beams
 */
class EnvironmentAccessorIntegrationTests {

	@Test
	void braceAccess() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "#{environment['my.name']}")
					.getBeanDefinition());

		GenericApplicationContext ctx = new GenericApplicationContext(bf);
		ctx.getEnvironment().getPropertySources().addFirst(new MockPropertySource().withProperty("my.name", "myBean"));
		ctx.refresh();

		assertThat(ctx.getBean(TestBean.class).getName()).isEqualTo("myBean");
		ctx.close();
	}

}
