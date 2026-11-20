/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation6;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigForScanning {

	@Bean
	public TestBean testBean() {
		return new TestBean();
	}

}
