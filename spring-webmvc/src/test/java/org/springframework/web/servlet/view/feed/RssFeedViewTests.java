/*
 * Copyright 2002-2014 the original author or authors.
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

import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Description;
import com.rometools.rome.feed.rss.Item;
import org.junit.Before;
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
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

/**
 * @author Arjen Poutsma
 */
public class RssFeedViewTests {

	private AbstractRssFeedView view;

	@Before
	public void createView() throws Exception {
		view = new MyRssFeedView();
	}

	@Test
	public void render() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		Map<String, String> model = new LinkedHashMap<>();
		model.put("2", "This is entry 2");
		model.put("1", "This is entry 1");

		view.render(model, request, response);
		assertEquals("Invalid content-type", "application/rss+xml", response.getContentType());
		String expected = "<rss version=\"2.0\">" +
				"<channel><title>Test Feed</title><link>http://example.com</link><description>Test feed description</description>" +
				"<item><title>2</title><description>This is entry 2</description></item>" +
				"<item><title>1</title><description>This is entry 1</description></item>" + "</channel></rss>";
		assertThat(response.getContentAsString(), isSimilarTo(expected).ignoreWhitespace());
	}


	private static class MyRssFeedView extends AbstractRssFeedView {

		@Override
		protected void buildFeedMetadata(Map model, Channel channel, HttpServletRequest request) {
			channel.setTitle("Test Feed");
			channel.setDescription("Test feed description");
			channel.setLink("http://example.com");
		}

		@Override
		protected List<Item> buildFeedItems(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
			List<Item> items = new ArrayList<>();
			for (String name : model.keySet()) {
				Item item = new Item();
				item.setTitle(name);
				Description description = new Description();
				description.setValue((String) model.get(name));
				item.setDescription(description);
				items.add(item);
			}
			return items;
		}
	}

}
