/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;

/**
 * Utility methods to resolve a list of {@link MessageSourceResolvable}s, and
 * optionally join them.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
public abstract class BindErrorUtils {

	private final static MessageSource defaultMessageSource = new MethodArgumentErrorMessageSource();


	/**
	 * Shortcut for {@link #resolveAndJoin(List, MessageSource, Locale)} with
	 * an empty {@link MessageSource} that simply formats the default message,
	 * or first error code, also prepending the field name for field errors.
	 */
	public static String resolveAndJoin(List<? extends MessageSourceResolvable> errors) {
		return resolveAndJoin(errors, defaultMessageSource, Locale.getDefault());
	}

	/**
	 * Shortcut for {@link #resolveAndJoin(CharSequence, CharSequence, CharSequence, List, MessageSource, Locale)}
	 * with {@code ", and "} as delimiter, and an empty prefix and suffix.
	 */
	public static String resolveAndJoin(
			List<? extends MessageSourceResolvable> errors, MessageSource messageSource, Locale locale) {

		return resolveAndJoin(", and ", "", "", errors, messageSource, locale);
	}

	/**
	 * Resolve all errors through the given {@link MessageSource} and join them.
	 * @param delimiter the delimiter to use between each error
	 * @param prefix characters to insert at the beginning
	 * @param suffix characters to insert at the end
	 * @param errors the errors to resolve and join
	 * @param messageSource the {@code MessageSource} to resolve with
	 * @param locale the locale to resolve with
	 * @return the resolved errors formatted as a string
	 */
	public static String resolveAndJoin(
			CharSequence delimiter, CharSequence prefix, CharSequence suffix,
			List<? extends MessageSourceResolvable> errors, MessageSource messageSource, Locale locale) {

		return errors.stream()
				.map(error -> messageSource.getMessage(error, locale))
				.filter(StringUtils::hasText)
				.collect(Collectors.joining(delimiter, prefix, suffix));
	}

	/**
	 * Shortcut for {@link #resolve(List, MessageSource, Locale)} with an empty
	 * {@link MessageSource} that simply formats the default message, or first
	 * error code, also prepending the field name for field errors.
	 */
	public static <E extends MessageSourceResolvable> Map<E, String> resolve(List<E> errors) {
		return resolve(errors, defaultMessageSource, Locale.getDefault());
	}

	/**
	 * Resolve all errors through the given {@link MessageSource}.
	 * @param errors the errors to resolve
	 * @param messageSource the {@code MessageSource} to resolve with
	 * @param locale the locale to resolve with an empty {@link MessageSource}
	 * @return map with resolved errors as values, in the order of the input list
	 */
	public static <E extends MessageSourceResolvable> Map<E, String> resolve(
			List<E> errors, MessageSource messageSource, Locale locale) {

		Map<E, String> map = new LinkedHashMap<>(errors.size());
		errors.forEach(error -> map.put(error, messageSource.getMessage(error, locale)));
		return map;
	}


	/**
	 * {@code MessageSource} for default error formatting.
	 */
	private static class MethodArgumentErrorMessageSource extends StaticMessageSource {

		MethodArgumentErrorMessageSource() {
			setUseCodeAsDefaultMessage(true);
		}

		@Override
		protected String getDefaultMessage(MessageSourceResolvable resolvable, Locale locale) {
			String message = super.getDefaultMessage(resolvable, locale);
			return (resolvable instanceof FieldError error ? error.getField() + ": " + message : message);
		}
	}

}
