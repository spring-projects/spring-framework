/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.componentscan.level1;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("org.springframework.context.annotation.componentscan.level2")
public class Level1Config {
	@Bean
	public TestBean level1Bean() {
		return new TestBean("level1Bean");
	}
}
