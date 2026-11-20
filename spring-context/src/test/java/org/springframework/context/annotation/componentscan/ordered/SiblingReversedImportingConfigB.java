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
@Import(SiblingReversedImportingConfigA.class)
public class SiblingReversedImportingConfigB {

	@Bean(name = "b-imports-a")
	String bean() {
		return "valueFromBR";
	}

}
