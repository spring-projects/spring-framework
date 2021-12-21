/*
 * Copyright 2002-2021 the original author or authors.
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
		this.decodedMessage = new StringBuilder(original.length());
	}


	public String decode() {
		while (this.currentPosition < this.originalMessage.length()) {
			findNextPotentialReference(this.currentPosition);
			copyCharactersTillPotentialReference();
			processPossibleReference();
		}
		return this.decodedMessage.toString();
	}

	private void findNextPotentialReference(int startPosition) {
		this.nextPotentialReferencePosition = Math.max(startPosition, this.nextSemicolonPosition - MAX_REFERENCE_SIZE);

		do {
			this.nextPotentialReferencePosition =
					this.originalMessage.indexOf('&', this.nextPotentialReferencePosition);

			if (this.nextSemicolonPosition != -1 &&
					this.nextSemicolonPosition < this.nextPotentialReferencePosition) {
				this.nextSemicolonPosition = this.originalMessage.indexOf(';', this.nextPotentialReferencePosition + 1);
			}

			if (this.nextPotentialReferencePosition == -1) {
				break;
			}
			if (this.nextSemicolonPosition == -1) {
				this.nextPotentialReferencePosition = -1;
				break;
			}
			if (this.nextSemicolonPosition - this.nextPotentialReferencePosition < MAX_REFERENCE_SIZE) {
				break;
			}

			this.nextPotentialReferencePosition = this.nextPotentialReferencePosition + 1;
		}
		while (this.nextPotentialReferencePosition != -1);
	}

	private void copyCharactersTillPotentialReference() {
		if (this.nextPotentialReferencePosition != this.currentPosition) {
			int skipUntilIndex = (this.nextPotentialReferencePosition != -1 ?
					this.nextPotentialReferencePosition : this.originalMessage.length());
			if (skipUntilIndex - this.currentPosition > 3) {
				this.decodedMessage.append(this.originalMessage, this.currentPosition, skipUntilIndex);
				this.currentPosition = skipUntilIndex;
			}
			else {
				while (this.currentPosition < skipUntilIndex) {
					this.decodedMessage.append(this.originalMessage.charAt(this.currentPosition++));
				}
			}
		}
	}

	private void processPossibleReference() {
		if (this.nextPotentialReferencePosition != -1) {
			boolean isNumberedReference = (this.originalMessage.charAt(this.currentPosition + 1) == '#');
			boolean wasProcessable = isNumberedReference ? processNumberedReference() : processNamedReference();
			if (wasProcessable) {
				this.currentPosition = this.nextSemicolonPosition + 1;
			}
			else {
				char currentChar = this.originalMessage.charAt(this.currentPosition);
				this.decodedMessage.append(currentChar);
				this.currentPosition++;
			}
		}
	}

	private boolean processNumberedReference() {
		char referenceChar = this.originalMessage.charAt(this.nextPotentialReferencePosition + 2);
		boolean isHexNumberedReference = (referenceChar == 'x' || referenceChar == 'X');
		try {
			int value = (!isHexNumberedReference ?
					Integer.parseInt(getReferenceSubstring(2)) :
					Integer.parseInt(getReferenceSubstring(3), 16));
			this.decodedMessage.append((char) value);
			return true;
		}
		catch (NumberFormatException ex) {
			return false;
		}
	}

	private boolean processNamedReference() {
		String referenceName = getReferenceSubstring(1);
		char mappedCharacter = this.characterEntityReferences.convertToCharacter(referenceName);
		if (mappedCharacter != HtmlCharacterEntityReferences.CHAR_NULL) {
			this.decodedMessage.append(mappedCharacter);
			return true;
		}
		return false;
	}

	private String getReferenceSubstring(int referenceOffset) {
		return this.originalMessage.substring(
				this.nextPotentialReferencePosition + referenceOffset, this.nextSemicolonPosition);
	}

}
