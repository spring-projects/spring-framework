/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.multipart.support;

import java.io.IOException;

import org.junit.Test;

import org.springframework.web.multipart.MultipartFile;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Rick Evans
 * @author Sam Brannen
 */
public final class ByteArrayMultipartFileEditorTests {

	private final ByteArrayMultipartFileEditor editor = new ByteArrayMultipartFileEditor();

	@Test
	public void setValueAsByteArray() throws Exception {
		String expectedValue = "Shumwere, shumhow, a shuck ish washing you. - Drunken Far Side";
		editor.setValue(expectedValue.getBytes());
		assertEquals(expectedValue, editor.getAsText());
	}

	@Test
	public void setValueAsString() throws Exception {
		String expectedValue = "'Green Wing' - classic British comedy";
		editor.setValue(expectedValue);
		assertEquals(expectedValue, editor.getAsText());
	}

	@Test
	public void setValueAsCustomObjectInvokesToString() throws Exception {
		final String expectedValue = "'Green Wing' - classic British comedy";
		Object object = new Object() {
			@Override
			public String toString() {
				return expectedValue;
			}
		};

		editor.setValue(object);
		assertEquals(expectedValue, editor.getAsText());
	}

	@Test
	public void setValueAsNullGetsBackEmptyString() throws Exception {
		editor.setValue(null);
		assertEquals("", editor.getAsText());
	}

	@Test
	public void setValueAsMultipartFile() throws Exception {
		String expectedValue = "That is comforting to know";
		MultipartFile file = mock(MultipartFile.class);
		given(file.getBytes()).willReturn(expectedValue.getBytes());
		editor.setValue(file);
		assertEquals(expectedValue, editor.getAsText());
	}

	@Test(expected = IllegalArgumentException.class)
	public void setValueAsMultipartFileWithBadBytes() throws Exception {
		MultipartFile file = mock(MultipartFile.class);
		given(file.getBytes()).willThrow(new IOException());
		editor.setValue(file);
	}

}
