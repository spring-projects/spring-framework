/*
 * Copyright 2002-2015 the original author or authors.
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

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 */
public class Spr12278Tests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void componentSingleConstructor() {
		this.context = new AnnotationConfigApplicationContext(BaseConfiguration.class,
				SingleConstructorComponent.class);
		assertThat(this.context.getBean(SingleConstructorComponent.class).autowiredName, is("foo"));
	}

	@Test
	public void componentTwoConstructorsNoHint() {
		this.context = new AnnotationConfigApplicationContext(BaseConfiguration.class,
				TwoConstructorsComponent.class);
		assertThat(this.context.getBean(TwoConstructorsComponent.class).name, is("fallback"));
	}

	@Test
	public void componentTwoSpecificConstructorsNoHint() {
		thrown.expect(BeanCreationException.class);
		thrown.expectMessage(NoSuchMethodException.class.getName());
		new AnnotationConfigApplicationContext(BaseConfiguration.class,
				TwoSpecificConstructorsComponent.class);
	}


	@Configuration
	static class BaseConfiguration {

		@Bean
		public String autowiredName() {
			return "foo";
		}
	}

	private static class SingleConstructorComponent {

		private final String autowiredName;

		// No @Autowired - implicit wiring
		public SingleConstructorComponent(String autowiredName) {
			this.autowiredName = autowiredName;
		}

	}

	private static class TwoConstructorsComponent {

		private final String name;

		public TwoConstructorsComponent(String name) {
			this.name = name;
		}

		public TwoConstructorsComponent() {
			this("fallback");
		}
	}

	private static class TwoSpecificConstructorsComponent {

		private final Integer counter;

		public TwoSpecificConstructorsComponent(Integer counter) {
			this.counter = counter;
		}

		public TwoSpecificConstructorsComponent(String name) {
			this(Integer.valueOf(name));
		}
	}

}
