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
import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 */
class URIEditorTests {

	@Test
	void standardURI() {
		doTestURI("mailto:juergen.hoeller@interface21.com");
	}

	@Test
	void withNonExistentResource() {
		doTestURI("gonna:/freak/in/the/morning/freak/in/the.evening");
	}

	@Test
	void standardURL() {
		doTestURI("https://www.springframework.org");
	}

	@Test
	void standardURLWithFragment() {
		doTestURI("https://www.springframework.org#1");
	}

	@Test
	void standardURLWithWhitespace() {
		PropertyEditor uriEditor = new URIEditor();
		uriEditor.setAsText("  https://www.springframework.org  ");
		Object value = uriEditor.getValue();
		assertThat(value).isInstanceOf(URI.class);
		URI uri = (URI) value;
		assertThat(uri.toString()).isEqualTo("https://www.springframework.org");
	}

	@Test
	void classpathURL() {
		PropertyEditor uriEditor = new URIEditor(getClass().getClassLoader());
		uriEditor.setAsText("classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
				"/" + ClassUtils.getShortName(getClass()) + ".class");
		Object value = uriEditor.getValue();
		assertThat(value).isInstanceOf(URI.class);
		URI uri = (URI) value;
		assertThat(uriEditor.getAsText()).isEqualTo(uri.toString());
		assertThat(uri.getScheme()).doesNotStartWith("classpath");
	}

	@Test
	void classpathURLWithWhitespace() {
		PropertyEditor uriEditor = new URIEditor(getClass().getClassLoader());
		uriEditor.setAsText("  classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
				"/" + ClassUtils.getShortName(getClass()) + ".class  ");
		Object value = uriEditor.getValue();
		assertThat(value).isInstanceOf(URI.class);
		URI uri = (URI) value;
		assertThat(uriEditor.getAsText()).isEqualTo(uri.toString());
		assertThat(uri.getScheme()).doesNotStartWith("classpath");
	}

	@Test
	void classpathURLAsIs() {
		PropertyEditor uriEditor = new URIEditor();
		uriEditor.setAsText("classpath:test.txt");
		Object value = uriEditor.getValue();
		assertThat(value).isInstanceOf(URI.class);
		URI uri = (URI) value;
		assertThat(uriEditor.getAsText()).isEqualTo(uri.toString());
		assertThat(uri.getScheme()).startsWith("classpath");
	}

	@Test
	void setAsTextWithNull() {
		PropertyEditor uriEditor = new URIEditor();
		uriEditor.setAsText(null);
		assertThat(uriEditor.getValue()).isNull();
		assertThat(uriEditor.getAsText()).isEmpty();
	}

	@Test
	void getAsTextReturnsEmptyStringIfValueNotSet() {
		PropertyEditor uriEditor = new URIEditor();
		assertThat(uriEditor.getAsText()).isEmpty();
	}

	@Test
	void encodeURI() {
		PropertyEditor uriEditor = new URIEditor();
		uriEditor.setAsText("https://example.com/spaces and \u20AC");
		Object value = uriEditor.getValue();
		assertThat(value).isInstanceOf(URI.class);
		URI uri = (URI) value;
		assertThat(uriEditor.getAsText()).isEqualTo(uri.toString());
		assertThat(uri.toASCIIString()).isEqualTo("https://example.com/spaces%20and%20%E2%82%AC");
	}

	@Test
	void encodeAlreadyEncodedURI() {
		PropertyEditor uriEditor = new URIEditor(false);
		uriEditor.setAsText("https://example.com/spaces%20and%20%E2%82%AC");
		Object value = uriEditor.getValue();
		assertThat(value).isInstanceOf(URI.class);
		URI uri = (URI) value;
		assertThat(uriEditor.getAsText()).isEqualTo(uri.toString());
		assertThat(uri.toASCIIString()).isEqualTo("https://example.com/spaces%20and%20%E2%82%AC");
	}


	private void doTestURI(String uriSpec) {
		PropertyEditor uriEditor = new URIEditor();
		uriEditor.setAsText(uriSpec);
		Object value = uriEditor.getValue();
		assertThat(value).isInstanceOf(URI.class);
		URI uri = (URI) value;
		assertThat(uri.toString()).isEqualTo(uriSpec);
	}

}
