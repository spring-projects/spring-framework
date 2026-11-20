/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.GenericXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AOP advice specific parsing tests.
 *
 * @author Stephane Nicoll
 */
class CacheAdviceParserTests {

	@Test
	void keyAndKeyGeneratorCannotBeSetTogether() {
		assertThatThrownBy(() -> new GenericXmlApplicationContext(
				"/org/springframework/cache/config/cache-advice-invalid.xml")
		).isInstanceOf(BeanDefinitionStoreException.class);
		// TODO better exception handling
	}

}
