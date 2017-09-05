/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.context;

import org.springframework.lang.Nullable;

/**
 * Interface for objects that are suitable for message resolution in a
 * {@link MessageSource}.
 *
 * <p>Spring's own validation error classes implement this interface.
 *
 * @author Juergen Hoeller
 * @see MessageSource#getMessage(MessageSourceResolvable, java.util.Locale)
 * @see org.springframework.validation.ObjectError
 * @see org.springframework.validation.FieldError
 */
@FunctionalInterface
public interface MessageSourceResolvable {

	/**
	 * Return the codes to be used to resolve this message, in the order that
	 * they should get tried. The last code will therefore be the default one.
	 * @return a String array of codes which are associated with this message
	 */
	@Nullable
	String[] getCodes();

	/**
	 * Return the array of arguments to be used to resolve this message.
	 * <p>The default implementation simply returns {@code null}.
	 * @return an array of objects to be used as parameters to replace
	 * placeholders within the message text
	 * @see java.text.MessageFormat
	 */
	@Nullable
	default Object[] getArguments() {
		return null;
	}

	/**
	 * Return the default message to be used to resolve this message.
	 * <p>The default implementation simply returns {@code null}.
	 * Note that the default message may be identical to the primary
	 * message code ({@link #getCodes()}), which effectively enforces
	 * {@link org.springframework.context.support.AbstractMessageSource#setUseCodeAsDefaultMessage}
	 * for this particular message.
	 * @return the default message, or {@code null} if no default
	 */
	@Nullable
	default String getDefaultMessage() {
		return null;
	}

}
