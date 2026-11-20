/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		assertThat(p.get("foo")).as("foo=bar").isEqualTo("bar");
	}

	@Test
	void twoProperties() {
		String s = "foo=bar with whitespace\n" +
			"me=mi";
		PropertiesEditor pe= new PropertiesEditor();
		pe.setAsText(s);
		Properties p = (Properties) pe.getValue();
		assertThat(p.entrySet().size()).as("contains two entries").isEqualTo(2);
		assertThat(p.get("foo")).as("foo=bar with whitespace").isEqualTo("bar with whitespace");
		assertThat(p.get("me")).as("me=mi").isEqualTo("mi");
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
		assertThat(p.get("foo")).as("foo=bar").isEqualTo("bar");
		assertThat(p.get("me")).as("me=mi").isEqualTo("mi");
		assertThat(p.get("x")).as("x='y=z'").isEqualTo("y=z");
	}

	@Test
	void handlesEmptyProperty() {
		String s = "foo=bar\nme=mi\nx=";
		PropertiesEditor pe= new PropertiesEditor();
		pe.setAsText(s);
		Properties p = (Properties) pe.getValue();
		assertThat(p.entrySet().size()).as("contains two entries").isEqualTo(3);
		assertThat(p.get("foo")).as("foo=bar").isEqualTo("bar");
		assertThat(p.get("me")).as("me=mi").isEqualTo("mi");
		assertThat(p.get("x")).as("x='y=z'").isEqualTo("");
	}

	@Test
	void handlesEmptyPropertyWithoutEquals() {
		String s = "foo\nme=mi\nx=x";
		PropertiesEditor pe= new PropertiesEditor();
		pe.setAsText(s);
		Properties p = (Properties) pe.getValue();
		assertThat(p.entrySet().size()).as("contains three entries").isEqualTo(3);
		assertThat(p.get("foo")).as("foo is empty").isEqualTo("");
		assertThat(p.get("me")).as("me=mi").isEqualTo("mi");
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
		assertThat(p.get("foo")).as("foo is bar").isEqualTo("bar");
		assertThat(p.get("me")).as("me=mi").isEqualTo("mi");
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
