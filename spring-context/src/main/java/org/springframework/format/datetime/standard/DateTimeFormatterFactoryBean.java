/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format.datetime.standard;

import java.time.format.DateTimeFormatter;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * {@link FactoryBean} that creates a JSR-310 {@link java.time.format.DateTimeFormatter}.
 * See the {@link DateTimeFormatterFactory base class} for configuration details.
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see #setPattern
 * @see #setIso
 * @see #setDateStyle
 * @see #setTimeStyle
 * @see DateTimeFormatterFactory
 */
public class DateTimeFormatterFactoryBean extends DateTimeFormatterFactory
		implements FactoryBean<DateTimeFormatter>, InitializingBean {

	private @Nullable DateTimeFormatter dateTimeFormatter;


	@Override
	public void afterPropertiesSet() {
		this.dateTimeFormatter = createDateTimeFormatter();
	}

	@Override
	public @Nullable DateTimeFormatter getObject() {
		return this.dateTimeFormatter;
	}

	@Override
	public Class<?> getObjectType() {
		return DateTimeFormatter.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
