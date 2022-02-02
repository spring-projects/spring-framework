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

import com.rometools.rome.feed.atom.Entry;
import com.rometools.rome.feed.atom.Feed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.diff.NodeMatcher;

import org.springframework.core.testfixture.xml.XmlContent;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Arjen Poutsma
 */
public class AtomFeedHttpMessageConverterTests {

	private static final MediaType ATOM_XML_UTF8 =
			new MediaType(MediaType.APPLICATION_ATOM_XML, StandardCharsets.UTF_8);


	private AtomFeedHttpMessageConverter converter;


	@BeforeEach
	public void setUp() {
		converter = new AtomFeedHttpMessageConverter();
	}


	@Test
	public void canRead() {
		assertThat(converter.canRead(Feed.class, MediaType.APPLICATION_ATOM_XML)).isTrue();
		assertThat(converter.canRead(Feed.class, ATOM_XML_UTF8)).isTrue();
	}

	@Test
	public void canWrite() {
		assertThat(converter.canWrite(Feed.class, MediaType.APPLICATION_ATOM_XML)).isTrue();
		assertThat(converter.canWrite(Feed.class, ATOM_XML_UTF8)).isTrue();
	}

	@Test
	public void read() throws IOException {
		InputStream inputStream = spy(getClass().getResourceAsStream("atom.xml"));
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(inputStream);
		inputMessage.getHeaders().setContentType(ATOM_XML_UTF8);
		Feed result = converter.read(Feed.class, inputMessage);
		assertThat(result.getTitle()).isEqualTo("title");
		assertThat(result.getSubtitle().getValue()).isEqualTo("subtitle");
		List<?> entries = result.getEntries();
		assertThat(entries.size()).isEqualTo(2);

		Entry entry1 = (Entry) entries.get(0);
		assertThat(entry1.getId()).isEqualTo("id1");
		assertThat(entry1.getTitle()).isEqualTo("title1");

		Entry entry2 = (Entry) entries.get(1);
		assertThat(entry2.getId()).isEqualTo("id2");
		assertThat(entry2.getTitle()).isEqualTo("title2");
		verify(inputStream, never()).close();
	}

	@Test
	public void write() throws IOException {
		Feed feed = new Feed("atom_1.0");
		feed.setTitle("title");

		Entry entry1 = new Entry();
		entry1.setId("id1");
		entry1.setTitle("title1");

		Entry entry2 = new Entry();
		entry2.setId("id2");
		entry2.setTitle("title2");

		List<Entry> entries = new ArrayList<>(2);
		entries.add(entry1);
		entries.add(entry2);
		feed.setEntries(entries);

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(feed, null, outputMessage);

		assertThat(outputMessage.getHeaders().getContentType())
				.as("Invalid content-type")
				.isEqualTo(ATOM_XML_UTF8);
		String expected = "<feed xmlns=\"http://www.w3.org/2005/Atom\">" + "<title>title</title>" +
				"<entry><id>id1</id><title>title1</title></entry>" +
				"<entry><id>id2</id><title>title2</title></entry></feed>";
		NodeMatcher nm = new DefaultNodeMatcher(ElementSelectors.byName);
		assertThat(XmlContent.of(outputMessage.getBodyAsString(StandardCharsets.UTF_8)))
				.isSimilarToIgnoringWhitespace(expected, nm);
		verify(outputMessage.getBody(), never()).close();
	}

	@Test
	public void writeOtherCharset() throws IOException {
		Feed feed = new Feed("atom_1.0");
		feed.setTitle("title");
		String encoding = "ISO-8859-1";
		feed.setEncoding(encoding);

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(feed, null, outputMessage);

		assertThat(outputMessage.getHeaders().getContentType())
				.as("Invalid content-type")
				.isEqualTo(new MediaType("application", "atom+xml", Charset.forName(encoding)));
	}

	@Test
	public void writeOtherContentTypeParameters() throws IOException {
		MockHttpOutputMessage message = new MockHttpOutputMessage();
		MediaType contentType = new MediaType("application", "atom+xml", singletonMap("type", "feed"));
		converter.write(new Feed("atom_1.0"), contentType, message);

		assertThat(message.getHeaders().getContentType().getParameters())
				.as("Invalid content-type")
				.hasSize(2)
				.containsEntry("type", "feed")
				.containsEntry("charset", "UTF-8");
	}

}
