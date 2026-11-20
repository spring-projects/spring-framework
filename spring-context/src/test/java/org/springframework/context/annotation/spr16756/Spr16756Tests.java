/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.spr16756;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Juergen Hoeller
 */
class Spr16756Tests {

	@Test
	void shouldNotFailOnNestedScopedComponent() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(ScanningConfiguration.class);
		context.refresh();
		context.getBean(ScannedComponent.class);
		context.getBean(ScannedComponent.State.class);
		context.close();
	}

}
