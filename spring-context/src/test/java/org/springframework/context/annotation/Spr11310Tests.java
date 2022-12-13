/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.context.annotation;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
class Spr11310Tests {

	@Test
	void orderedList() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		StringHolder holder = context.getBean(StringHolder.class);
		assertThat(holder.itemsList).containsExactly("second", "first", "unknownOrder");
		context.close();
	}

	@Test
	void orderedArray() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		StringHolder holder = context.getBean(StringHolder.class);
		assertThat(holder.itemsArray).containsExactly("second", "first", "unknownOrder");
		context.close();
	}


	@Configuration
	static class Config {

		@Bean
		@Order(50)
		public String first() {
			return "first";
		}

		@Bean
		public String unknownOrder() {
			return "unknownOrder";
		}

		@Bean
		@Order(5)
		public String second() {
			return "second";
		}

		@Bean
		public StringHolder stringHolder() {
			return new StringHolder();
		}

	}


	private static class StringHolder {
		@Autowired
		private List<String> itemsList;

		@Autowired
		private String[] itemsArray;

	}
}
