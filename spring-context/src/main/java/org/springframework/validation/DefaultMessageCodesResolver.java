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

package org.springframework.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link MessageCodesResolver} interface.
 *
 * <p>Will create two message codes for an object error, in the following order (when
 * using the {@link Format#PREFIX_ERROR_CODE prefixed}
 * {@link #setMessageCodeFormatter(MessageCodeFormatter) formatter}):
 * <ul>
 * <li>1.: code + "." + object name
 * <li>2.: code
 * </ul>
 *
 * <p>Will create four message codes for a field specification, in the following order:
 * <ul>
 * <li>1.: code + "." + object name + "." + field
 * <li>2.: code + "." + field
 * <li>3.: code + "." + field type
 * <li>4.: code
 * </ul>
 *
 * <p>For example, in case of code "typeMismatch", object name "user", field "age":
 * <ul>
 * <li>1. try "typeMismatch.user.age"
 * <li>2. try "typeMismatch.age"
 * <li>3. try "typeMismatch.int"
 * <li>4. try "typeMismatch"
 * </ul>
 *
 * <p>This resolution algorithm thus can be leveraged for example to show
 * specific messages for binding errors like "required" and "typeMismatch":
 * <ul>
 * <li>at the object + field level ("age" field, but only on "user");
 * <li>at the field level (all "age" fields, no matter which object name);
 * <li>or at the general level (all fields, on any object).
 * </ul>
 *
 * <p>In case of array, {@link List} or {@link java.util.Map} properties,
 * both codes for specific elements and for the whole collection are
 * generated. Assuming a field "name" of an array "groups" in object "user":
 * <ul>
 * <li>1. try "typeMismatch.user.groups[0].name"
 * <li>2. try "typeMismatch.user.groups.name"
 * <li>3. try "typeMismatch.groups[0].name"
 * <li>4. try "typeMismatch.groups.name"
 * <li>5. try "typeMismatch.name"
 * <li>6. try "typeMismatch.java.lang.String"
 * <li>7. try "typeMismatch"
 * </ul>
 *
 * <p>By default the {@code errorCode}s will be placed at the beginning of constructed
 * message strings. The {@link #setMessageCodeFormatter(MessageCodeFormatter)
 * messageCodeFormatter} property can be used to specify an alternative concatenation
 * {@link MessageCodeFormatter format}.
 *
 * <p>In order to group all codes into a specific category within your resource bundles,
 * e.g. "validation.typeMismatch.name" instead of the default "typeMismatch.name",
 * consider specifying a {@link #setPrefix prefix} to be applied.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Chris Beams
 * @since 1.0.1
 */
@SuppressWarnings("serial")
public class DefaultMessageCodesResolver implements MessageCodesResolver, Serializable {

	/**
	 * The separator that this implementation uses when resolving message codes.
	 */
	public static final String CODE_SEPARATOR = ".";

	private static final MessageCodeFormatter DEFAULT_FORMATTER = Format.PREFIX_ERROR_CODE;


	private String prefix = "";

	private MessageCodeFormatter formatter = DEFAULT_FORMATTER;


	/**
	 * Specify a prefix to be applied to any code built by this resolver.
	 * <p>Default is none. Specify, for example, "validation." to get
	 * error codes like "validation.typeMismatch.name".
	 */
	public void setPrefix(String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * Return the prefix to be applied to any code built by this resolver.
	 * <p>Returns an empty String in case of no prefix.
	 */
	protected String getPrefix() {
		return this.prefix;
	}

	/**
	 * Specify the format for message codes built by this resolver.
	 * <p>The default is {@link Format#PREFIX_ERROR_CODE}.
	 * @since 3.2
	 * @see Format
	 */
	public void setMessageCodeFormatter(MessageCodeFormatter formatter) {
		this.formatter = (formatter != null ? formatter : DEFAULT_FORMATTER);
	}


	@Override
	public String[] resolveMessageCodes(String errorCode, String objectName) {
		return resolveMessageCodes(errorCode, objectName, "", null);
	}

	/**
	 * Build the code list for the given code and field: an
	 * object/field-specific code, a field-specific code, a plain error code.
	 * <p>Arrays, Lists and Maps are resolved both for specific elements and
	 * the whole collection.
	 * <p>See the {@link DefaultMessageCodesResolver class level javadoc} for
	 * details on the generated codes.
	 * @return the list of codes
	 */
	@Override
	public String[] resolveMessageCodes(String errorCode, String objectName, String field, Class<?> fieldType) {
		Set<String> codeList = new LinkedHashSet<String>();
		List<String> fieldList = new ArrayList<String>();
		buildFieldList(field, fieldList);
		addCodes(codeList, errorCode, objectName, fieldList);
		int dotIndex = field.lastIndexOf('.');
		if (dotIndex != -1) {
			buildFieldList(field.substring(dotIndex + 1), fieldList);
		}
		addCodes(codeList, errorCode, null, fieldList);
		if (fieldType != null) {
			addCode(codeList, errorCode, null, fieldType.getName());
		}
		addCode(codeList, errorCode, null, null);
		return StringUtils.toStringArray(codeList);
	}

	private void addCodes(Collection<String> codeList, String errorCode, String objectName, Iterable<String> fields) {
		for (String field : fields) {
			addCode(codeList, errorCode, objectName, field);
		}
	}

	private void addCode(Collection<String> codeList, String errorCode, String objectName, String field) {
		codeList.add(postProcessMessageCode(this.formatter.format(errorCode, objectName, field)));
	}

	/**
	 * Add both keyed and non-keyed entries for the supplied {@code field}
	 * to the supplied field list.
	 */
	protected void buildFieldList(String field, List<String> fieldList) {
		fieldList.add(field);
		String plainField = field;
		int keyIndex = plainField.lastIndexOf('[');
		while (keyIndex != -1) {
			int endKeyIndex = plainField.indexOf(']', keyIndex);
			if (endKeyIndex != -1) {
				plainField = plainField.substring(0, keyIndex) + plainField.substring(endKeyIndex + 1);
				fieldList.add(plainField);
				keyIndex = plainField.lastIndexOf('[');
			}
			else {
				keyIndex = -1;
			}
		}
	}

	/**
	 * Post-process the given message code, built by this resolver.
	 * <p>The default implementation applies the specified prefix, if any.
	 * @param code the message code as built by this resolver
	 * @return the final message code to be returned
	 * @see #setPrefix
	 */
	protected String postProcessMessageCode(String code) {
		return getPrefix() + code;
	}


	/**
	 * Common message code formats.
	 * @see MessageCodeFormatter
	 * @see DefaultMessageCodesResolver#setMessageCodeFormatter(MessageCodeFormatter)
	 */
	public enum Format implements MessageCodeFormatter {

		/**
		 * Prefix the error code at the beginning of the generated message code. e.g.:
		 * {@code errorCode + "." + object name + "." + field}
		 */
		PREFIX_ERROR_CODE {
			@Override
			public String format(String errorCode, String objectName, String field) {
				return toDelimitedString(errorCode, objectName, field);
			}
		},

		/**
		 * Postfix the error code at the end of the generated message code. e.g.:
		 * {@code object name + "." + field + "." + errorCode}
		 */
		POSTFIX_ERROR_CODE {
			@Override
			public String format(String errorCode, String objectName, String field) {
				return toDelimitedString(objectName, field, errorCode);
			}
		};

		/**
		 * Concatenate the given elements, delimiting each with
		 * {@link DefaultMessageCodesResolver#CODE_SEPARATOR}, skipping zero-length or
		 * null elements altogether.
		 */
		public static String toDelimitedString(String... elements) {
			StringBuilder rtn = new StringBuilder();
			for (String element : elements) {
				if (StringUtils.hasLength(element)) {
					rtn.append(rtn.length() == 0 ? "" : CODE_SEPARATOR);
					rtn.append(element);
				}
			}
			return rtn.toString();
		}
	}

}
