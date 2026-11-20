/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.componentscan.ordered;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vladislav Kisel
 */
@Configuration
public class SiblingImportingConfigB {

	@Bean(name = "a-imports-b")
	String bean() {
		return "valueFromB";
	}

}
