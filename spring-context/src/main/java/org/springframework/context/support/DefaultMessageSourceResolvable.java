/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.context.support;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Spring's default implementation of the {@link MessageSourceResolvable} interface.
 * Offers an easy way to store all the necessary values needed to resolve
 * a message via a {@link org.springframework.context.MessageSource}.
 *
 * @author Juergen Hoeller
 * @since 13.02.2004
 * @see org.springframework.context.MessageSource#getMessage(MessageSourceResolvable, java.util.Locale)
 */
@SuppressWarnings("serial")
public class DefaultMessageSourceResolvable implements MessageSourceResolvable, Serializable {

	private final String @Nullable [] codes;

	private final Object @Nullable [] arguments;

	private final @Nullable String defaultMessage;


	/**
	 * Create a new DefaultMessageSourceResolvable.
	 * @param code the code to be used to resolve this message
	 */
	public DefaultMessageSourceResolvable(String code) {
		this(new String[] {code}, null, null);
	}

	/**
	 * Create a new DefaultMessageSourceResolvable.
	 * @param codes the codes to be used to resolve this message
	 */
	public DefaultMessageSourceResolvable(String[] codes) {
		this(codes, null, null);
	}

	/**
	 * Create a new DefaultMessageSourceResolvable.
	 * @param codes the codes to be used to resolve this message
	 * @param defaultMessage the default message to be used to resolve this message
	 */
	public DefaultMessageSourceResolvable(String[] codes, String defaultMessage) {
		this(codes, null, defaultMessage);
	}

	/**
	 * Create a new DefaultMessageSourceResolvable.
	 * @param codes the codes to be used to resolve this message
	 * @param arguments the array of arguments to be used to resolve this message
	 */
	public DefaultMessageSourceResolvable(String[] codes, Object[] arguments) {
		this(codes, arguments, null);
	}

	/**
	 * Create a new DefaultMessageSourceResolvable.
	 * @param codes the codes to be used to resolve this message
	 * @param arguments the array of arguments to be used to resolve this message
	 * @param defaultMessage the default message to be used to resolve this message
	 */
	public DefaultMessageSourceResolvable(
			String @Nullable [] codes, Object @Nullable [] arguments, @Nullable String defaultMessage) {

		this.codes = codes;
		this.arguments = arguments;
		this.defaultMessage = defaultMessage;
	}

	/**
	 * Copy constructor: Create a new instance from another resolvable.
	 * @param resolvable the resolvable to copy from
	 */
	public DefaultMessageSourceResolvable(MessageSourceResolvable resolvable) {
		this(resolvable.getCodes(), resolvable.getArguments(), resolvable.getDefaultMessage());
	}


	/**
	 * Return the default code of this resolvable, that is,
	 * the last one in the codes array.
	 */
	public @Nullable String getCode() {
		return (this.codes != null && this.codes.length > 0 ? this.codes[this.codes.length - 1] : null);
	}

	@Override
	public String @Nullable [] getCodes() {
		return this.codes;
	}

	@Override
	public Object @Nullable [] getArguments() {
		return this.arguments;
	}

	@Override
	public @Nullable String getDefaultMessage() {
		return this.defaultMessage;
	}

	/**
	 * Indicate whether the specified default message needs to be rendered for
	 * substituting placeholders and/or {@link java.text.MessageFormat} escaping.
	 * @return {@code true} if the default message may contain argument placeholders;
	 * {@code false} if it definitely does not contain placeholders or custom escaping
	 * and can therefore be simply exposed as-is
	 * @since 5.1.7
	 * @see #getDefaultMessage()
	 * @see #getArguments()
	 * @see AbstractMessageSource#renderDefaultMessage
	 */
	public boolean shouldRenderDefaultMessage() {
		return true;
	}


	/**
	 * Build a default String representation for this MessageSourceResolvable:
	 * including codes, arguments, and default message.
	 */
	protected final String resolvableToString() {
		StringBuilder result = new StringBuilder(64);
		result.append("codes [").append(StringUtils.arrayToDelimitedString(this.codes, ","));
		result.append("]; arguments [").append(StringUtils.arrayToDelimitedString(this.arguments, ","));
		result.append("]; default message [").append(this.defaultMessage).append(']');
		return result.toString();
	}

	/**
	 * The default implementation exposes the attributes of this MessageSourceResolvable.
	 * <p>To be overridden in more specific subclasses, potentially including the
	 * resolvable content through {@code resolvableToString()}.
	 * @see #resolvableToString()
	 */
	@Override
	public String toString() {
		return getClass().getName() + ": " + resolvableToString();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof MessageSourceResolvable that &&
				ObjectUtils.nullSafeEquals(getCodes(), that.getCodes()) &&
				ObjectUtils.nullSafeEquals(getArguments(), that.getArguments()) &&
				ObjectUtils.nullSafeEquals(getDefaultMessage(), that.getDefaultMessage())));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(getCode(), getArguments(), getDefaultMessage());
	}

}
