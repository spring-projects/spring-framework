/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config.interfaces;

import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.config.interfaces.ContextConfigurationTestInterface.Config;

/**
 * @author Sam Brannen
 * @since 4.3
 */
@ContextConfiguration(classes = Config.class)
interface ContextConfigurationTestInterface {

	class Config {

		@Bean
		Employee employee() {
			return new Employee("Dilbert");
		}
	}

}
