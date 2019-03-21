/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.view.feed;

import com.rometools.rome.feed.atom.Content;
import com.rometools.rome.feed.atom.Entry;
import com.rometools.rome.feed.atom.Feed;
import org.junit.Test;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.xmlunit.matchers.CompareMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Arjen Poutsma
 */
public class AtomFeedViewTests {

	private final AbstractAtomFeedView view = new MyAtomFeedView();

	@Test
	public void render() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		Map<String, String> model = new LinkedHashMap<>();
		model.put("2", "This is entry 2");
		model.put("1", "This is entry 1");

		view.render(model, request, response);
		assertEquals("Invalid content-type", "application/atom+xml", response.getContentType());
		String expected = "<feed xmlns=\"http://www.w3.org/2005/Atom\">" + "<title>Test Feed</title>" +
				"<entry><title>2</title><summary>This is entry 2</summary></entry>" +
				"<entry><title>1</title><summary>This is entry 1</summary></entry>" + "</feed>";
		assertThat(response.getContentAsString(), isSimilarTo(expected));
	}

	private static CompareMatcher isSimilarTo(String content) {
		return CompareMatcher.isSimilarTo(content).ignoreWhitespace();
	}

	private static class MyAtomFeedView extends AbstractAtomFeedView {

		@Override
		protected void buildFeedMetadata(Map<String, Object>model, Feed feed, HttpServletRequest request) {
			feed.setTitle("Test Feed");
		}

		@Override
		protected List<Entry> buildFeedEntries(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {
			List<Entry> entries = new ArrayList<>();
			for (String name : model.keySet()) {
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
