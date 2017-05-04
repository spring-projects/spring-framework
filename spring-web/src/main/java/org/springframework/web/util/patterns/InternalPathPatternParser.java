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

package org.springframework.web.util.patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * Parser for URI template patterns. It breaks the path pattern into a number of
 * {@link PathElement}s in a linked list. Instances are reusable but are not thread-safe.
 *
 * @author Andy Clement
 * @since 5.0
 */
public class InternalPathPatternParser {

	// The expected path separator to split path elements during parsing
	char separator = PathPatternParser.DEFAULT_SEPARATOR;

	// Is the parser producing case sensitive PathPattern matchers
	boolean caseSensitive = true;

	// If true the PathPatterns produced by the parser will allow patterns
	// that don't have a trailing slash to match paths that may or may not
	// have a trailing slash
	private boolean matchOptionalTrailingSlash = false;
	
	// The input data for parsing
	private char[] pathPatternData;

	// The length of the input data
	private int pathPatternLength;

	// Current parsing position
	int pos;

	// How many ? characters in a particular path element
	private int singleCharWildcardCount;

	// Is the path pattern using * characters in a particular path element
	private boolean wildcard = false;

	// Is the construct {*...} being used in a particular path element
	private boolean isCaptureTheRestVariable = false;

	// Has the parser entered a {...} variable capture block in a particular
	// path element
	private boolean insideVariableCapture = false;

	// How many variable captures are occurring in a particular path element
	private int variableCaptureCount = 0;

	// Start of the most recent path element in a particular path element
	int pathElementStart;

	// Start of the most recent variable capture in a particular path element
	int variableCaptureStart;

	// Variables captures in this path pattern
	List<String> capturedVariableNames;

	// The head of the path element chain currently being built
	PathElement headPE;

	// The most recently constructed path element in the chain
	PathElement currentPE;

	/**
	 * Create a PatternParser that will use the specified separator instead of
	 * the default.
	 *
	 * @param separator the path separator to look for when parsing
	 * @param caseSensitive true if PathPatterns should be sensitive to case
	 * @param matchOptionalTrailingSlash true if patterns without a trailing slash can match paths that do have a trailing slash
	 */
	public InternalPathPatternParser(char separator, boolean caseSensitive, boolean matchOptionalTrailingSlash) {
		this.separator = separator;
		this.caseSensitive = caseSensitive;
		this.matchOptionalTrailingSlash = matchOptionalTrailingSlash;
	}

	/**
	 * Process the path pattern data, a character at a time, breaking it into
	 * path elements around separator boundaries and verifying the structure at each
	 * stage. Produces a PathPattern object that can be used for fast matching
	 * against paths.
	 *
	 * @param pathPattern the input path pattern, e.g. /foo/{bar}
	 * @return a PathPattern for quickly matching paths against the specified path pattern
	 */
	public PathPattern parse(String pathPattern) {
		if (pathPattern == null) {
			pathPattern = "";
		}
		pathPatternData = pathPattern.toCharArray();
		pathPatternLength = pathPatternData.length;
		headPE = null;
		currentPE = null;
		capturedVariableNames = null;
		pathElementStart = -1;
		pos = 0;
		resetPathElementState();
		while (pos < pathPatternLength) {
			char ch = pathPatternData[pos];
			if (ch == separator) {
				if (pathElementStart != -1) {
					pushPathElement(createPathElement());
				}
				if (peekDoubleWildcard()) {
					pushPathElement(new WildcardTheRestPathElement(pos, separator));
					pos += 2;
				}
				else {
					pushPathElement(new SeparatorPathElement(pos, separator));
				}
			}
			else {
				if (pathElementStart == -1) {
					pathElementStart = pos;
				}
				if (ch == '?') {
					singleCharWildcardCount++;
				}
				else if (ch == '{') {
					if (insideVariableCapture) {
						throw new PatternParseException(pos, pathPatternData, PatternMessage.ILLEGAL_NESTED_CAPTURE);
						// If we enforced that adjacent captures weren't allowed,
						// // this would do it (this would be an error: /foo/{bar}{boo}/)
//					} else if (pos > 0 && pathPatternData[pos - 1] == '}') {
//						throw new PatternParseException(pos, pathPatternData,
//								PatternMessage.CANNOT_HAVE_ADJACENT_CAPTURES);
					}
					insideVariableCapture = true;
					variableCaptureStart = pos;
				}
				else if (ch == '}') {
					if (!insideVariableCapture) {
						throw new PatternParseException(pos, pathPatternData, PatternMessage.MISSING_OPEN_CAPTURE);
					}
					insideVariableCapture = false;
					if (isCaptureTheRestVariable && (pos + 1) < pathPatternLength) {
						throw new PatternParseException(pos + 1, pathPatternData,
								PatternMessage.NO_MORE_DATA_EXPECTED_AFTER_CAPTURE_THE_REST);
					}
					variableCaptureCount++;
				}
				else if (ch == ':') {
					if (insideVariableCapture) {
						skipCaptureRegex();
						insideVariableCapture = false;
						variableCaptureCount++;
					}
				}
				else if (ch == '*') {
					if (insideVariableCapture) {
						if (variableCaptureStart == pos - 1) {
							isCaptureTheRestVariable = true;
						}
					}
					wildcard = true;
				}
				// Check that the characters used for captured variable names are like java identifiers
				if (insideVariableCapture) {
					if ((variableCaptureStart + 1 + (isCaptureTheRestVariable ? 1 : 0)) == pos
							&& !Character.isJavaIdentifierStart(ch)) {
						throw new PatternParseException(pos, pathPatternData,
								PatternMessage.ILLEGAL_CHARACTER_AT_START_OF_CAPTURE_DESCRIPTOR,
								Character.toString(ch));

					}
					else if ((pos > (variableCaptureStart + 1 + (isCaptureTheRestVariable ? 1 : 0))
							&& !Character.isJavaIdentifierPart(ch))) {
						throw new PatternParseException(pos, pathPatternData,
								PatternMessage.ILLEGAL_CHARACTER_IN_CAPTURE_DESCRIPTOR, Character.toString(ch));
					}
				}
			}
			pos++;
		}
		if (pathElementStart != -1) {
			pushPathElement(createPathElement());
		}
		return new PathPattern(pathPattern, headPE, separator, caseSensitive, matchOptionalTrailingSlash);
	}

	/**
	 * Just hit a ':' and want to jump over the regex specification for this
	 * variable. pos will be pointing at the ':', we want to skip until the }.
	 * <p>
	 * Nested {...} pairs don't have to be escaped: <tt>/abc/{var:x{1,2}}/def</tt>
	 * <p>An escaped } will not be treated as the end of the regex: <tt>/abc/{var:x\\{y:}/def</tt>
	 * <p>A separator that should not indicate the end of the regex can be escaped:
	 */
	private void skipCaptureRegex() {
		pos++;
		int regexStart = pos;
		int curlyBracketDepth = 0; // how deep in nested {...} pairs
		boolean previousBackslash = false;
		while (pos < pathPatternLength) {
			char ch = pathPatternData[pos];
			if (ch == '\\' && !previousBackslash) {
				pos++;
				previousBackslash = true;
				continue;
			}
			if (ch == '{' && !previousBackslash) {
				curlyBracketDepth++;
			}
			else if (ch == '}' && !previousBackslash) {
				if (curlyBracketDepth == 0) {
					if (regexStart == pos) {
						throw new PatternParseException(regexStart, pathPatternData,
								PatternMessage.MISSING_REGEX_CONSTRAINT);
					}
					return;
				}
				curlyBracketDepth--;
			}
			if (ch == separator && !previousBackslash) {
				throw new PatternParseException(pos, pathPatternData, PatternMessage.MISSING_CLOSE_CAPTURE);
			}
			pos++;
			previousBackslash = false;
		}
		throw new PatternParseException(pos - 1, pathPatternData, PatternMessage.MISSING_CLOSE_CAPTURE);
	}

	/**
	 * After processing a separator, a quick peek whether it is followed by **
	 * (and only ** before the end of the pattern or the next separator)
	 */
	private boolean peekDoubleWildcard() {
		if ((pos + 2) >= pathPatternLength) {
			return false;
		}
		if (pathPatternData[pos + 1] != '*' || pathPatternData[pos + 2] != '*') {
			return false;
		}
		return (pos + 3 == pathPatternLength);
	}

	/**
	 * @param newPathElement the new path element to add to the chain being built
	 */
	private void pushPathElement(PathElement newPathElement) {
		if (newPathElement instanceof CaptureTheRestPathElement) {
			// There must be a separator ahead of this thing
			// currentPE SHOULD be a SeparatorPathElement
			if (currentPE == null) {
				headPE = newPathElement;
				currentPE = newPathElement;
			}
			else if (currentPE instanceof SeparatorPathElement) {
				PathElement peBeforeSeparator = currentPE.prev;
				if (peBeforeSeparator == null) {
					// /{*foobar} is at the start
					headPE = newPathElement;
					newPathElement.prev = peBeforeSeparator;
				}
				else {
					peBeforeSeparator.next = newPathElement;
					newPathElement.prev = peBeforeSeparator;
				}
				currentPE = newPathElement;
			}
			else {
				throw new IllegalStateException("Expected SeparatorPathElement but was " + currentPE);
			}
		}
		else {
			if (headPE == null) {
				headPE = newPathElement;
				currentPE = newPathElement;
			}
			else {
				currentPE.next = newPathElement;
				newPathElement.prev = currentPE;
				currentPE = newPathElement;
			}
		}
		resetPathElementState();
	}

	/**
	 * Used the knowledge built up whilst processing since the last path element to determine what kind of path
	 * element to create.
	 * @return the new path element
	 */
	private PathElement createPathElement() {
		if (insideVariableCapture) {
			throw new PatternParseException(pos, pathPatternData, PatternMessage.MISSING_CLOSE_CAPTURE);
		}
		char[] pathElementText = new char[pos - pathElementStart];
		System.arraycopy(pathPatternData, pathElementStart, pathElementText, 0, pos - pathElementStart);
		PathElement newPE = null;
		if (variableCaptureCount > 0) {
			if (variableCaptureCount == 1
					&& pathElementStart == variableCaptureStart && pathPatternData[pos - 1] == '}') {
				if (isCaptureTheRestVariable) {
					// It is {*....}
					newPE = new CaptureTheRestPathElement(pathElementStart, pathElementText, separator);
				}
				else {
					// It is a full capture of this element (possibly with constraint), for example: /foo/{abc}/
					try {
						newPE = new CaptureVariablePathElement(pathElementStart, pathElementText, caseSensitive, separator);
					}
					catch (PatternSyntaxException pse) {
						throw new PatternParseException(pse, findRegexStart(pathPatternData, pathElementStart)
								+ pse.getIndex(), pathPatternData, PatternMessage.JDK_PATTERN_SYNTAX_EXCEPTION);
					}
					recordCapturedVariable(pathElementStart, ((CaptureVariablePathElement) newPE).getVariableName());
				}
			}
			else {
				if (isCaptureTheRestVariable) {
					throw new PatternParseException(pathElementStart, pathPatternData,
							PatternMessage.CAPTURE_ALL_IS_STANDALONE_CONSTRUCT);
				}
				RegexPathElement newRegexSection = new RegexPathElement(pathElementStart, pathElementText,
						caseSensitive, pathPatternData, separator);
				for (String variableName : newRegexSection.getVariableNames()) {
					recordCapturedVariable(pathElementStart, variableName);
				}
				newPE = newRegexSection;
			}
		}
		else {
			if (wildcard) {
				if (pos - 1 == pathElementStart) {
					newPE = new WildcardPathElement(pathElementStart, separator);
				}
				else {
					newPE = new RegexPathElement(pathElementStart, pathElementText, caseSensitive, pathPatternData, separator);
				}
			}
			else if (singleCharWildcardCount != 0) {
				newPE = new SingleCharWildcardedPathElement(pathElementStart, pathElementText,
						singleCharWildcardCount, caseSensitive, separator);
			}
			else {
				newPE = new LiteralPathElement(pathElementStart, pathElementText, caseSensitive, separator);
			}
		}
		return newPE;
	}

	/**
	 * For a path element representing a captured variable, locate the constraint pattern.
	 * Assumes there is a constraint pattern.
	 * @param data a complete path expression, e.g. /aaa/bbb/{ccc:...}
	 * @param offset the start of the capture pattern of interest 
	 * @return the index of the character after the ':' within
	 * the pattern expression relative to the start of the whole expression
	 */
	private int findRegexStart(char[] data, int offset) {
		int pos = offset;
		while (pos < data.length) {
			if (data[pos] == ':') {
				return pos + 1;
			}
			pos++;
		}
		return -1;
	}

	/**
	 * Reset all the flags and position markers computed during path element processing.
	 */
	private void resetPathElementState() {
		pathElementStart = -1;
		singleCharWildcardCount = 0;
		insideVariableCapture = false;
		variableCaptureCount = 0;
		wildcard = false;
		isCaptureTheRestVariable = false;
		variableCaptureStart = -1;
	}

	/**
	 * Record a new captured variable. If it clashes with an existing one then report an error.
	 */
	private void recordCapturedVariable(int pos, String variableName) {
		if (capturedVariableNames == null) {
			capturedVariableNames = new ArrayList<>();
		}
		if (capturedVariableNames.contains(variableName)) {
			throw new PatternParseException(pos, this.pathPatternData,
					PatternMessage.ILLEGAL_DOUBLE_CAPTURE, variableName);
		}
		capturedVariableNames.add(variableName);
	}
}