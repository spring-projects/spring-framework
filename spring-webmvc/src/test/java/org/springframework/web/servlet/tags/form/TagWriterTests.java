/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags.form;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Rick Evans
 */
class TagWriterTests {

	private final StringWriter data = new StringWriter();

	private final TagWriter writer = new TagWriter(this.data);


	@Test
	void simpleTag() throws Exception {
		this.writer.startTag("br");
		this.writer.endTag();

		assertThat(this.data.toString()).isEqualTo("<br/>");
	}

	@Test
	void emptyTag() throws Exception {
		this.writer.startTag("input");
		this.writer.writeAttribute("type", "text");
		this.writer.endTag();

		assertThat(this.data.toString()).isEqualTo("<input type=\"text\"/>");
	}

	@Test
	void simpleBlockTag() throws Exception {
		this.writer.startTag("textarea");
		this.writer.appendValue("foobar");
		this.writer.endTag();

		assertThat(this.data.toString()).isEqualTo("<textarea>foobar</textarea>");
	}

	@Test
	void blockTagWithAttributes() throws Exception {
		this.writer.startTag("textarea");
		this.writer.writeAttribute("width", "10");
		this.writer.writeAttribute("height", "20");
		this.writer.appendValue("foobar");
		this.writer.endTag();

		assertThat(this.data.toString()).isEqualTo("<textarea width=\"10\" height=\"20\">foobar</textarea>");
	}

	@Test
	void nestedTags() throws Exception {
		this.writer.startTag("span");
		this.writer.writeAttribute("style", "foo");
		this.writer.startTag("strong");
		this.writer.appendValue("Rob Harrop");
		this.writer.endTag();
		this.writer.endTag();

		assertThat(this.data.toString()).isEqualTo("<span style=\"foo\"><strong>Rob Harrop</strong></span>");
	}

	@Test
	void multipleNestedTags() throws Exception {
		this.writer.startTag("span");
		this.writer.writeAttribute("class", "highlight");
		{
			this.writer.startTag("strong");
			this.writer.appendValue("Rob");
			this.writer.endTag();
		}
		this.writer.appendValue(" ");
		{
			this.writer.startTag("emphasis");
			this.writer.appendValue("Harrop");
			this.writer.endTag();
		}
		this.writer.endTag();

		assertThat(this.data.toString()).isEqualTo("<span class=\"highlight\"><strong>Rob</strong> <emphasis>Harrop</emphasis></span>");
	}

	@Test
	void writeInterleavedWithForceBlock() throws Exception {
		this.writer.startTag("span");
		this.writer.forceBlock();
		this.data.write("Rob Harrop"); // interleaved writing
		this.writer.endTag();

		assertThat(this.data.toString()).isEqualTo("<span>Rob Harrop</span>");
	}

	@Test
	void appendingValue() throws Exception {
		this.writer.startTag("span");
		this.writer.appendValue("Rob ");
		this.writer.appendValue("Harrop");
		this.writer.endTag();

		assertThat(this.data.toString()).isEqualTo("<span>Rob Harrop</span>");
	}

}
