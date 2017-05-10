/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.context.support;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;

/**
 * {@link org.springframework.context.MessageSource} implementation that
 * resolves messages via underlying {@link Properties}.
 * 
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 */
public class PropertiesMessageSource extends AbstractMessageSource {

	private Properties properties;

	/**
	 * Set properties to use.
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	@Override
	protected MessageFormat resolveCode(String code, Locale locale) {
		String property = properties.getProperty(code);

		if (property == null) {
			return null;
		}

		return createMessageFormat(property, locale);
	}
}
