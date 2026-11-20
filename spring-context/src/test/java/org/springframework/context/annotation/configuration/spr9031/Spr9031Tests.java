/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.configuration.spr9031;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.configuration.spr9031.scanpackage.Spr9031Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests cornering bug SPR-9031.
 *
 * @author Chris Beams
 * @since 3.1.1
 */
class Spr9031Tests {

	/**
	 * Use of @Import to register LowLevelConfig results in ASM-based annotation
	 * processing.
	 */
	@Test
	void withAsmAnnotationProcessing() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(HighLevelConfig.class);
		assertThat(ctx.getBean(LowLevelConfig.class).scanned).isNotNull();
		ctx.close();
	}

	/**
	 * Direct registration of LowLevelConfig results in reflection-based annotation
	 * processing.
	 */
	@Test
	void withoutAsmAnnotationProcessing() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(LowLevelConfig.class);
		assertThat(ctx.getBean(LowLevelConfig.class).scanned).isNotNull();
		ctx.close();
	}


	@Configuration
	@Import(LowLevelConfig.class)
	static class HighLevelConfig {}

	@Configuration
	@ComponentScan(
			basePackages = "org.springframework.context.annotation.configuration.spr9031.scanpackage",
			includeFilters = { @Filter(MarkerAnnotation.class) })
	static class LowLevelConfig {
		// fails to wire when LowLevelConfig is processed with ASM because nested @Filter
		// annotation is not parsed
		@Autowired Spr9031Component scanned;
	}

}
