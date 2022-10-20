/*
 * Copyright 2002-2022 the original author or authors.
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
import org.junit.jupiter.api.Test;

import org.springframework.core.testfixture.xml.XmlContent;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class RssChannelHttpMessageConverterTests {

	private static final MediaType RSS_XML_UTF8 = new MediaType(MediaType.APPLICATION_RSS_XML, StandardCharsets.UTF_8);

	private final RssChannelHttpMessageConverter converter = new RssChannelHttpMessageConverter();


	@Test
	public void canReadAndWrite() {
		assertThat(converter.canRead(Channel.class, MediaType.APPLICATION_RSS_XML)).isTrue();
		assertThat(converter.canRead(Channel.class, RSS_XML_UTF8)).isTrue();

		assertThat(converter.canWrite(Channel.class, MediaType.APPLICATION_RSS_XML)).isTrue();
		assertThat(converter.canWrite(Channel.class, RSS_XML_UTF8)).isTrue();
	}

	@Test
	public void read() throws IOException {
		InputStream inputStream = getClass().getResourceAsStream("rss.xml");
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(inputStream);
		inputMessage.getHeaders().setContentType(RSS_XML_UTF8);
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
	public void write() throws IOException {
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

		assertThat(outputMessage.getHeaders().getContentType())
				.as("Invalid content-type")
				.isEqualTo(RSS_XML_UTF8);
		String expected = "<rss version=\"2.0\">" +
				"<channel><title>title</title><link>https://example.com</link><description>description</description>" +
				"<item><title>title1</title></item>" +
				"<item><title>title2</title></item>" +
				"</channel></rss>";
		assertThat(XmlContent.of(outputMessage.getBodyAsString(StandardCharsets.UTF_8)))
				.isSimilarToIgnoringWhitespace(expected);
	}

	@Test
	public void writeOtherCharset() throws IOException {
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

		assertThat(outputMessage.getHeaders().getContentType())
				.as("Invalid content-type")
				.isEqualTo(new MediaType("application", "rss+xml", Charset.forName(encoding)));
	}

	@Test
	public void writeOtherContentTypeParameters() throws IOException {
		Channel channel = new Channel("rss_2.0");
		channel.setTitle("title");
		channel.setLink("https://example.com");
		channel.setDescription("description");

		MockHttpOutputMessage message = new MockHttpOutputMessage();
		converter.write(channel, new MediaType("application", "rss+xml", singletonMap("x", "y")), message);

		assertThat(message.getHeaders().getContentType().getParameters())
				.as("Invalid content-type")
				.hasSize(2)
				.containsEntry("x", "y")
				.containsEntry("charset", "UTF-8");
	}

}
