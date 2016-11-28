/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.web.util;

/**
 * Helper for decoding HTML Strings by replacing character
 * entity references with the referred character.
 *
 * @author Juergen Hoeller
 * @author Martin Kersten
 * @since 1.2.1
 */
class HtmlCharacterEntityDecoder {

	private static final int MAX_REFERENCE_SIZE = 10;


	private final HtmlCharacterEntityReferences characterEntityReferences;

	private final String originalMessage;

	private final StringBuilder decodedMessage;

	private int currentPosition = 0;

	private int nextPotentialReferencePosition = -1;

	private int nextSemicolonPosition = -2;


	public HtmlCharacterEntityDecoder(HtmlCharacterEntityReferences characterEntityReferences, String original) {
		this.characterEntityReferences = characterEntityReferences;
		this.originalMessage = original;
		this.decodedMessage = new StringBuilder(originalMessage.length());
	}

	public String decode() {
		while (currentPosition < originalMessage.length()) {
			findNextPotentialReference(currentPosition);
			copyCharactersTillPotentialReference();
			processPossibleReference();
		}
		return decodedMessage.toString();
	}

	private void findNextPotentialReference(int startPosition) {
		nextPotentialReferencePosition = Math.max(startPosition, nextSemicolonPosition - MAX_REFERENCE_SIZE);

		do {
			nextPotentialReferencePosition =
					originalMessage.indexOf('&', nextPotentialReferencePosition);

			if (nextSemicolonPosition != -1 &&
					nextSemicolonPosition < nextPotentialReferencePosition)
				nextSemicolonPosition = originalMessage.indexOf(';', nextPotentialReferencePosition + 1);

			boolean isPotentialReference =
					nextPotentialReferencePosition != -1
					&& nextSemicolonPosition != -1
					&& nextSemicolonPosition - nextPotentialReferencePosition < MAX_REFERENCE_SIZE;

			if (isPotentialReference) {
				break;
			}
			if (nextPotentialReferencePosition == -1) {
				break;
			}
			if (nextSemicolonPosition == -1) {
				nextPotentialReferencePosition = -1;
				break;
			}

			nextPotentialReferencePosition = nextPotentialReferencePosition + 1;
		}
		while (nextPotentialReferencePosition != -1);
	}


	private void copyCharactersTillPotentialReference() {
		if (nextPotentialReferencePosition != currentPosition) {
			int skipUntilIndex = nextPotentialReferencePosition != -1 ?
					nextPotentialReferencePosition : originalMessage.length();
			if (skipUntilIndex - currentPosition > 3) {
				decodedMessage.append(originalMessage.substring(currentPosition, skipUntilIndex));
				currentPosition = skipUntilIndex;
			}
			else {
				while (currentPosition < skipUntilIndex)
					decodedMessage.append(originalMessage.charAt(currentPosition++));
			}
		}
	}

	private void processPossibleReference() {
		if (nextPotentialReferencePosition != -1) {
			boolean isNumberedReference = originalMessage.charAt(currentPosition + 1) == '#';
			boolean wasProcessable = isNumberedReference ? processNumberedReference() : processNamedReference();
			if (wasProcessable) {
				currentPosition = nextSemicolonPosition + 1;
			}
			else {
				char currentChar = originalMessage.charAt(currentPosition);
				decodedMessage.append(currentChar);
				currentPosition++;
			}
		}
	}

	private boolean processNumberedReference() {
		boolean isHexNumberedReference =
				originalMessage.charAt(nextPotentialReferencePosition + 2) == 'x' ||
				originalMessage.charAt(nextPotentialReferencePosition + 2) == 'X';
		try {
			int value = (!isHexNumberedReference) ?
					Integer.parseInt(getReferenceSubstring(2)) :
					Integer.parseInt(getReferenceSubstring(3), 16);
			decodedMessage.append((char) value);
			return true;
		}
		catch (NumberFormatException ex) {
			return false;
		}
	}

	private boolean processNamedReference() {
		String referenceName = getReferenceSubstring(1);
		char mappedCharacter = characterEntityReferences.convertToCharacter(referenceName);
		if (mappedCharacter != HtmlCharacterEntityReferences.CHAR_NULL) {
			decodedMessage.append(mappedCharacter);
			return true;
		}
		return false;
	}

	private String getReferenceSubstring(int referenceOffset) {
		return originalMessage.substring(nextPotentialReferencePosition + referenceOffset, nextSemicolonPosition);
	}

}
