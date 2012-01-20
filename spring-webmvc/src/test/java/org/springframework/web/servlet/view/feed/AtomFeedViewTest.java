/*
 * Copyright ${YEAR} the original author or authors.
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

package org.springframework.web.servlet.view.feed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.syndication.feed.atom.Content;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.custommonkey.xmlunit.XMLUnit.setIgnoreWhitespace;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class AtomFeedViewTest {

	private AbstractAtomFeedView view;

	@Before
	public void createView() throws Exception {
		view = new MyAtomFeedView();
		setIgnoreWhitespace(true);
	}

	@Test
	public void render() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		Map<String, String> model = new HashMap<String, String>();
		model.put("1", "This is entry 1");
		model.put("2", "This is entry 2");

		view.render(model, request, response);
		assertEquals("Invalid content-type", "application/atom+xml", response.getContentType());
		String expected = "<feed xmlns=\"http://www.w3.org/2005/Atom\">" + "<title>Test Feed</title>" +
				"<entry><title>2</title><summary>This is entry 2</summary></entry>" +
				"<entry><title>1</title><summary>This is entry 1</summary></entry>" + "</feed>";
		assertXMLEqual(expected, response.getContentAsString());
	}

	private static class MyAtomFeedView extends AbstractAtomFeedView {

		@Override
		protected void buildFeedMetadata(Map model, Feed feed, HttpServletRequest request) {
			feed.setTitle("Test Feed");
		}

		@Override
		protected List<Entry> buildFeedEntries(Map model, HttpServletRequest request, HttpServletResponse response)
				throws Exception {
			List<Entry> entries = new ArrayList<Entry>();
			for (Iterator iterator = model.keySet().iterator(); iterator.hasNext();) {
				String name = (String) iterator.next();
				Entry entry = new Entry();
				entry.setTitle(name);
				Content content = new Content();
				content.setValue((String) model.get(name));
				entry.setSummary(content);
				entries.add(entry);
			}
			return entries;
		}
	}
}
