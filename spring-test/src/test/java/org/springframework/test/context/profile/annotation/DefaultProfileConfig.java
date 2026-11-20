/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.profile.annotation;

import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Sam Brannen
 * @since 3.1
 */
@Configuration(proxyBeanMethods = false)
public class DefaultProfileConfig {

	@Bean
	public Pet pet() {
		return new Pet("Fido");
	}

}
