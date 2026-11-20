/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.validation.beanvalidation;

import java.util.Locale;
import java.util.ResourceBundle;

import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;

import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.util.Assert;

/**
 * Implementation of Hibernate Validator's {@link ResourceBundleLocator} interface,
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
