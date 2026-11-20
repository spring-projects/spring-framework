/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format.datetime.standard;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Phillip Webb
 * @author Sam Brannen
 */
class DateTimeFormatterFactoryBeanTests {

	private final DateTimeFormatterFactoryBean factory = new DateTimeFormatterFactoryBean();


	@Test
	void isSingleton() {
		assertThat(factory.isSingleton()).isTrue();
	}

	@Test
	void getObjectType() {
		assertThat(factory.getObjectType()).isEqualTo(DateTimeFormatter.class);
	}

	@Test
	void getObject() {
		factory.afterPropertiesSet();
		assertThat(factory.getObject().toString()).isEqualTo(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).toString());
	}

	@Test
	void getObjectIsAlwaysSingleton() {
		factory.afterPropertiesSet();
		DateTimeFormatter formatter = factory.getObject();
		assertThat(formatter.toString()).isEqualTo(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).toString());
		factory.setStylePattern("LL");
		assertThat(factory.getObject()).isSameAs(formatter);
	}

}
