/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.view.feed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Description;
import com.rometools.rome.feed.rss.Item;
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
@SuppressWarnings("removal")
class RssFeedViewTests {

	private final AbstractRssFeedView view = new MyRssFeedView();


	@Test
	void render() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		Map<String, String> model = new LinkedHashMap<>();
		model.put("2", "This is entry 2");
		model.put("1", "This is entry 1");

		view.render(model, request, response);
		assertThat(response.getContentType()).as("Invalid content-type").isEqualTo("application/rss+xml");
		String expected = "<rss version=\"2.0\">" +
				"<channel><title>Test Feed</title>" +
				"<link>https://example.com</link>" +
				"<description>Test feed description</description>" +
				"<item><title>2</title><description>This is entry 2</description></item>" +
				"<item><title>1</title><description>This is entry 1</description></item>" +
				"</channel></rss>";
		assertThat(XmlContent.of(response.getContentAsString())).isSimilarToIgnoringWhitespace(expected);
	}


	private static class MyRssFeedView extends AbstractRssFeedView {

		@Override
		protected void buildFeedMetadata(Map<String, Object> model, Channel channel, HttpServletRequest request) {
			channel.setTitle("Test Feed");
			channel.setDescription("Test feed description");
			channel.setLink("https://example.com");
		}

		@Override
		protected List<Item> buildFeedItems(Map<String, Object> model,
				HttpServletRequest request, HttpServletResponse response) {

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
