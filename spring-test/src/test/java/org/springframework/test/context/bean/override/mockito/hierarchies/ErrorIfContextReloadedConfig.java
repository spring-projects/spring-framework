/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.hierarchies;

import jakarta.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;

@Configuration
class ErrorIfContextReloadedConfig {

	private static boolean loaded = false;


	@PostConstruct
	public void postConstruct() {
		if (loaded) {
			throw new RuntimeException("Context loaded multiple times");
		}
		loaded = true;
	}

}
