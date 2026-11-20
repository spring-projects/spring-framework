/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.componentscan.simple;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class SimpleComponent {

	@Bean
	public String exampleBean() {
		return "example";
	}

}
