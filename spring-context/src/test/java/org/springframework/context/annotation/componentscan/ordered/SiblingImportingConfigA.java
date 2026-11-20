/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.componentscan.ordered;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Vladislav Kisel
 */
@Configuration
@Import(SiblingImportingConfigB.class)
public class SiblingImportingConfigA {

	@Bean(name = "a-imports-b")
	String bean() {
		return "valueFromA";
	}

}
