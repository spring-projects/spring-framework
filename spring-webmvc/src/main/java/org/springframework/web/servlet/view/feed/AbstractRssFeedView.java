/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Item;

import org.springframework.http.MediaType;

/**
 * Abstract superclass for RSS Feed views, using the
 * <a href="https://github.com/rometools/rome">ROME</a> package.
 *
 * <p>><b>NOTE: As of Spring 4.1, this is based on the {@code com.rometools}
 * variant of ROME, version 1.5. Please upgrade your build dependency.</b>
 *
 * <p>Application-specific view classes will extend this class.
 * The view will be held in the subclass itself, not in a template.
 * Main entry points are the {@link #buildFeedMetadata} and {@link #buildFeedItems}.
 *
 * <p>Thanks to Jettro Coenradie and Sergio Bossa for the original feed view prototype!
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see #buildFeedMetadata
 * @see #buildFeedItems
 */
public abstract class AbstractRssFeedView extends AbstractFeedView<Channel> {

	public AbstractRssFeedView() {
		setContentType(MediaType.APPLICATION_RSS_XML_VALUE);
	}


	/**
	 * Create a new Channel instance to hold the entries.
	 * <p>By default returns an RSS 2.0 channel, but the subclass can specify any channel.
	 */
	@Override
	protected Channel newFeed() {
		return new Channel("rss_2.0");
	}

	/**
	 * Invokes {@link #buildFeedItems(Map, HttpServletRequest, HttpServletResponse)}
	 * to get a list of feed items.
	 */
	@Override
	protected final void buildFeedEntries(Map<String, Object> model, Channel channel,
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		List<Item> items = buildFeedItems(model, request, response);
		channel.setItems(items);
	}

	/**
	 * Subclasses must implement this method to build feed items, given the model.
	 * <p>Note that the passed-in HTTP response is just supposed to be used for
	 * setting cookies or other HTTP headers. The built feed itself will automatically
	 * get written to the response after this method returns.
	 * @param model	the model Map
	 * @param request  in case we need locale etc. Shouldn't look at attributes.
	 * @param response in case we need to set cookies. Shouldn't write to it.
	 * @return the feed items to be added to the feed
	 * @throws Exception any exception that occurred during document building
	 * @see Item
	 */
	protected abstract List<Item> buildFeedItems(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception;

}
