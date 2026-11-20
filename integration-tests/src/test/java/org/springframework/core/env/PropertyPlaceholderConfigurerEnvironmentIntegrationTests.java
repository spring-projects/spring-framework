/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.env;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.GenericApplicationContext;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

class PropertyPlaceholderConfigurerEnvironmentIntegrationTests {

	@Test
	@SuppressWarnings({"deprecation", "removal"})
	void test() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.registerBeanDefinition("ppc",
				rootBeanDefinition(org.springframework.beans.factory.config.PropertyPlaceholderConfigurer.class)
				.addPropertyValue("searchSystemEnvironment", false)
				.getBeanDefinition());
		ctx.refresh();
		ctx.getBean("ppc");
		ctx.close();
	}

}
