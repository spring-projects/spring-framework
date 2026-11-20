/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.context.annotation;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ConfigurableCglibConfiguration extends CglibConfiguration {

	private final Environment environment;

	public ConfigurableCglibConfiguration(Environment environment) {
		this.environment = environment;
	}

	@Override
	protected String getPrefix() {
		return this.environment.getProperty("test.prefix", String.class, "Howdy");
	}

}
