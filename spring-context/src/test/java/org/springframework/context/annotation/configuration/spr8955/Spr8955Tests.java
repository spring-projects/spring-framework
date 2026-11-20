/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.configuration.spr8955;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Chris Beams
 * @author Willem Dekker
 */
class Spr8955Tests {

	@Test
	void repro() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.scan("org.springframework.context.annotation.configuration.spr8955");
		ctx.refresh();
		ctx.close();
	}

}
