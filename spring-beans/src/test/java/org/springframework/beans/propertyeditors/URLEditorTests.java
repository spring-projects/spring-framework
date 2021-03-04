/*
 * Copyright 2002-2019 the original author or authors.
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
public class URLEditorTests {

	@Test
	public void testCtorWithNullResourceEditor() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new URLEditor(null));
	}

	@Test
	public void testStandardURI() throws Exception {
		PropertyEditor urlEditor = new URLEditor();
		urlEditor.setAsText("mailto:juergen.hoeller@interface21.com");
		Object value = urlEditor.getValue();
		boolean condition = value instanceof URL;
		assertThat(condition).isTrue();
		URL url = (URL) value;
		assertThat(urlEditor.getAsText()).isEqualTo(url.toExternalForm());
	}

	@Test
	public void testStandardURL() throws Exception {
		PropertyEditor urlEditor = new URLEditor();
		urlEditor.setAsText("https://www.springframework.org");
		Object value = urlEditor.getValue();
		boolean condition = value instanceof URL;
		assertThat(condition).isTrue();
		URL url = (URL) value;
		assertThat(urlEditor.getAsText()).isEqualTo(url.toExternalForm());
	}

	@Test
	public void testClasspathURL() throws Exception {
		PropertyEditor urlEditor = new URLEditor();
		urlEditor.setAsText("classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
				"/" + ClassUtils.getShortName(getClass()) + ".class");
		Object value = urlEditor.getValue();
		boolean condition1 = value instanceof URL;
		assertThat(condition1).isTrue();
		URL url = (URL) value;
		assertThat(urlEditor.getAsText()).isEqualTo(url.toExternalForm());
		boolean condition = !url.getProtocol().startsWith("classpath");
		assertThat(condition).isTrue();
	}

	@Test
	public void testWithNonExistentResource() throws Exception {
		PropertyEditor urlEditor = new URLEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				urlEditor.setAsText("gonna:/freak/in/the/morning/freak/in/the.evening"));
	}

	@Test
	public void testSetAsTextWithNull() throws Exception {
		PropertyEditor urlEditor = new URLEditor();
		urlEditor.setAsText(null);
		assertThat(urlEditor.getValue()).isNull();
		assertThat(urlEditor.getAsText()).isEqualTo("");
	}

	@Test
	public void testGetAsTextReturnsEmptyStringIfValueNotSet() throws Exception {
		PropertyEditor urlEditor = new URLEditor();
		assertThat(urlEditor.getAsText()).isEqualTo("");
	}

}
