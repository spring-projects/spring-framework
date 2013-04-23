/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.format.datetime.joda;

import org.joda.time.format.DateTimeFormatter;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * {@link FactoryBean} that creates a Joda-Time {@link DateTimeFormatter}.
 * See the {@link DateTimeFormatterFactory base class} for configuration details.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.2
 * @see #setPattern
 * @see #setIso
 * @see #setStyle
 * @see DateTimeFormatterFactory
 */
public class DateTimeFormatterFactoryBean extends DateTimeFormatterFactory
		implements FactoryBean<DateTimeFormatter>, InitializingBean {

	private DateTimeFormatter dateTimeFormatter;


	public void afterPropertiesSet() {
		this.dateTimeFormatter = createDateTimeFormatter();
	}

	public DateTimeFormatter getObject() {
		return this.dateTimeFormatter;
	}

	public Class<?> getObjectType() {
		return DateTimeFormatter.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
