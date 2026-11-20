/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.componentscan.cycle.left.LeftConfig;
import org.springframework.context.annotation.componentscan.level1.Level1Config;
import org.springframework.context.annotation.componentscan.level2.Level2Config;
import org.springframework.context.annotation.componentscan.level3.Level3Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests ensuring that configuration classes marked with @ComponentScan
 * may be processed recursively
 *
 * @author Chris Beams
 * @since 3.1
 */
class ComponentScanAnnotationRecursionTests {

	@Test
	void recursion() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(Level1Config.class);
		ctx.refresh();

		// assert that all levels have been detected
		ctx.getBean(Level1Config.class);
		ctx.getBean(Level2Config.class);
		ctx.getBean(Level3Component.class);

		// assert that enhancement is working
		assertThat(ctx.getBean("level1Bean")).isSameAs(ctx.getBean("level1Bean"));
		assertThat(ctx.getBean("level2Bean")).isSameAs(ctx.getBean("level2Bean"));

		ctx.close();
	}

	@Test
	void evenCircularScansAreSupported() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(LeftConfig.class); // left scans right, and right scans left
		ctx.refresh();
		ctx.getBean("leftConfig");      // but this is handled gracefully
		ctx.getBean("rightConfig");     // and beans from both packages are available

		ctx.close();
	}

}
