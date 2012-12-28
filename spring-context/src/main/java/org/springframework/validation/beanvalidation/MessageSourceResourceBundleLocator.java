/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.validation.beanvalidation;

import java.util.Locale;
import java.util.ResourceBundle;

import org.hibernate.validator.resourceloading.ResourceBundleLocator;

import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.util.Assert;

/**
 * Implementation of Hibernate Validator 4.1's {@link ResourceBundleLocator} interface,
 * exposing a Spring {@link MessageSource} as localized {@link MessageSourceResourceBundle}.
 *
 * @author Juergen Hoeller
 * @since 3.0.4
 * @see ResourceBundleLocator
 * @see MessageSource
 * @see MessageSourceResourceBundle
 */
public class MessageSourceResourceBundleLocator implements ResourceBundleLocator {

	private final MessageSource messageSource;

	/**
	 * Build a MessageSourceResourceBundleLocator for the given MessageSource.
	 * @param messageSource the Spring MessageSource to wrap
	 */
	public MessageSourceResourceBundleLocator(MessageSource messageSource) {
		Assert.notNull(messageSource, "MessageSource must not be null");
		this.messageSource = messageSource;
	}

	@Override
	public ResourceBundle getResourceBundle(Locale locale) {
		return new MessageSourceResourceBundle(this.messageSource, locale);
	}

}
