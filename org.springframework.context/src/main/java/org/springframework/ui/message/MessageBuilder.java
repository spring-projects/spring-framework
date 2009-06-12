/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.ui.message;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.style.ToStringCreator;

/**
 * A convenient builder for building {@link MessageResolver} objects programmatically.
 * Often used by model code such as validation logic to conveniently record validation messages.
 * Supports the production of message resolvers that hard-code their message text,
 * as well as message resolvers that retrieve their text from a {@link MessageSource}.
 * 
 * Usage example:
 * <p>
 * <pre>
 * new MessageBuilder().
 *     severity(Severity.ERROR).
 *     code(&quot;invalidFormat&quot;).
 *     arg("mathForm.decimalField").
 *     arg("#,###.##").
 *     defaultText(&quot;The decimal field must be in format #,###.##&quot;).
 *     build();
 * </pre>
 * </p>
 * @author Keith Donald
 */
public class MessageBuilder {

	private Set<String> codes = new LinkedHashSet<String>();

	private Severity severity;

	private List<Object> args = new ArrayList<Object>();

	private String defaultText;

	/**
	 * Records the severity of the message.
	 * @return this, for fluent API usage
	 */
	public MessageBuilder severity(Severity severity) {
		this.severity = severity;
		return this;
	}
	
	/**
	 * Records that the message being built should try and resolve its text using the code provided.
	 * Adds the code to the codes list. Successive calls to this method add additional codes.
	 * Codes are applied in the order they are added.
	 * @param code the message code
	 * @return this, for fluent API usage
	 */
	public MessageBuilder code(String code) {
		codes.add(code);
		return this;
	}

	/**
	 * Records that the message being built has a variable argument.
	 * Adds the arg to the args list. Successive calls to this method add additional args.
	 * Args are applied in the order they are added.
	 * @param arg the message argument value
	 * @return this, for fluent API usage
	 */
	public MessageBuilder arg(Object arg) {
		args.add(arg);
		return this;
	}

	/**
	 * Records that the message being built has a variable argument, whose display value is also {@link MessageSourceResolvable}.
	 * Adds the arg to the args list. Successive calls to this method add additional resolvable args.
	 * Args are applied in the order they are added.
	 * @param arg the resolvable message argument
	 * @return this, for fluent API usage
	 */
	public MessageBuilder resolvableArg(Object arg) {
		args.add(new ResolvableArgument(arg));
		return this;
	}

	/**
	 * Records the fallback text of the message being built.
	 * If the message has no codes, this will always be used as the text.
	 * If the message has codes but none can be resolved, this will always be used as the text.
	 * @param text the default text
	 * @return this, for fluent API usage
	 */
	public MessageBuilder defaultText(String text) {
		defaultText = text;
		return this;
	}

	/**
	 * Builds the message that will be resolved.
	 * Call after recording builder instructions.
	 * @return the built message resolver
	 */
	public MessageResolver build() {
		if (severity == null) {
			severity = Severity.INFO;
		}
		if (codes == null && defaultText == null) {
			throw new IllegalArgumentException(
					"A message code or the message text is required to build this message resolver");
		}
		String[] codesArray = (String[]) codes.toArray(new String[codes.size()]);
		Object[] argsArray = args.toArray(new Object[args.size()]);
		return new DefaultMessageResolver(severity, codesArray, argsArray, defaultText);
	}

	private static class ResolvableArgument implements MessageSourceResolvable {

		private Object arg;

		public ResolvableArgument(Object arg) {
			this.arg = arg;
		}

		public Object[] getArguments() {
			return null;
		}

		public String[] getCodes() {
			return new String[] { arg.toString() };
		}

		public String getDefaultMessage() {
			return arg.toString();
		}

		public String toString() {
			return new ToStringCreator(this).append("arg", arg).toString();
		}

	}

}