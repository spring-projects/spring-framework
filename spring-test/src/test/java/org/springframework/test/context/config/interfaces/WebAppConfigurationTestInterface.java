/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config.interfaces;

import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.config.interfaces.WebAppConfigurationTestInterface.Config;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * @author Sam Brannen
 * @since 4.3
 */
@WebAppConfiguration
@ContextConfiguration(classes = Config.class)
interface WebAppConfigurationTestInterface {

	@Configuration
	class Config {
		/* no user beans required for these tests */
	}

}
