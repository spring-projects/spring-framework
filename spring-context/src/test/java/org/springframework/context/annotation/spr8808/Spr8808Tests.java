/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.spr8808;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Tests cornering the bug in which @Configuration classes that @ComponentScan themselves
 * would result in a ConflictingBeanDefinitionException.
 *
 * @author Chris Beams
 * @since 3.1
 */
class Spr8808Tests {

	/**
	 * This test failed with ConflictingBeanDefinitionException prior to fixes for
	 * SPR-8808.
	 */
	@Test
	void repro() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
		ctx.close();
	}

}

@Configuration
@ComponentScan(basePackageClasses=Spr8808Tests.class) // scan *this* package
class Config {
}
