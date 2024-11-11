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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the conversion of Strings to {@link java.util.Properties} objects,
 * and other property editors.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 */
class PropertiesEditorTests {

	@Test
	void oneProperty() {
		String s = "foo=bar";
		PropertiesEditor pe= new PropertiesEditor();
		pe.setAsText(s);
		Properties p = (Properties) pe.getValue();
		assertThat(p.entrySet().size()).as("contains one entry").isEqualTo(1);
		assertThat(p.get("foo").equals("bar")).as("foo=bar").isTrue();
	}

	@Test
	void twoProperties() {
		String s = "foo=bar with whitespace\n" +
			"me=mi";
		PropertiesEditor pe= new PropertiesEditor();
		pe.setAsText(s);
		Properties p = (Properties) pe.getValue();
		assertThat(p.entrySet().size()).as("contains two entries").isEqualTo(2);
		assertThat(p.get("foo").equals("bar with whitespace")).as("foo=bar with whitespace").isTrue();
		assertThat(p.get("me").equals("mi")).as("me=mi").isTrue();
	}

	@Test
	void handlesEqualsInValue() {
		String s = """
				foo=bar
				me=mi
				x=y=z""";
		PropertiesEditor pe= new PropertiesEditor();
		pe.setAsText(s);
		Properties p = (Properties) pe.getValue();
		assertThat(p.entrySet().size()).as("contains two entries").isEqualTo(3);
		assertThat(p.get("foo").equals("bar")).as("foo=bar").isTrue();
		assertThat(p.get("me").equals("mi")).as("me=mi").isTrue();
		assertThat(p.get("x").equals("y=z")).as("x='y=z'").isTrue();
	}

	@Test
	void handlesEmptyProperty() {
		String s = "foo=bar\nme=mi\nx=";
		PropertiesEditor pe= new PropertiesEditor();
		pe.setAsText(s);
		Properties p = (Properties) pe.getValue();
		assertThat(p.entrySet().size()).as("contains two entries").isEqualTo(3);
		assertThat(p.get("foo").equals("bar")).as("foo=bar").isTrue();
		assertThat(p.get("me").equals("mi")).as("me=mi").isTrue();
		assertThat(p.get("x").equals("")).as("x='y=z'").isTrue();
	}

	@Test
	void handlesEmptyPropertyWithoutEquals() {
		String s = "foo\nme=mi\nx=x";
		PropertiesEditor pe= new PropertiesEditor();
		pe.setAsText(s);
		Properties p = (Properties) pe.getValue();
		assertThat(p.entrySet().size()).as("contains three entries").isEqualTo(3);
		assertThat(p.get("foo").equals("")).as("foo is empty").isTrue();
		assertThat(p.get("me").equals("mi")).as("me=mi").isTrue();
	}

	/**
	 * Comments begin with #
	 */
	@Test
	void ignoresCommentLinesAndEmptyLines() {
		String s = """
				#Ignore this comment
				foo=bar
				#Another=comment more junk /
				me=mi
				x=x

				""";
		PropertiesEditor pe= new PropertiesEditor();
		pe.setAsText(s);
		Properties p = (Properties) pe.getValue();
		assertThat(p.entrySet().size()).as("contains three entries").isEqualTo(3);
		assertThat(p.get("foo").equals("bar")).as("foo is bar").isTrue();
		assertThat(p.get("me").equals("mi")).as("me=mi").isTrue();
	}

	/**
	 * We'll typically align by indenting with tabs or spaces.
	 * These should be ignored if at the beginning of a line.
	 * We must ensure that comment lines beginning with whitespace are
	 * still ignored: The standard syntax doesn't allow this on JDK 1.3.
	 */
	@Test
	void ignoresLeadingSpacesAndTabs() {
		String s = "    #Ignore this comment\n" +
			"\t\tfoo=bar\n" +
			"\t#Another comment more junk \n" +
			" me=mi\n" +
			"x=x\n" +
			"\n";
		PropertiesEditor pe= new PropertiesEditor();
		pe.setAsText(s);
		Properties p = (Properties) pe.getValue();
		assertThat(p).contains(entry("foo", "bar"), entry("me", "mi"));
		assertThat(p).hasSize(3);
	}

	@Test
	void nullValue() {
		PropertiesEditor pe= new PropertiesEditor();
		pe.setAsText(null);
		Properties p = (Properties) pe.getValue();
		assertThat(p).isEmpty();
	}

	@Test
	void emptyString() {
		PropertiesEditor pe = new PropertiesEditor();
		pe.setAsText("");
		Properties p = (Properties) pe.getValue();
		assertThat(p.isEmpty()).as("empty string means empty properties").isTrue();
	}

	@Test
	void usingMapAsValueSource() {
		Map<String, String> map = new HashMap<>();
		map.put("one", "1");
		map.put("two", "2");
		map.put("three", "3");
		PropertiesEditor pe = new PropertiesEditor();
		pe.setValue(map);
		Object value = pe.getValue();
		assertThat(value).isNotNull();
		assertThat(value).isInstanceOf(Properties.class);
		Properties props = (Properties) value;
		assertThat(props).hasSize(3);
		assertThat(props.getProperty("one")).isEqualTo("1");
		assertThat(props.getProperty("two")).isEqualTo("2");
		assertThat(props.getProperty("three")).isEqualTo("3");
	}

}
