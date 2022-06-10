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

package org.springframework.web.servlet.view.feed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.rometools.rome.feed.atom.Content;
import com.rometools.rome.feed.atom.Entry;
import com.rometools.rome.feed.atom.Feed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.core.testfixture.xml.XmlContent;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(response.getContentType()).as("Invalid content-type").isEqualTo("application/atom+xml");
		String expected = "<feed xmlns=\"http://www.w3.org/2005/Atom\">" + "<title>Test Feed</title>" +
				"<entry><title>2</title><summary>This is entry 2</summary></entry>" +
				"<entry><title>1</title><summary>This is entry 1</summary></entry>" + "</feed>";
		assertThat(XmlContent.of(response.getContentAsString())).isSimilarToIgnoringWhitespace(expected);
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
