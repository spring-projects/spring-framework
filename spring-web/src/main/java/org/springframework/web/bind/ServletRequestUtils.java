/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.bind;

import javax.servlet.ServletRequest;

import org.springframework.lang.Nullable;

/**
 * Parameter extraction methods, for an approach distinct from data binding,
 * in which parameters of specific types are required.
 *
 * <p>This approach is very useful for simple submissions, where binding
 * request parameters to a command object would be overkill.
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @since 2.0
 */
public abstract class ServletRequestUtils {

	private static final IntParser INT_PARSER = new IntParser();

	private static final LongParser LONG_PARSER = new LongParser();

	private static final FloatParser FLOAT_PARSER = new FloatParser();

	private static final DoubleParser DOUBLE_PARSER = new DoubleParser();

	private static final BooleanParser BOOLEAN_PARSER = new BooleanParser();

	private static final StringParser STRING_PARSER = new StringParser();


	/**
	 * Get an Integer parameter, or {@code null} if not present.
	 * Throws an exception if the parameter value isn't a number.
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @return the Integer value, or {@code null} if not present
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	@Nullable
	public static Integer getIntParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return getRequiredIntParameter(request, name);
	}

	/**
	 * Get an int parameter, with a fallback value. Never throws an exception.
	 * Can pass a distinguished value as default to enable checks of whether it was supplied.
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @param defaultVal the default value to use as fallback
	 */
	public static int getIntParameter(ServletRequest request, String name, int defaultVal) {
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			return getRequiredIntParameter(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return defaultVal;
		}
	}

	/**
	 * Get an array of int parameters, return an empty array if not found.
	 * @param request current HTTP request
	 * @param name the name of the parameter with multiple possible values
	 */
	public static int[] getIntParameters(ServletRequest request, String name) {
		try {
			return getRequiredIntParameters(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return new int[0];
		}
	}

	/**
	 * Get an int parameter, throwing an exception if it isn't found or isn't a number.
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	public static int getRequiredIntParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return INT_PARSER.parseInt(name, request.getParameter(name));
	}

	/**
	 * Get an array of int parameters, throwing an exception if not found or one is not a number.
	 * @param request current HTTP request
	 * @param name the name of the parameter with multiple possible values
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	public static int[] getRequiredIntParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return INT_PARSER.parseInts(name, request.getParameterValues(name));
	}


	/**
	 * Get a Long parameter, or {@code null} if not present.
	 * Throws an exception if the parameter value isn't a number.
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @return the Long value, or {@code null} if not present
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	@Nullable
	public static Long getLongParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return getRequiredLongParameter(request, name);
	}

	/**
	 * Get a long parameter, with a fallback value. Never throws an exception.
	 * Can pass a distinguished value as default to enable checks of whether it was supplied.
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @param defaultVal the default value to use as fallback
	 */
	public static long getLongParameter(ServletRequest request, String name, long defaultVal) {
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			return getRequiredLongParameter(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return defaultVal;
		}
	}

	/**
	 * Get an array of long parameters, return an empty array if not found.
	 * @param request current HTTP request
	 * @param name the name of the parameter with multiple possible values
	 */
	public static long[] getLongParameters(ServletRequest request, String name) {
		try {
			return getRequiredLongParameters(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return new long[0];
		}
	}

	/**
	 * Get a long parameter, throwing an exception if it isn't found or isn't a number.
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	public static long getRequiredLongParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return LONG_PARSER.parseLong(name, request.getParameter(name));
	}

	/**
	 * Get an array of long parameters, throwing an exception if not found or one is not a number.
	 * @param request current HTTP request
	 * @param name the name of the parameter with multiple possible values
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	public static long[] getRequiredLongParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return LONG_PARSER.parseLongs(name, request.getParameterValues(name));
	}


	/**
	 * Get a Float parameter, or {@code null} if not present.
	 * Throws an exception if the parameter value isn't a number.
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @return the Float value, or {@code null} if not present
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	@Nullable
	public static Float getFloatParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return getRequiredFloatParameter(request, name);
	}

	/**
	 * Get a float parameter, with a fallback value. Never throws an exception.
	 * Can pass a distinguished value as default to enable checks of whether it was supplied.
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @param defaultVal the default value to use as fallback
	 */
	public static float getFloatParameter(ServletRequest request, String name, float defaultVal) {
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			return getRequiredFloatParameter(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return defaultVal;
		}
	}

	/**
	 * Get an array of float parameters, return an empty array if not found.
	 * @param request current HTTP request
	 * @param name the name of the parameter with multiple possible values
	 */
	public static float[] getFloatParameters(ServletRequest request, String name) {
		try {
			return getRequiredFloatParameters(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return new float[0];
		}
	}

	/**
	 * Get a float parameter, throwing an exception if it isn't found or isn't a number.
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	public static float getRequiredFloatParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return FLOAT_PARSER.parseFloat(name, request.getParameter(name));
	}

	/**
	 * Get an array of float parameters, throwing an exception if not found or one is not a number.
	 * @param request current HTTP request
	 * @param name the name of the parameter with multiple possible values
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	public static float[] getRequiredFloatParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return FLOAT_PARSER.parseFloats(name, request.getParameterValues(name));
	}


	/**
	 * Get a Double parameter, or {@code null} if not present.
	 * Throws an exception if the parameter value isn't a number.
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @return the Double value, or {@code null} if not present
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	@Nullable
	public static Double getDoubleParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return getRequiredDoubleParameter(request, name);
	}

	/**
	 * Get a double parameter, with a fallback value. Never throws an exception.
	 * Can pass a distinguished value as default to enable checks of whether it was supplied.
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @param defaultVal the default value to use as fallback
	 */
	public static double getDoubleParameter(ServletRequest request, String name, double defaultVal) {
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			return getRequiredDoubleParameter(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return defaultVal;
		}
	}

	/**
	 * Get an array of double parameters, return an empty array if not found.
	 * @param request current HTTP request
	 * @param name the name of the parameter with multiple possible values
	 */
	public static double[] getDoubleParameters(ServletRequest request, String name) {
		try {
			return getRequiredDoubleParameters(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return new double[0];
		}
	}

	/**
	 * Get a double parameter, throwing an exception if it isn't found or isn't a number.
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	public static double getRequiredDoubleParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return DOUBLE_PARSER.parseDouble(name, request.getParameter(name));
	}

	/**
	 * Get an array of double parameters, throwing an exception if not found or one is not a number.
	 * @param request current HTTP request
	 * @param name the name of the parameter with multiple possible values
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	public static double[] getRequiredDoubleParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return DOUBLE_PARSER.parseDoubles(name, request.getParameterValues(name));
	}


	/**
	 * Get a Boolean parameter, or {@code null} if not present.
	 * Throws an exception if the parameter value isn't a boolean.
	 * <p>Accepts "true", "on", "yes" (any case) and "1" as values for true;
	 * treats every other non-empty value as false (i.e. parses leniently).
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @return the Boolean value, or {@code null} if not present
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	@Nullable
	public static Boolean getBooleanParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return (getRequiredBooleanParameter(request, name));
	}

	/**
	 * Get a boolean parameter, with a fallback value. Never throws an exception.
	 * Can pass a distinguished value as default to enable checks of whether it was supplied.
	 * <p>Accepts "true", "on", "yes" (any case) and "1" as values for true;
	 * treats every other non-empty value as false (i.e. parses leniently).
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @param defaultVal the default value to use as fallback
	 */
	public static boolean getBooleanParameter(ServletRequest request, String name, boolean defaultVal) {
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			return getRequiredBooleanParameter(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return defaultVal;
		}
	}

	/**
	 * Get an array of boolean parameters, return an empty array if not found.
	 * <p>Accepts "true", "on", "yes" (any case) and "1" as values for true;
	 * treats every other non-empty value as false (i.e. parses leniently).
	 * @param request current HTTP request
	 * @param name the name of the parameter with multiple possible values
	 */
	public static boolean[] getBooleanParameters(ServletRequest request, String name) {
		try {
			return getRequiredBooleanParameters(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return new boolean[0];
		}
	}

	/**
	 * Get a boolean parameter, throwing an exception if it isn't found
	 * or isn't a boolean.
	 * <p>Accepts "true", "on", "yes" (any case) and "1" as values for true;
	 * treats every other non-empty value as false (i.e. parses leniently).
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	public static boolean getRequiredBooleanParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return BOOLEAN_PARSER.parseBoolean(name, request.getParameter(name));
	}

	/**
	 * Get an array of boolean parameters, throwing an exception if not found
	 * or one isn't a boolean.
	 * <p>Accepts "true", "on", "yes" (any case) and "1" as values for true;
	 * treats every other non-empty value as false (i.e. parses leniently).
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	public static boolean[] getRequiredBooleanParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return BOOLEAN_PARSER.parseBooleans(name, request.getParameterValues(name));
	}


	/**
	 * Get a String parameter, or {@code null} if not present.
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @return the String value, or {@code null} if not present
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	@Nullable
	public static String getStringParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return getRequiredStringParameter(request, name);
	}

	/**
	 * Get a String parameter, with a fallback value. Never throws an exception.
	 * Can pass a distinguished value to default to enable checks of whether it was supplied.
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @param defaultVal the default value to use as fallback
	 */
	public static String getStringParameter(ServletRequest request, String name, String defaultVal) {
		String val = request.getParameter(name);
		return (val != null ? val : defaultVal);
	}

	/**
	 * Get an array of String parameters, return an empty array if not found.
	 * @param request current HTTP request
	 * @param name the name of the parameter with multiple possible values
	 */
	public static String[] getStringParameters(ServletRequest request, String name) {
		try {
			return getRequiredStringParameters(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return new String[0];
		}
	}

	/**
	 * Get a String parameter, throwing an exception if it isn't found.
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	public static String getRequiredStringParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return STRING_PARSER.validateRequiredString(name, request.getParameter(name));
	}

	/**
	 * Get an array of String parameters, throwing an exception if not found.
	 * @param request current HTTP request
	 * @param name the name of the parameter
	 * @throws ServletRequestBindingException a subclass of ServletException,
	 * so it doesn't need to be caught
	 */
	public static String[] getRequiredStringParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return STRING_PARSER.validateRequiredStrings(name, request.getParameterValues(name));
	}


	private abstract static class ParameterParser<T> {

		protected final T parse(String name, String parameter) throws ServletRequestBindingException {
			validateRequiredParameter(name, parameter);
			try {
				return doParse(parameter);
			}
			catch (NumberFormatException ex) {
				throw new ServletRequestBindingException(
						"Required " + getType() + " parameter '" + name + "' with value of '" +
						parameter + "' is not a valid number", ex);
			}
		}

		protected final void validateRequiredParameter(String name, @Nullable Object parameter)
				throws ServletRequestBindingException {

			if (parameter == null) {
				throw new MissingServletRequestParameterException(name, getType());
			}
		}

		protected abstract String getType();

		protected abstract T doParse(String parameter) throws NumberFormatException;
	}


	private static class IntParser extends ParameterParser<Integer> {

		@Override
		protected String getType() {
			return "int";
		}

		@Override
		protected Integer doParse(String s) throws NumberFormatException {
			return Integer.valueOf(s);
		}

		public int parseInt(String name, String parameter) throws ServletRequestBindingException {
			return parse(name, parameter);
		}

		public int[] parseInts(String name, String[] values) throws ServletRequestBindingException {
			validateRequiredParameter(name, values);
			int[] parameters = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				parameters[i] = parseInt(name, values[i]);
			}
			return parameters;
		}
	}


	private static class LongParser extends ParameterParser<Long> {

		@Override
		protected String getType() {
			return "long";
		}

		@Override
		protected Long doParse(String parameter) throws NumberFormatException {
			return Long.valueOf(parameter);
		}

		public long parseLong(String name, String parameter) throws ServletRequestBindingException {
			return parse(name, parameter);
		}

		public long[] parseLongs(String name, String[] values) throws ServletRequestBindingException {
			validateRequiredParameter(name, values);
			long[] parameters = new long[values.length];
			for (int i = 0; i < values.length; i++) {
				parameters[i] = parseLong(name, values[i]);
			}
			return parameters;
		}
	}


	private static class FloatParser extends ParameterParser<Float> {

		@Override
		protected String getType() {
			return "float";
		}

		@Override
		protected Float doParse(String parameter) throws NumberFormatException {
			return Float.valueOf(parameter);
		}

		public float parseFloat(String name, String parameter) throws ServletRequestBindingException {
			return parse(name, parameter);
		}

		public float[] parseFloats(String name, String[] values) throws ServletRequestBindingException {
			validateRequiredParameter(name, values);
			float[] parameters = new float[values.length];
			for (int i = 0; i < values.length; i++) {
				parameters[i] = parseFloat(name, values[i]);
			}
			return parameters;
		}
	}


	private static class DoubleParser extends ParameterParser<Double> {

		@Override
		protected String getType() {
			return "double";
		}

		@Override
		protected Double doParse(String parameter) throws NumberFormatException {
			return Double.valueOf(parameter);
		}

		public double parseDouble(String name, String parameter) throws ServletRequestBindingException {
			return parse(name, parameter);
		}

		public double[] parseDoubles(String name, String[] values) throws ServletRequestBindingException {
			validateRequiredParameter(name, values);
			double[] parameters = new double[values.length];
			for (int i = 0; i < values.length; i++) {
				parameters[i] = parseDouble(name, values[i]);
			}
			return parameters;
		}
	}


	private static class BooleanParser extends ParameterParser<Boolean> {

		@Override
		protected String getType() {
			return "boolean";
		}

		@Override
		protected Boolean doParse(String parameter) throws NumberFormatException {
			return (parameter.equalsIgnoreCase("true") || parameter.equalsIgnoreCase("on") ||
					parameter.equalsIgnoreCase("yes") || parameter.equals("1"));
		}

		public boolean parseBoolean(String name, String parameter) throws ServletRequestBindingException {
			return parse(name, parameter);
		}

		public boolean[] parseBooleans(String name, String[] values) throws ServletRequestBindingException {
			validateRequiredParameter(name, values);
			boolean[] parameters = new boolean[values.length];
			for (int i = 0; i < values.length; i++) {
				parameters[i] = parseBoolean(name, values[i]);
			}
			return parameters;
		}
	}


	private static class StringParser extends ParameterParser<String> {

		@Override
		protected String getType() {
			return "string";
		}

		@Override
		protected String doParse(String parameter) throws NumberFormatException {
			return parameter;
		}

		public String validateRequiredString(String name, String value) throws ServletRequestBindingException {
			validateRequiredParameter(name, value);
			return value;
		}

		public String[] validateRequiredStrings(String name, String[] values) throws ServletRequestBindingException {
			validateRequiredParameter(name, values);
			for (String value : values) {
				validateRequiredParameter(name, value);
			}
			return values;
		}
	}

}
