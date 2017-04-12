/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.context.annotation;

import org.junit.Test;

import org.springframework.context.annotation.componentscan.cycle.left.LeftConfig;
import org.springframework.context.annotation.componentscan.level1.Level1Config;
import org.springframework.context.annotation.componentscan.level2.Level2Config;
import org.springframework.context.annotation.componentscan.level3.Level3Component;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Tests ensuring that configuration classes marked with @ComponentScan
 * may be processed recursively
 *
 * @author Chris Beams
 * @since 3.1
 */
public class ComponentScanAnnotationRecursionTests {

	@Test
	public void recursion() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(Level1Config.class);
		ctx.refresh();

		// assert that all levels have been detected
		ctx.getBean(Level1Config.class);
		ctx.getBean(Level2Config.class);
		ctx.getBean(Level3Component.class);

		// assert that enhancement is working
		assertThat(ctx.getBean("level1Bean"), sameInstance(ctx.getBean("level1Bean")));
		assertThat(ctx.getBean("level2Bean"), sameInstance(ctx.getBean("level2Bean")));
	}

	public void evenCircularScansAreSupported() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(LeftConfig.class); // left scans right, and right scans left
		ctx.refresh();
		ctx.getBean("leftConfig");      // but this is handled gracefully
		ctx.getBean("rightConfig");     // and beans from both packages are available
	}

}
