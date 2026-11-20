/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void ctorWithNullResourceEditor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new URLEditor(null));
	}

	@Test
	void standardURI() {
		PropertyEditor urlEditor = new URLEditor();
		urlEditor.setAsText("mailto:juergen.hoeller@interface21.com");
		Object value = urlEditor.getValue();
		assertThat(value).isInstanceOf(URL.class);
		URL url = (URL) value;
		assertThat(urlEditor.getAsText()).isEqualTo(url.toExternalForm());
	}

	@Test
	void standardURL() {
		PropertyEditor urlEditor = new URLEditor();
		urlEditor.setAsText("https://www.springframework.org");
		Object value = urlEditor.getValue();
		assertThat(value).isInstanceOf(URL.class);
		URL url = (URL) value;
		assertThat(urlEditor.getAsText()).isEqualTo(url.toExternalForm());
	}

	@Test
	void classpathURL() {
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
	void withNonExistentResource() {
		PropertyEditor urlEditor = new URLEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				urlEditor.setAsText("gonna:/freak/in/the/morning/freak/in/the.evening"));
	}

	@Test
	void setAsTextWithNull() {
		PropertyEditor urlEditor = new URLEditor();
		urlEditor.setAsText(null);
		assertThat(urlEditor.getValue()).isNull();
		assertThat(urlEditor.getAsText()).isEmpty();
	}

	@Test
	void getAsTextReturnsEmptyStringIfValueNotSet() {
		PropertyEditor urlEditor = new URLEditor();
		assertThat(urlEditor.getAsText()).isEmpty();
	}

}
