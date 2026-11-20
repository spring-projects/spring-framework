/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.context.annotation.registrar;

import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ConfigurationBeanRegistrar implements BeanRegistrar {

	@Override
	public void register(BeanRegistry registry, Environment env) {
		registry.registerBean(IgnoredFromConfiguration.class);
	}

	@Bean
	BeanBeanRegistrar beanBeanRegistrar() {
		return new BeanBeanRegistrar();
	}

	public static class BeanBeanRegistrar implements BeanRegistrar {
		@Override
		public void register(BeanRegistry registry, Environment env) {
			registry.registerBean(IgnoredFromBean.class);
		}
	}


	public record IgnoredFromConfiguration() {}

	public record IgnoredFromBean() {}
}
