/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
