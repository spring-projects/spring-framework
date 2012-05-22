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

package org.springframework.aop.interceptor;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;

import org.springframework.core.Constants;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

/**
 * <code>MethodInterceptor</code> implementation that allows for highly customizable
 * method-level tracing, using placeholders.
 *
 * <p>Trace messages are written on method entry, and if the method invocation succeeds
 * on method exit. If an invocation results in an exception, then an exception message
 * is written. The contents of these trace messages is fully customizable and special
 * placeholders are available to allow you to include runtime information in your log
 * messages. The placeholders available are:
 *
 * <p><ul>
 * <li><code>$[methodName]</code> - replaced with the name of the method being invoked</li>
 * <li><code>$[targetClassName]</code> - replaced with the name of the class that is
 * the target of the invocation</li>
 * <li><code>$[targetClassShortName]</code> - replaced with the short name of the class
 * that is the target of the invocation</li>
 * <li><code>$[returnValue]</code> - replaced with the value returned by the invocation</li>
 * <li><code>$[argumentTypes]</code> - replaced with a comma-separated list of the
 * short class names of the method arguments</li>
 * <li><code>$[arguments]</code> - replaced with a comma-separated list of the
 * <code>String</code> representation of the method arguments</li>
 * <li><code>$[exception]</code> - replaced with the <code>String</code> representation
 * of any <code>Throwable</code> raised during the invocation</li>
 * <li><code>$[invocationTime]</code> - replaced with the time, in milliseconds,
 * taken by the method invocation</li>
 * </ul>
 *
 * <p>There are restrictions on which placeholders can be used in which messages:
 * see the individual message properties for details on the valid placeholders.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setEnterMessage
 * @see #setExitMessage
 * @see #setExceptionMessage
 * @see SimpleTraceInterceptor
 */
public class CustomizableTraceInterceptor extends AbstractTraceInterceptor {

	/**
	 * The <code>$[methodName]</code> placeholder.
	 * Replaced with the name of the method being invoked.
	 */
	public static final String PLACEHOLDER_METHOD_NAME = "$[methodName]";

	/**
	 * The <code>$[targetClassName]</code> placeholder.
	 * Replaced with the fully-qualifed name of the <code>Class</code>
	 * of the method invocation target.
	 */
	public static final String PLACEHOLDER_TARGET_CLASS_NAME = "$[targetClassName]";

	/**
	 * The <code>$[targetClassShortName]</code> placeholder.
	 * Replaced with the short name of the <code>Class</code> of the
	 * method invocation target.
	 */
	public static final String PLACEHOLDER_TARGET_CLASS_SHORT_NAME = "$[targetClassShortName]";

	/**
	 * The <code>$[returnValue]</code> placeholder.
	 * Replaced with the <code>String</code> representation of the value
	 * returned by the method invocation.
	 */
	public static final String PLACEHOLDER_RETURN_VALUE = "$[returnValue]";

	/**
	 * The <code>$[argumentTypes]</code> placeholder.
	 * Replaced with a comma-separated list of the argument types for the
	 * method invocation. Argument types are written as short class names.
	 */
	public static final String PLACEHOLDER_ARGUMENT_TYPES = "$[argumentTypes]";

	/**
	 * The <code>$[arguments]</code> placeholder.
	 * Replaced with a comma separated list of the argument values for the
	 * method invocation. Relies on the <code>toString()</code> method of
	 * each argument type.
	 */
	public static final String PLACEHOLDER_ARGUMENTS = "$[arguments]";

	/**
	 * The <code>$[exception]</code> placeholder.
	 * Replaced with the <code>String</code> representation of any
	 * <code>Throwable</code> raised during method invocation.
	 */
	public static final String PLACEHOLDER_EXCEPTION = "$[exception]";

	/**
	 * The <code>$[invocationTime]</code> placeholder.
	 * Replaced with the time taken by the invocation (in milliseconds).
	 */
	public static final String PLACEHOLDER_INVOCATION_TIME = "$[invocationTime]";

	/**
	 * The default message used for writing method entry messages.
	 */
	private static final String DEFAULT_ENTER_MESSAGE =
			"Entering method '" + PLACEHOLDER_METHOD_NAME + "' of class [" + PLACEHOLDER_TARGET_CLASS_NAME + "]";

	/**
	 * The default message used for writing method exit messages.
	 */
	private static final String DEFAULT_EXIT_MESSAGE =
			"Exiting method '" + PLACEHOLDER_METHOD_NAME + "' of class [" + PLACEHOLDER_TARGET_CLASS_NAME + "]";

	/**
	 * The default message used for writing exception messages.
	 */
	private static final String DEFAULT_EXCEPTION_MESSAGE =
			"Exception thrown in method '" + PLACEHOLDER_METHOD_NAME + "' of class [" + PLACEHOLDER_TARGET_CLASS_NAME + "]";

	/**
	 * The <code>Pattern</code> used to match placeholders.
	 */
	private static final Pattern PATTERN = Pattern.compile("\\$\\[\\p{Alpha}+\\]");

	/**
	 * The <code>Set</code> of allowed placeholders.
	 */
	private static final Set<Object> ALLOWED_PLACEHOLDERS =
			new Constants(CustomizableTraceInterceptor.class).getValues("PLACEHOLDER_");


	/**
	 * The message for method entry.
	 */
	private String enterMessage = DEFAULT_ENTER_MESSAGE;

	/**
	 * The message for method exit.
	 */
	private String exitMessage = DEFAULT_EXIT_MESSAGE;

	/**
	 * The message for exceptions during method execution.
	 */
	private String exceptionMessage = DEFAULT_EXCEPTION_MESSAGE;


	/**
	 * Set the template used for method entry log messages.
	 * This template can contain any of the following placeholders:
	 * <ul>
	 * <li><code>$[targetClassName]</code></li>
	 * <li><code>$[targetClassShortName]</code></li>
	 * <li><code>$[argumentTypes]</code></li>
	 * <li><code>$[arguments]</code></li>
	 * </ul>
	 */
	public void setEnterMessage(String enterMessage) throws IllegalArgumentException {
		Assert.hasText(enterMessage, "'enterMessage' must not be empty");
		checkForInvalidPlaceholders(enterMessage);
		Assert.doesNotContain(enterMessage, PLACEHOLDER_RETURN_VALUE,
				"enterMessage cannot contain placeholder [" + PLACEHOLDER_RETURN_VALUE + "]");
		Assert.doesNotContain(enterMessage, PLACEHOLDER_EXCEPTION,
				"enterMessage cannot contain placeholder [" + PLACEHOLDER_EXCEPTION + "]");
		Assert.doesNotContain(enterMessage, PLACEHOLDER_INVOCATION_TIME,
				"enterMessage cannot contain placeholder [" + PLACEHOLDER_INVOCATION_TIME + "]");
		this.enterMessage = enterMessage;
	}

	/**
	 * Set the template used for method exit log messages.
	 * This template can contain any of the following placeholders:
	 * <ul>
	 * <li><code>$[targetClassName]</code></li>
	 * <li><code>$[targetClassShortName]</code></li>
	 * <li><code>$[argumentTypes]</code></li>
	 * <li><code>$[arguments]</code></li>
	 * <li><code>$[returnValue]</code></li>
	 * <li><code>$[invocationTime]</code></li>
	 * </ul>
	 */
	public void setExitMessage(String exitMessage) {
		Assert.hasText(exitMessage, "'exitMessage' must not be empty");
		checkForInvalidPlaceholders(exitMessage);
		Assert.doesNotContain(exitMessage, PLACEHOLDER_EXCEPTION,
				"exitMessage cannot contain placeholder [" + PLACEHOLDER_EXCEPTION + "]");
		this.exitMessage = exitMessage;
	}

	/**
	 * Set the template used for method exception log messages.
	 * This template can contain any of the following placeholders:
	 * <ul>
	 * <li><code>$[targetClassName]</code></li>
	 * <li><code>$[targetClassShortName]</code></li>
	 * <li><code>$[argumentTypes]</code></li>
	 * <li><code>$[arguments]</code></li>
	 * <li><code>$[exception]</code></li>
	 * </ul>
	 */
	public void setExceptionMessage(String exceptionMessage) {
		Assert.hasText(exceptionMessage, "'exceptionMessage' must not be empty");
		checkForInvalidPlaceholders(exceptionMessage);
		Assert.doesNotContain(exceptionMessage, PLACEHOLDER_RETURN_VALUE,
				"exceptionMessage cannot contain placeholder [" + PLACEHOLDER_RETURN_VALUE + "]");
		Assert.doesNotContain(exceptionMessage, PLACEHOLDER_INVOCATION_TIME,
				"exceptionMessage cannot contain placeholder [" + PLACEHOLDER_INVOCATION_TIME + "]");
		this.exceptionMessage = exceptionMessage;
	}


	/**
	 * Writes a log message before the invocation based on the value of <code>enterMessage</code>.
	 * If the invocation succeeds, then a log message is written on exit based on the value
	 * <code>exitMessage</code>. If an exception occurs during invocation, then a message is
	 * written based on the value of <code>exceptionMessage</code>.
	 * @see #setEnterMessage
	 * @see #setExitMessage
	 * @see #setExceptionMessage
	 */
	@Override
	protected Object invokeUnderTrace(MethodInvocation invocation, Log logger) throws Throwable {
		String name = invocation.getMethod().getDeclaringClass().getName() + "." + invocation.getMethod().getName();
		StopWatch stopWatch = new StopWatch(name);
		Object returnValue = null;
		boolean exitThroughException = false;
		try {
			stopWatch.start(name);
			writeToLog(logger,
					replacePlaceholders(this.enterMessage, invocation, null, null, -1));
			returnValue = invocation.proceed();
			return returnValue;
		}
		catch (Throwable ex) {
			if(stopWatch.isRunning()) {
				stopWatch.stop();
			}
			exitThroughException = true;
			writeToLog(logger,
					replacePlaceholders(this.exceptionMessage, invocation, null, ex, stopWatch.getTotalTimeMillis()), ex);
			throw ex;
		}
		finally {
			if (!exitThroughException) {
				if(stopWatch.isRunning()) {
					stopWatch.stop();
				}
				writeToLog(logger,
						replacePlaceholders(this.exitMessage, invocation, returnValue, null, stopWatch.getTotalTimeMillis()));
			}
		}
	}

	/**
	 * Writes the supplied message to the supplied <code>Log</code> instance.
	 * @see #writeToLog(org.apache.commons.logging.Log, String, Throwable)
	 */
	protected void writeToLog(Log logger, String message) {
		writeToLog(logger, message, null);
	}

	/**
	 * Writes the supplied message and {@link Throwable} to the
	 * supplied <code>Log</code> instance. By default messages are written
	 * at <code>TRACE</code> level. Sub-classes can override this method
	 * to control which level the message is written at.
	 */
	protected void writeToLog(Log logger, String message, Throwable ex) {
		if (ex != null) {
			logger.trace(message, ex);
		}
		else {
			logger.trace(message);
		}
	}

	/**
	 * Replace the placeholders in the given message with the supplied values,
	 * or values derived from those supplied.
	 * @param message the message template containing the placeholders to be replaced
	 * @param methodInvocation the <code>MethodInvocation</code> being logged.
	 * Used to derive values for all placeholders except <code>$[exception]</code>
	 * and <code>$[returnValue]</code>.
	 * @param returnValue any value returned by the invocation.
	 * Used to replace the <code>$[returnValue]</code> placeholder. May be <code>null</code>.
	 * @param throwable any <code>Throwable</code> raised during the invocation.
	 * The value of <code>Throwable.toString()</code> is replaced for the
	 * <code>$[exception]</code> placeholder. May be <code>null</code>.
	 * @param invocationTime the value to write in place of the
	 * <code>$[invocationTime]</code> placeholder
	 * @return the formatted output to write to the log
	 */
	protected String replacePlaceholders(String message, MethodInvocation methodInvocation,
			Object returnValue, Throwable throwable, long invocationTime) {

		Matcher matcher = PATTERN.matcher(message);

		StringBuffer output = new StringBuffer();
		while (matcher.find()) {
			String match = matcher.group();
			if (PLACEHOLDER_METHOD_NAME.equals(match)) {
				matcher.appendReplacement(output, escape(methodInvocation.getMethod().getName()));
			}
			else if (PLACEHOLDER_TARGET_CLASS_NAME.equals(match)) {
				String className = getClassForLogging(methodInvocation.getThis()).getName();
				matcher.appendReplacement(output, escape(className));
			}
			else if (PLACEHOLDER_TARGET_CLASS_SHORT_NAME.equals(match)) {
				String shortName = ClassUtils.getShortName(getClassForLogging(methodInvocation.getThis()));
				matcher.appendReplacement(output, escape(shortName));
			}
			else if (PLACEHOLDER_ARGUMENTS.equals(match)) {
				matcher.appendReplacement(output, escape(StringUtils.arrayToCommaDelimitedString(methodInvocation.getArguments())));
			}
			else if (PLACEHOLDER_ARGUMENT_TYPES.equals(match)) {
				appendArgumentTypes(methodInvocation, matcher, output);
			}
			else if (PLACEHOLDER_RETURN_VALUE.equals(match)) {
				appendReturnValue(methodInvocation, matcher, output, returnValue);
			}
			else if (throwable != null && PLACEHOLDER_EXCEPTION.equals(match)) {
				matcher.appendReplacement(output, escape(throwable.toString()));
			}
			else if (PLACEHOLDER_INVOCATION_TIME.equals(match)) {
				matcher.appendReplacement(output, Long.toString(invocationTime));
			}
			else {
				// Should not happen since placeholders are checked earlier.
				throw new IllegalArgumentException("Unknown placeholder [" + match + "]");
			}
		}
		matcher.appendTail(output);

		return output.toString();
	}

	/**
	 * Adds the <code>String</code> representation of the method return value
	 * to the supplied <code>StringBuffer</code>. Correctly handles
	 * <code>null</code> and <code>void</code> results.
	 * @param methodInvocation the <code>MethodInvocation</code> that returned the value
	 * @param matcher the <code>Matcher</code> containing the matched placeholder
	 * @param output the <code>StringBuffer</code> to write output to
	 * @param returnValue the value returned by the method invocation.
	 */
	private void appendReturnValue(
			MethodInvocation methodInvocation, Matcher matcher, StringBuffer output, Object returnValue) {

		if (methodInvocation.getMethod().getReturnType() == void.class) {
			matcher.appendReplacement(output, "void");
		}
		else if (returnValue == null) {
			matcher.appendReplacement(output, "null");
		}
		else {
			matcher.appendReplacement(output, escape(returnValue.toString()));
		}
	}

	/**
	 * Adds a comma-separated list of the short <code>Class</code> names of the
	 * method argument types to the output. For example, if a method has signature
	 * <code>put(java.lang.String, java.lang.Object)</code> then the value returned
	 * will be <code>String, Object</code>.
	 * @param methodInvocation the <code>MethodInvocation</code> being logged.
	 * Arguments will be retrieved from the corresponding <code>Method</code>.
	 * @param matcher the <code>Matcher</code> containing the state of the output
	 * @param output the <code>StringBuffer</code> containing the output
	 */
	private void appendArgumentTypes(MethodInvocation methodInvocation, Matcher matcher, StringBuffer output) {
		Class<?>[] argumentTypes = methodInvocation.getMethod().getParameterTypes();
		String[] argumentTypeShortNames = new String[argumentTypes.length];
		for (int i = 0; i < argumentTypeShortNames.length; i++) {
			argumentTypeShortNames[i] = ClassUtils.getShortName(argumentTypes[i]);
		}
		matcher.appendReplacement(output, escape(StringUtils.arrayToCommaDelimitedString(argumentTypeShortNames)));
	}

	/**
	 * Checks to see if the supplied <code>String</code> has any placeholders
	 * that are not specified as constants on this class and throws an
	 * <code>IllegalArgumentException</code> if so.
	 */
	private void checkForInvalidPlaceholders(String message) throws IllegalArgumentException {
		Matcher matcher = PATTERN.matcher(message);
		while (matcher.find()) {
			String match = matcher.group();
			if (!ALLOWED_PLACEHOLDERS.contains(match)) {
				throw new IllegalArgumentException("Placeholder [" + match + "] is not valid");
			}
		}
	}

	/**
	 * Replaces <code>$</code> in inner class names with <code>\$</code>.
	 * <p>This code is equivalent to JDK 1.5's <code>quoteReplacement</code>
	 * method in the Matcher class itself. We're keeping our own version
	 * here for JDK 1.4 compliance reasons only.
	 */
	private String escape(String input) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c == '\\') {
				sb.append("\\\\");
			}
			else if (c == '$') {
				sb.append("\\$");
			}
			else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

}
