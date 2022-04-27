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

package org.springframework.format.datetime.joda;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Phillip Webb
 * @author Sam Brannen
 */
@SuppressWarnings("deprecation")
public class DateTimeFormatterFactoryBeanTests {

	private final DateTimeFormatterFactoryBean factory = new DateTimeFormatterFactoryBean();


	@Test
	public void isSingleton() {
		assertThat(factory.isSingleton()).isTrue();
	}

	@Test
	public void getObjectType() {
		assertThat(factory.getObjectType()).isEqualTo(DateTimeFormatter.class);
	}

	@Test
	public void getObject() {
		factory.afterPropertiesSet();
		assertThat(factory.getObject()).isEqualTo(DateTimeFormat.mediumDateTime());
	}

	@Test
	public void getObjectIsAlwaysSingleton() {
		factory.afterPropertiesSet();
		DateTimeFormatter formatter = factory.getObject();
		assertThat(formatter).isEqualTo(DateTimeFormat.mediumDateTime());
		factory.setStyle("LL");
		assertThat(factory.getObject()).isSameAs(formatter);
	}

}
