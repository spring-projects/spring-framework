/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditor;
import java.net.URL;

import org.junit.jupiter.api.Test;

import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
class URLEditorTests {

	@Test
	void testCtorWithNullResourceEditor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new URLEditor(null));
	}

	@Test
	void testStandardURI() {
		PropertyEditor urlEditor = new URLEditor();
		urlEditor.setAsText("mailto:juergen.hoeller@interface21.com");
		Object value = urlEditor.getValue();
		assertThat(value).isInstanceOf(URL.class);
		URL url = (URL) value;
		assertThat(urlEditor.getAsText()).isEqualTo(url.toExternalForm());
	}

	@Test
	void testStandardURL() {
		PropertyEditor urlEditor = new URLEditor();
		urlEditor.setAsText("https://www.springframework.org");
		Object value = urlEditor.getValue();
		assertThat(value).isInstanceOf(URL.class);
		URL url = (URL) value;
		assertThat(urlEditor.getAsText()).isEqualTo(url.toExternalForm());
	}

	@Test
	void testClasspathURL() {
		PropertyEditor urlEditor = new URLEditor();
		urlEditor.setAsText("classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
				"/" + ClassUtils.getShortName(getClass()) + ".class");
		Object value = urlEditor.getValue();
		assertThat(value).isInstanceOf(URL.class);
		URL url = (URL) value;
		assertThat(urlEditor.getAsText()).isEqualTo(url.toExternalForm());
		assertThat(url.getProtocol()).doesNotStartWith("classpath");
	}

	@Test
	void testWithNonExistentResource() {
		PropertyEditor urlEditor = new URLEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				urlEditor.setAsText("gonna:/freak/in/the/morning/freak/in/the.evening"));
	}

	@Test
	void testSetAsTextWithNull() {
		PropertyEditor urlEditor = new URLEditor();
		urlEditor.setAsText(null);
		assertThat(urlEditor.getValue()).isNull();
		assertThat(urlEditor.getAsText()).isEmpty();
	}

	@Test
	void testGetAsTextReturnsEmptyStringIfValueNotSet() {
		PropertyEditor urlEditor = new URLEditor();
		assertThat(urlEditor.getAsText()).isEmpty();
	}

}
