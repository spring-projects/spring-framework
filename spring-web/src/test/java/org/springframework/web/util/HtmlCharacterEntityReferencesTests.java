/*
 * Copyright 2002-2016 the original author or authors.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Martin Kersten
 * @author Juergen Hoeller
 */
public class HtmlCharacterEntityReferencesTests {

	private static final String DTD_FILE = "HtmlCharacterEntityReferences.dtd";

	@Test
	public void testSupportsAllCharacterEntityReferencesDefinedByHtml() {
		HtmlCharacterEntityReferences entityReferences = new HtmlCharacterEntityReferences();
		Map<Integer, String> referenceCharactersMap = getReferenceCharacterMap();

		for (int character = 0; character < 10000; character++) {
			String referenceName = referenceCharactersMap.get(character);
			if (referenceName != null) {
				String fullReference =
						HtmlCharacterEntityReferences.REFERENCE_START +
						referenceName +
						HtmlCharacterEntityReferences.REFERENCE_END;
				assertTrue("The unicode character " + character + " should be mapped to a reference",
						entityReferences.isMappedToReference((char) character));
				assertEquals("The reference of unicode character " + character + " should be entity " + referenceName,
						fullReference, entityReferences.convertToReference((char) character));
				assertEquals("The entity reference [" + referenceName + "] should be mapped to unicode character " +
						character, (char) character, entityReferences.convertToCharacter(referenceName));
			}
			else if (character == 39) {
				assertTrue(entityReferences.isMappedToReference((char) character));
				assertEquals("&#39;", entityReferences.convertToReference((char) character));
			}
			else {
				assertFalse("The unicode character " + character + " should not be mapped to a reference",
						entityReferences.isMappedToReference((char) character));
				assertNull("No entity reference of unicode character " + character + " should exist",
						entityReferences.convertToReference((char) character));
			}
		}

		assertEquals("The registered entity count of entityReferences should match the number of entity references",
				referenceCharactersMap.size() + 1, entityReferences.getSupportedReferenceCount());
		assertEquals("The HTML 4.0 Standard defines 252+1 entity references so do entityReferences",
				252 + 1, entityReferences.getSupportedReferenceCount());

		assertEquals("Invalid entity reference names should not be convertible",
				(char) -1, entityReferences.convertToCharacter("invalid"));
	}

	// SPR-9293
	@Test
	public void testConvertToReferenceUTF8() {
		HtmlCharacterEntityReferences entityReferences = new HtmlCharacterEntityReferences();
		String utf8 = "UTF-8";
		assertEquals("&lt;", entityReferences.convertToReference('<', utf8));
		assertEquals("&gt;", entityReferences.convertToReference('>', utf8));
		assertEquals("&amp;", entityReferences.convertToReference('&', utf8));
		assertEquals("&quot;", entityReferences.convertToReference('"', utf8));
		assertEquals("&#39;", entityReferences.convertToReference('\'', utf8));
		assertNull(entityReferences.convertToReference((char) 233, utf8));
		assertNull(entityReferences.convertToReference((char) 934, utf8));
	}

	private Map<Integer, String> getReferenceCharacterMap() {
		CharacterEntityResourceIterator entityIterator = new CharacterEntityResourceIterator();
		Map<Integer, String> referencedCharactersMap = new HashMap<>();
		while (entityIterator.hasNext()) {
			int character = entityIterator.getReferredCharacter();
			String entityName = entityIterator.nextEntry();
			referencedCharactersMap.put(new Integer(character), entityName);
		}
		return referencedCharactersMap;
	}


	private static class CharacterEntityResourceIterator {

		private final StreamTokenizer tokenizer;

		private String currentEntityName = null;

		private int referredCharacter = -1;

		public CharacterEntityResourceIterator() {
			try {
				InputStream inputStream = getClass().getResourceAsStream(DTD_FILE);
				if (inputStream == null) {
					throw new IOException("Cannot find definition resource [" + DTD_FILE + "]");
				}
				tokenizer = new StreamTokenizer(new BufferedReader(new InputStreamReader(inputStream, "UTF-8")));
			}
			catch (IOException ex) {
				throw new IllegalStateException("Failed to open definition resource [" + DTD_FILE + "]");
			}
		}

		public boolean hasNext() {
			return (currentEntityName != null || readNextEntity());
		}

		public String nextEntry() {
			if (hasNext()) {
				String entityName = currentEntityName;
				currentEntityName = null;
				return entityName;
			}
			return null;
		}

		public int getReferredCharacter() {
			return referredCharacter;
		}

		private boolean readNextEntity() {
			try {
				while (navigateToNextEntity()) {
					String entityName = nextWordToken();
					if ("CDATA".equals(nextWordToken())) {
						int referredCharacter = nextReferredCharacterId();
						if (entityName != null && referredCharacter != -1) {
							this.currentEntityName = entityName;
							this.referredCharacter = referredCharacter;
							return true;
						}
					}
				}
				return false;
			}
			catch (IOException ex) {
				throw new IllegalStateException("Could not parse defintion resource: " + ex.getMessage());
			}
		}

		private boolean navigateToNextEntity() throws IOException {
			while (tokenizer.nextToken() != StreamTokenizer.TT_WORD || !"ENTITY".equals(tokenizer.sval)) {
				if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
					return false;
				}
			}
			return true;
		}

		private int nextReferredCharacterId() throws IOException {
			String reference = nextWordToken();
			if (reference != null && reference.startsWith("&#") && reference.endsWith(";")) {
				return Integer.parseInt(reference.substring(2, reference.length() - 1));
			}
			return -1;
		}

		private String nextWordToken() throws IOException {
			tokenizer.nextToken();
			return tokenizer.sval;
		}
	}

}
