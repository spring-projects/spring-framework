/*
 * Copyright 2004-2008 the original author or authors.
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
 * <pre><code>
 * EL1004E: (pos 34): Type cannot be found 'String'
 * </pre></code> The prefix captures the code and the error kind, whilst the position is included if it is known and the
 * message has had all relevant inserts applied to it.
 * 
 * @author Andy Clement
 * 
 */
public enum SpelMessages {
	// TODO put keys and messages into bundles for easy NLS
	// TODO damn code formatter keeps messing up the layout

	INITIALIZER_LENGTH_INCORRECT(Kind.ERROR, 1001,
			"Array constructor call: initializer size of {0} does not match declared length of {1}"), TYPE_CONVERSION_ERROR(
			Kind.ERROR, 1002, "Type conversion problem, cannot convert from {0} to {1}"), CONSTRUCTOR_NOT_FOUND(
			Kind.ERROR, 1003, "Constructor call: No suitable constructor on type {0} for arguments {1}"), TYPE_NOT_FOUND(
			Kind.ERROR, 1004, "Type cannot be found ''{0}''"), ADDITION_NOT_DEFINED(Kind.ERROR, 1005,
			"Addition not defined between operands of type {0} and {1}"), METHOD_NOT_FOUND(Kind.ERROR, 1006,
			"Method call: Method {0} cannot be found on {1} type"), ATTEMPTED_METHOD_CALL_ON_NULL_CONTEXT_OBJECT(
			Kind.ERROR, 1007, "Method call: Attempted to call method {0} on null context object"), ATTEMPTED_PROPERTY_FIELD_REF_ON_NULL_CONTEXT_OBJECT(
			Kind.ERROR, 1008,
			"Field or property reference: Attempted to refer to field or property ''{0}'' on null context object"), PROPERTY_OR_FIELD_NOT_FOUND(
			Kind.ERROR, 1009, "Field or property ''{0}'' cannot be found on object of type ''{1}''"), PROPERTY_OR_FIELD_SETTER_NOT_FOUND(
			Kind.ERROR, 1010, "Field or property ''{0}'' cannot be set on object of type ''{1}''"), MULTIPLY_NOT_DEFINED(
			Kind.ERROR, 1011, "Multiply not defined between operands of type {0} and {1}"), NOT_COMPARABLE(Kind.ERROR,
			1012, "Cannot compare instances of {0} and {1}"), NOT_COMPARABLE_CANNOT_COERCE(Kind.ERROR, 1013,
			"Cannot compare instances of {0} and {1} because they cannot be coerced to the same type"), VARIABLE_NOT_FOUND(
			Kind.ERROR, 1014, "Variable named ''{0}'' cannot be found"), INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION(
			Kind.ERROR, 1015, "Incorrect number of arguments for function, {0} supplied but function takes {1}"), NO_SUCH_FUNCTION(
			Kind.ERROR, 1016, "No such function named ''{0}''"), NOT_A_FUNCTION(Kind.ERROR, 1017,
			"The name ''{0}'' did not map to a function, it mapped to a ''{1}''"), INVALID_TYPE_FOR_SELECTION(
			Kind.ERROR, 1018, "Cannot perform selection on input data of type ''{0}''"), RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN(
			Kind.ERROR, 1019, "Result of selection criteria is not boolean"), MODULUS_NOT_DEFINED(Kind.ERROR, 1020,
			"Modulus not defined between operands of type ''{0}'' and ''{1}''"), NULL_OPERAND_TO_OPERATOR(Kind.ERROR,
			1021, "Operand evaluated to null and that is not supported for this operator"), NO_SIZE_OR_INITIALIZER_FOR_ARRAY_CONSTRUCTION(
			Kind.ERROR, 1022, "No array size or initializer was supplied to construct the array"), INCORRECT_ELEMENT_TYPE_FOR_ARRAY(
			Kind.ERROR, 1023, "The array of type ''{0}'' cannot have an element of type ''{1}'' inserted"), BETWEEN_RIGHT_OPERAND_MUST_BE_TWO_ELEMENT_LIST(
			Kind.ERROR, 1024, "Right operand for the 'between' operator has to be a two-element list"), TYPE_NOT_SUPPORTED_BY_PROCESSOR(
			Kind.ERROR, 1025,
			"The collection processor ''{0}'' does not understand and input collection of elements of type {1}"), UNABLE_TO_ACCESS_FIELD(
			Kind.ERROR, 1026, "Unable to access field ''{0}'' on type ''{1}''"), UNABLE_TO_ACCESS_PROPERTY_THROUGH_GETTER(
			Kind.ERROR, 1027, "Unable to access property ''{0}'' through getter on type ''{1}''"), UNABLE_TO_ACCESS_PROPERTY_THROUGH_SETTER(
			Kind.ERROR, 1028, "Unable to access property ''{0}'' through setter on type ''{1}''"), INVALID_PATTERN(
			Kind.ERROR, 1029, "Pattern is not valid ''{0}''"), RECOGNITION_ERROR(Kind.ERROR, 1030,
			"Recognition error: {0}"), // TODO poor message when a recognition exception occurs
	PROJECTION_NOT_SUPPORTED_ON_TYPE(Kind.ERROR, 1031, "Projection is not supported on the type ''{0}''"), ARGLIST_SHOULD_NOT_BE_EVALUATED(
			Kind.ERROR, 1032, "The argument list of a lambda expression should never have getValue() called upon it"), MAPENTRY_SHOULD_NOT_BE_EVALUATED(
			Kind.ERROR, 1033, "A map entry should never have getValue() called upon it"), EXCEPTION_DURING_PROPERTY_READ(
			Kind.ERROR, 1034, "A problem occurred whilst attempting to access the property ''{0}'': ''{1}''"), EXCEPTION_DURING_CONSTRUCTOR_INVOCATION(
			Kind.ERROR, 1035, "A problem occurred whilst attempting to construct ''{0}'': ''{1}''"), DATE_CANNOT_BE_PARSED(
			Kind.ERROR, 1036, "Unable to parse date ''{0}'' using format ''{1}''"), FUNCTION_REFERENCE_CANNOT_BE_INVOKED(
			Kind.ERROR, 1037, "The function ''{0}'' mapped to an object of type ''{1}'' which cannot be invoked"), FUNCTION_NOT_DEFINED(
			Kind.ERROR, 1038, "The function ''{0}'' could not be found"), EXCEPTION_DURING_FUNCTION_CALL(Kind.ERROR,
			1039, "A problem occurred whilst attempting to invoke the function ''{0}'': ''{1}''"), ARRAY_INDEX_OUT_OF_BOUNDS(
			Kind.ERROR, 1040, "The array has ''{0}'' elements, index ''{1}'' is invalid"), COLLECTION_INDEX_OUT_OF_BOUNDS(
			Kind.ERROR, 1041, "The collection has ''{0}'' elements, index ''{1}'' is invalid"), STRING_INDEX_OUT_OF_BOUNDS(
			Kind.ERROR, 1042, "The string has ''{0}'' characters, index ''{1}'' is invalid"), INDEXING_NOT_SUPPORTED_FOR_TYPE(
			Kind.ERROR, 1043, "Indexing into type ''{0}'' is not supported"), OPERATOR_IN_CANNOT_DETERMINE_MEMBERSHIP(
			Kind.ERROR, 1044, "Operator 'in' not implemented for detecting membership of a ''{0}'' in a ''{1}''"), CANNOT_NEGATE_TYPE(
			Kind.ERROR, 1045, "Cannot determine negation of type ''{0}''"), CUT_ARGUMENTS_MUST_BE_INTS(Kind.ERROR,
			1046, "Both arguments to the cut() processor must be Integers, but they are ''{0}'' and ''{1}''"), SOUNDSLIKE_NEEDS_STRING_OPERAND(
			Kind.ERROR, 1047, "The soundslike operator needs String operands, but found a ''{0}''"), IS_OPERATOR_NEEDS_CLASS_OPERAND(
			Kind.ERROR, 1048, "The operator 'is' needs the right operand to be a class, not a ''{0}''"), LOCAL_VARIABLE_NOT_DEFINED(
			Kind.ERROR, 1049, "Local variable named ''{0}'' could not be found"), EXCEPTION_DURING_METHOD_INVOCATION(
			Kind.ERROR, 1050,
			"A problem occurred when trying to execute method ''{0}'' on object of type ''{1}'': ''{2}''"), PLACEHOLDER_SHOULD_NEVER_BE_EVALUATED(
			Kind.ERROR, 1051, "InternalError: A placeholder node in the Ast should never be evaluated!"), OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES(
			Kind.ERROR, 1052, "The operator ''{0}'' is not supported between objects of type ''{1}'' and ''{2}''"), UNEXPECTED_PROBLEM_INVOKING_OPERATOR(
			Kind.ERROR, 1054,
			"Unexpected problem invoking operator ''{0}'' between objects of type ''{1}'' and ''{2}'': {3}"), PROBLEM_LOCATING_METHOD(
			Kind.ERROR, 1055, "Problem locating method {0} cannot on type {1}"), PROBLEM_LOCATING_CONSTRUCTOR(
			Kind.ERROR, 1056,
			"A problem occurred whilst attempting to construct an object of type ''{0}'' using arguments ''{1}''"), INVALID_FIRST_OPERAND_FOR_LIKE_OPERATOR(
			Kind.ERROR, 1057, "First operand to like operator must be a string.  ''{0}'' is not"), INVALID_SECOND_OPERAND_FOR_LIKE_OPERATOR(
			Kind.ERROR, 1058, "Second operand to like operator must be a string (regex). ''{0}'' is not"), SETVALUE_NOT_SUPPORTED(
			Kind.ERROR, 1059, "setValue(ExpressionState, Object) not implemented for ''{0}''  (''{1}''"), TYPE_NAME_EXPECTED_FOR_ARRAY_CONSTRUCTION(
			Kind.ERROR, 1060, "Expected the type of the new array to be specified as a String but found ''{0}''"), PROBLEM_DURING_TYPE_CONVERSION(
			Kind.ERROR, 1061, "Problem occurred during type conversion: {0}"), MULTIPLE_POSSIBLE_METHODS(Kind.ERROR,
			1062, "Method call of ''{0}'' is ambiguous, supported type conversions allow multiple variants to match"), EXCEPTION_DURING_PROPERTY_WRITE(
			Kind.ERROR, 1063, "A problem occurred whilst attempting to set the property ''{0}'': ''{1}''"), NOT_AN_INTEGER(
			Kind.ERROR, 1064, "The value ''{0}'' cannot be parsed as an int"), NOT_A_LONG(Kind.ERROR, 1065,
			"The value ''{0}'' cannot be parsed as a long"), PARSE_PROBLEM(Kind.ERROR, 1066,
			"Error occurred during expression parse: {0}"), INVALID_FIRST_OPERAND_FOR_MATCHES_OPERATOR(Kind.ERROR,
			1067, "First operand to matches operator must be a string.  ''{0}'' is not"), INVALID_SECOND_OPERAND_FOR_MATCHES_OPERATOR(
			Kind.ERROR, 1068, "Second operand to matches operator must be a string. ''{0}'' is not");

	private Kind kind;
	private int code;
	private String message;

	public static enum Kind {
		INFO, WARNING, ERROR
	};

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
}
