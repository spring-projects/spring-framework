/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.context.annotation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValueCglibConfiguration {

	private final String name;

	public ValueCglibConfiguration(@Value("${name:World}") String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@Bean
	public String dummy() {
		return "dummy";
	}

}
