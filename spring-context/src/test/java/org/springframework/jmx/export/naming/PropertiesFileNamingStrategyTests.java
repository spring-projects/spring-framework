/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.naming;

import org.springframework.core.io.ClassPathResource;

/**
 * @author Juergen Hoeller
 */
class PropertiesFileNamingStrategyTests extends PropertiesNamingStrategyTests {

	@Override
	protected ObjectNamingStrategy getStrategy() throws Exception {
		KeyNamingStrategy strat = new KeyNamingStrategy();
		strat.setMappingLocation(new ClassPathResource("jmx-names.properties", getClass()));
		strat.afterPropertiesSet();
		return strat;
	}

}
