/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.env.scan1;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile(org.springframework.core.env.Constants.DEV_ENV_NAME)
@Configuration
class DevConfig {

	@Bean
	public Object devBean() {
		return new Object();
	}

}
