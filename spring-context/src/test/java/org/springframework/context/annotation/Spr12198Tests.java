/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;


/**
 * Unit tests for issue SPR-12198, in which the order of adding 
 * {@link PropertySource}s is fixed. When adding in the order
 * A then B then C then D, properties from D should override the ones from
 * C, properties from C should override properties from B and so on:
 * D > C > B > A. Before SPR-12198 was addressed the order was B > C > D > A.
 * 
 * @author Martin Becker
 */
public class Spr12198Tests {
	
	@Test
	public void orderingWithAndWithoutNameAndFourResourceLocations() {
		// p4 should 'win' as it was registered last
		AnnotationConfigApplicationContext ctxWithoutName = new AnnotationConfigApplicationContext(ConfigWithFourResourceLocations.class);
		assertThat(ctxWithoutName.getEnvironment().getProperty("testbean.name"), equalTo("p4TestBean"));
	}
	
	@Configuration
	@PropertySource(
			value = {
					"classpath:org/springframework/context/annotation/p1.properties",
					"classpath:org/springframework/context/annotation/p2.properties",
					"classpath:org/springframework/context/annotation/p3.properties",
					"classpath:org/springframework/context/annotation/p4.properties"
			})
	static class ConfigWithFourResourceLocations {
	}
	
}
