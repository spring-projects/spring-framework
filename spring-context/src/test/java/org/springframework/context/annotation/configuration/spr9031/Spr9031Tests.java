/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation.configuration.spr9031;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.configuration.spr9031.scanpackage.Spr9031Component;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests cornering bug SPR-9031.
 *
 * @author Chris Beams
 * @since 3.1.1
 */
public class Spr9031Tests {

	/**
	 * Use of @Import to register LowLevelConfig results in ASM-based annotation
	 * processing.
	 */
	@Test
	public void withAsmAnnotationProcessing() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(HighLevelConfig.class);
		ctx.refresh();
		assertThat(ctx.getBean(LowLevelConfig.class).scanned, not(nullValue()));
	}

	/**
	 * Direct registration of LowLevelConfig results in reflection-based annotation
	 * processing.
	 */
	@Test
	public void withoutAsmAnnotationProcessing() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(LowLevelConfig.class);
		ctx.refresh();
		assertThat(ctx.getBean(LowLevelConfig.class).scanned, not(nullValue()));
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

	public @interface MarkerAnnotation {}
}
