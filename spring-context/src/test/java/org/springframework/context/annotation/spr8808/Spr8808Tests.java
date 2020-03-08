/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public class Spr8808Tests {

	/**
	 * This test failed with ConflictingBeanDefinitionException prior to fixes for
	 * SPR-8808.
	 */
	@Test
	public void repro() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(Config.class);
		ctx.refresh();
	}

}

@Configuration
@ComponentScan(basePackageClasses=Spr8808Tests.class) // scan *this* package
class Config {
}
