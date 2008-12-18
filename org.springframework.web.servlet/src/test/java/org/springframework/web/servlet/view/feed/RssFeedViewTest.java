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

import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Description;
import com.sun.syndication.feed.rss.Item;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.custommonkey.xmlunit.XMLUnit.setIgnoreWhitespace;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class RssFeedViewTest {

	private AbstractRssFeedView view;

	@Before
	public void createView() throws Exception {
		view = new MyRssFeedView();
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
		assertEquals("Invalid content-type", "application/rss+xml", response.getContentType());
		String expected = "<rss version=\"2.0\">" +
				"<channel><title>Test Feed</title><link>http://example.com</link><description>Test feed description</description>" +
				"<item><title>2</title><description>This is entry 2</description></item>" +
				"<item><title>1</title><description>This is entry 1</description></item>" + "</channel></rss>";
		assertXMLEqual(expected, response.getContentAsString());
	}

	private static class MyRssFeedView extends AbstractRssFeedView {

		@Override
		protected void buildFeedMetadata(Map model, Channel channel, HttpServletRequest request) {
			channel.setTitle("Test Feed");
			channel.setDescription("Test feed description");
			channel.setLink("http://example.com");
		}

		@Override
		protected List<Item> buildFeedItems(Map model, HttpServletRequest request, HttpServletResponse response)
				throws Exception {
			List<Item> items = new ArrayList<Item>();
			for (Iterator iterator = model.keySet().iterator(); iterator.hasNext();) {
				String name = (String) iterator.next();
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
