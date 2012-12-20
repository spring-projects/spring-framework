/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.ArrayList;
import java.util.List;

import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;
import static org.custommonkey.xmlunit.XMLAssert.*;
import org.custommonkey.xmlunit.XMLUnit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;

/** @author Arjen Poutsma */
public class AtomFeedHttpMessageConverterTests {

	private AtomFeedHttpMessageConverter converter;

	private Charset utf8;

	@Before
	public void setUp() {
		utf8 = Charset.forName("UTF-8");
		converter = new AtomFeedHttpMessageConverter();
		XMLUnit.setIgnoreWhitespace(true);
	}

	@Test
	public void canRead() {
		assertTrue(converter.canRead(Feed.class, new MediaType("application", "atom+xml")));
		assertTrue(converter.canRead(Feed.class, new MediaType("application", "atom+xml", utf8)));
	}

	@Test
	public void canWrite() {
		assertTrue(converter.canWrite(Feed.class, new MediaType("application", "atom+xml")));
		assertTrue(converter.canWrite(Feed.class, new MediaType("application", "atom+xml", Charset.forName("UTF-8"))));
	}

	@Test
	public void read() throws IOException {
		InputStream is = getClass().getResourceAsStream("atom.xml");
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(is);
		inputMessage.getHeaders().setContentType(new MediaType("application", "atom+xml", utf8));
		Feed result = converter.read(Feed.class, inputMessage);
		assertEquals("title", result.getTitle());
		assertEquals("subtitle", result.getSubtitle().getValue());
		List entries = result.getEntries();
		assertEquals(2, entries.size());

		Entry entry1 = (Entry) entries.get(0);
		assertEquals("id1", entry1.getId());
		assertEquals("title1", entry1.getTitle());

		Entry entry2 = (Entry) entries.get(1);
		assertEquals("id2", entry2.getId());
		assertEquals("title2", entry2.getTitle());
	}

	@Test
	public void write() throws IOException, SAXException {
		Feed feed = new Feed("atom_1.0");
		feed.setTitle("title");

		Entry entry1 = new Entry();
		entry1.setId("id1");
		entry1.setTitle("title1");

		Entry entry2 = new Entry();
		entry2.setId("id2");
		entry2.setTitle("title2");

		List entries = new ArrayList(2);
		entries.add(entry1);
		entries.add(entry2);
		feed.setEntries(entries);

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(feed, null, outputMessage);

		assertEquals("Invalid content-type", new MediaType("application", "atom+xml", utf8),
				outputMessage.getHeaders().getContentType());
		String expected = "<feed xmlns=\"http://www.w3.org/2005/Atom\">" + "<title>title</title>" +
				"<entry><id>id1</id><title>title1</title></entry>" +
				"<entry><id>id2</id><title>title2</title></entry></feed>";
		assertXMLEqual(expected, outputMessage.getBodyAsString(utf8));

	}

	@Test
	public void writeOtherCharset() throws IOException, SAXException {
		Feed feed = new Feed("atom_1.0");
		feed.setTitle("title");
		String encoding = "ISO-8859-1";
		feed.setEncoding(encoding);

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(feed, null, outputMessage);

		assertEquals("Invalid content-type", new MediaType("application", "atom+xml", Charset.forName(encoding)),
				outputMessage.getHeaders().getContentType());
	}


}
