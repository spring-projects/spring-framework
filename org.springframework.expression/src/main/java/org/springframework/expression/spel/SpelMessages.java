/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.expression.spel;

import java.text.MessageFormat;

/**
 * Contains all the messages that can be produced by the Spring Expression Language. Each message has a kind (info,
 * warn, error) and a code number. Tests can be written to expect particular code numbers rather than particular text,
 * enabling the message text to more easily be modified and the tests to run successfully in different locales.
 * <p>
 * When a message is formatted, it will have this kind of form
 * 
 * <pre>
 * &lt;code&gt;
 * EL1004E: (pos 34): Type cannot be found 'String'
 * </pre>
 * 
 * </code> The prefix captures the code and the error kind, whilst the position is included if it is known.
 * 
 * @author Andy Clement
 * @since 3.0
 */
public enum SpelMessages {
	// TODO put keys and messages into bundles for easy NLS
	// TODO review if any messages are not used
	// TODO sort messages into better groups if possible, sharing a name prefix perhaps

	TYPE_CONVERSION_ERROR(Kind.ERROR, 1001, "Type conversion problem, cannot convert from {0} to {1}"), //
	CONSTRUCTOR_NOT_FOUND(Kind.ERROR, 1002, "Constructor call: No suitable constructor found on type {0} for arguments {1}"), //
	METHOD_NOT_FOUND(Kind.ERROR, 1003, "Method call: Method {0} cannot be found on {1} type"), // 
	TYPE_NOT_FOUND(Kind.ERROR, 1004, "Type cannot be found ''{0}''"), //
	VARIABLE_NOT_FOUND(Kind.ERROR, 1005, "Variable named ''{0}'' cannot be found"), //
	LOCAL_VARIABLE_NOT_DEFINED(Kind.ERROR, 1006, "Local variable named ''{0}'' could not be found"), //
	FUNCTION_NOT_DEFINED(Kind.ERROR, 1007, "The function ''{0}'' could not be found"), //
	PROPERTY_OR_FIELD_NOT_READABLE_ON_NULL(Kind.ERROR, 1008, "Field or property ''{0}'' cannot be found on null"), //
	PROPERTY_OR_FIELD_NOT_READABLE(Kind.ERROR, 1009, "Field or property ''{0}'' cannot be found on object of type ''{1}''"), //
	PROPERTY_OR_FIELD_NOT_WRITABLE_ON_NULL(Kind.ERROR, 1010, "Field or property ''{0}'' cannot be set on null"), //
	PROPERTY_OR_FIELD_NOT_WRITABLE(Kind.ERROR, 1010, "Field or property ''{0}'' cannot be set on object of type ''{1}''"), //
	
	METHOD_CALL_ON_NULL_OBJECT_NOT_ALLOWED(Kind.ERROR, 1011, "Method call: Attempted to call method {0} on null context object"), //
	PROPERTY_OR_FIELD_ACCESS_ON_NULL_OBJECT_NOT_ALLOWED(Kind.ERROR, 1012, "Field or property reference: Attempted to refer to field or property ''{0}'' on null context object"), //
	CANNOT_INDEX_INTO_NULL_VALUE(Kind.ERROR, 1013, "Cannot index into a null value"),
	
	NOT_COMPARABLE(Kind.ERROR, 1014, "Cannot compare instances of {0} and {1}"), //
	NOT_COMPARABLE_CANNOT_COERCE(Kind.ERROR, 1015, "Cannot compare instances of {0} and {1} because they cannot be coerced to the same type"), //
	INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION(Kind.ERROR, 1016, "Incorrect number of arguments for function, {0} supplied but function takes {1}"), //
	INVALID_TYPE_FOR_SELECTION(Kind.ERROR, 1017, "Cannot perform selection on input data of type ''{0}''"), //
	RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN(Kind.ERROR, 1018, "Result of selection criteria is not boolean"), //
	NULL_OPERAND_TO_OPERATOR(Kind.ERROR, 1019, "Operand evaluated to null and that is not supported for this operator"), //
	BETWEEN_RIGHT_OPERAND_MUST_BE_TWO_ELEMENT_LIST(Kind.ERROR, 1020, "Right operand for the 'between' operator has to be a two-element list"), //
	UNABLE_TO_ACCESS_FIELD(Kind.ERROR, 1021, "Unable to access field ''{0}'' on type ''{1}''"), //
	UNABLE_TO_ACCESS_PROPERTY_THROUGH_GETTER(Kind.ERROR, 1022, "Unable to access property ''{0}'' through getter on type ''{1}''"), //
	UNABLE_TO_ACCESS_PROPERTY_THROUGH_SETTER(Kind.ERROR, 1023, "Unable to access property ''{0}'' through setter on type ''{1}''"), //
	INVALID_PATTERN(Kind.ERROR, 1024, "Pattern is not valid ''{0}''"), //
	RECOGNITION_ERROR(Kind.ERROR, 1025, "Recognition error: {0}"), // TODO poor message when a recognition exception occurs
	PROJECTION_NOT_SUPPORTED_ON_TYPE(Kind.ERROR, 1026, "Projection is not supported on the type ''{0}''"), //
	ARGLIST_SHOULD_NOT_BE_EVALUATED(Kind.ERROR, 1027, "The argument list of a lambda expression should never have getValue() called upon it"), //
	MAPENTRY_SHOULD_NOT_BE_EVALUATED(Kind.ERROR, 1028, "A map entry should never have getValue() called upon it"), //
	EXCEPTION_DURING_PROPERTY_READ(Kind.ERROR, 1029, "A problem occurred whilst attempting to access the property ''{0}'': ''{1}''"), //
	EXCEPTION_DURING_CONSTRUCTOR_INVOCATION(Kind.ERROR, 1030, "A problem occurred whilst attempting to construct ''{0}'': ''{1}''"), //
	DATE_CANNOT_BE_PARSED(Kind.ERROR, 1031, "Unable to parse date ''{0}'' using format ''{1}''"), //
	FUNCTION_REFERENCE_CANNOT_BE_INVOKED(Kind.ERROR, 1032, "The function ''{0}'' mapped to an object of type ''{1}'' which cannot be invoked"), //
	EXCEPTION_DURING_FUNCTION_CALL(Kind.ERROR, 1033, "A problem occurred whilst attempting to invoke the function ''{0}'': ''{1}''"), //

	// indexing
	ARRAY_INDEX_OUT_OF_BOUNDS(Kind.ERROR, 1034, "The array has ''{0}'' elements, index ''{1}'' is invalid"), //
	COLLECTION_INDEX_OUT_OF_BOUNDS(Kind.ERROR, 1035, "The collection has ''{0}'' elements, index ''{1}'' is invalid"), // 
	STRING_INDEX_OUT_OF_BOUNDS(Kind.ERROR, 1036, "The string has ''{0}'' characters, index ''{1}'' is invalid"), //
	INDEXING_NOT_SUPPORTED_FOR_TYPE(Kind.ERROR, 1037, "Indexing into type ''{0}'' is not supported"), //

	INSTANCEOF_OPERATOR_NEEDS_CLASS_OPERAND(Kind.ERROR, 1038, "The operator 'instanceof' needs the right operand to be a class, not a ''{0}''"), //
	EXCEPTION_DURING_METHOD_INVOCATION(Kind.ERROR, 1039, "A problem occurred when trying to execute method ''{0}'' on object of type ''{1}'': ''{2}''"), // 
	OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES(Kind.ERROR, 1040, "The operator ''{0}'' is not supported between objects of type ''{1}'' and ''{2}''"), //
	UNEXPECTED_PROBLEM_INVOKING_OPERATOR(Kind.ERROR, 1041, "Unexpected problem invoking operator ''{0}'' between objects of type ''{1}'' and ''{2}'': {3}"), //
	PROBLEM_LOCATING_METHOD(Kind.ERROR, 1042, "Problem locating method {0} cannot on type {1}"), 
	PROBLEM_LOCATING_CONSTRUCTOR(Kind.ERROR, 1043, "A problem occurred whilst attempting to construct an object of type ''{0}'' using arguments ''{1}''"), //
	SETVALUE_NOT_SUPPORTED(	Kind.ERROR, 1044, "setValue(ExpressionState, Object) not implemented for ''{0}''  (''{1}''"), //
	PROBLEM_DURING_TYPE_CONVERSION(Kind.ERROR, 1045, "Problem occurred during type conversion: {0}"), //
	MULTIPLE_POSSIBLE_METHODS(Kind.ERROR, 1046, "Method call of ''{0}'' is ambiguous, supported type conversions allow multiple variants to match"), //
	EXCEPTION_DURING_PROPERTY_WRITE(Kind.ERROR, 1047, "A problem occurred whilst attempting to set the property ''{0}'': {1}"), //
	NOT_AN_INTEGER(Kind.ERROR, 1048, "The value ''{0}'' cannot be parsed as an int"), //
	NOT_A_LONG(Kind.ERROR, 1049, "The value ''{0}'' cannot be parsed as a long"), // 
	PARSE_PROBLEM(Kind.ERROR, 1050, "Error occurred during expression parse: {0}"), //
	INVALID_FIRST_OPERAND_FOR_MATCHES_OPERATOR(Kind.ERROR, 1051, "First operand to matches operator must be a string.  ''{0}'' is not"), //
	INVALID_SECOND_OPERAND_FOR_MATCHES_OPERATOR(Kind.ERROR, 1052, "Second operand to matches operator must be a string. ''{0}'' is not"), //
	FUNCTION_MUST_BE_STATIC(Kind.ERROR, 1053, "Only static methods can be called via function references. The method ''{0}'' referred to by name ''{1}'' is not static."),//
	;

	private Kind kind;
	private int code;
	private String message;


	private SpelMessages(Kind kind, int code, String message) {
		this.kind = kind;
		this.code = code;
		this.message = message;
	}


	/**
	 * Produce a complete message including the prefix, the position (if known) and with the inserts applied to the
	 * message.
	 * 
	 * @param pos the position, if less than zero it is ignored and not included in the message
	 * @param inserts the inserts to put into the formatted message
	 * @return a formatted message
	 */
	public String formatMessage(int pos, Object... inserts) {
		StringBuilder formattedMessage = new StringBuilder();
		formattedMessage.append("EL").append(code);
		switch (kind) {
		case WARNING:
			formattedMessage.append("W");
			break;
		case INFO:
			formattedMessage.append("I");
			break;
		case ERROR:
			formattedMessage.append("E");
			break;
		}
		formattedMessage.append(":");
		if (pos != -1) {
			formattedMessage.append("(pos ").append(pos).append("): ");
		}
		formattedMessage.append(MessageFormat.format(message, inserts));
		return formattedMessage.toString();
	}


	public static enum Kind {
		INFO, WARNING, ERROR
	}

}
