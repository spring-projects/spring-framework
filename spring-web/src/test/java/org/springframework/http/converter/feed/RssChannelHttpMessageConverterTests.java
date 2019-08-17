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

package org.springframework.http.converter.feed;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.tests.XmlContent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class RssChannelHttpMessageConverterTests {

	private RssChannelHttpMessageConverter converter;


	@BeforeEach
	public void setUp() {
		converter = new RssChannelHttpMessageConverter();
	}


	@Test
	public void canRead() {
		assertThat(converter.canRead(Channel.class, new MediaType("application", "rss+xml"))).isTrue();
		assertThat(converter.canRead(Channel.class, new MediaType("application", "rss+xml", StandardCharsets.UTF_8))).isTrue();
	}

	@Test
	public void canWrite() {
		assertThat(converter.canWrite(Channel.class, new MediaType("application", "rss+xml"))).isTrue();
		assertThat(converter.canWrite(Channel.class, new MediaType("application", "rss+xml", StandardCharsets.UTF_8))).isTrue();
	}

	@Test
	public void read() throws IOException {
		InputStream is = getClass().getResourceAsStream("rss.xml");
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(is);
		inputMessage.getHeaders().setContentType(new MediaType("application", "rss+xml", StandardCharsets.UTF_8));
		Channel result = converter.read(Channel.class, inputMessage);
		assertThat(result.getTitle()).isEqualTo("title");
		assertThat(result.getLink()).isEqualTo("https://example.com");
		assertThat(result.getDescription()).isEqualTo("description");

		List<?> items = result.getItems();
		assertThat(items.size()).isEqualTo(2);

		Item item1 = (Item) items.get(0);
		assertThat(item1.getTitle()).isEqualTo("title1");

		Item item2 = (Item) items.get(1);
		assertThat(item2.getTitle()).isEqualTo("title2");
	}

	@Test
	public void write() throws IOException, SAXException {
		Channel channel = new Channel("rss_2.0");
		channel.setTitle("title");
		channel.setLink("https://example.com");
		channel.setDescription("description");

		Item item1 = new Item();
		item1.setTitle("title1");

		Item item2 = new Item();
		item2.setTitle("title2");

		List<Item> items = new ArrayList<>(2);
		items.add(item1);
		items.add(item2);
		channel.setItems(items);

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(channel, null, outputMessage);

		assertThat(outputMessage.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(new MediaType("application", "rss+xml", StandardCharsets.UTF_8));
		String expected = "<rss version=\"2.0\">" +
				"<channel><title>title</title><link>https://example.com</link><description>description</description>" +
				"<item><title>title1</title></item>" +
				"<item><title>title2</title></item>" +
				"</channel></rss>";
		assertThat(XmlContent.of(outputMessage.getBodyAsString(StandardCharsets.UTF_8)))
				.isSimilarToIgnoringWhitespace(expected);
	}

	@Test
	public void writeOtherCharset() throws IOException, SAXException {
		Channel channel = new Channel("rss_2.0");
		channel.setTitle("title");
		channel.setLink("https://example.com");
		channel.setDescription("description");

		String encoding = "ISO-8859-1";
		channel.setEncoding(encoding);

		Item item1 = new Item();
		item1.setTitle("title1");

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(channel, null, outputMessage);

		assertThat(outputMessage.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(new MediaType("application", "rss+xml", Charset.forName(encoding)));
	}

}
