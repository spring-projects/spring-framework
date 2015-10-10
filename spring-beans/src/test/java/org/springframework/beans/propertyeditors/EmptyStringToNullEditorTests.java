/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.beans.propertyeditors;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Test cases for {@link EmptyStringToNullEditor}.
 *
 * @author Kazuki Shimizu
 * @since 4.2.2
 */
public class EmptyStringToNullEditorTests {

	private final EmptyStringToNullEditor editor = new EmptyStringToNullEditor();

	@Test
	public void textIsNull() {

		editor.setAsText(null);

		assertThat(editor.getValue(), is(nullValue()));
		assertThat(editor.getAsText(), is(""));

	}

	@Test
	public void textIsEmpty() {

		editor.setAsText("");

		assertThat(editor.getValue(), is(nullValue()));
		assertThat(editor.getAsText(), is(""));

	}

	@Test
	public void textIsAnyString() {

		editor.setAsText(" Spring ");

		assertThat(editor.getValue(), is(" Spring "));
		assertThat(editor.getAsText(), is(" Spring "));

	}


}
