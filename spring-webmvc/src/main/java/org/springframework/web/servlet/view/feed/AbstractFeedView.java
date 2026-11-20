/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.view.feed;

import java.io.OutputStreamWriter;
import java.util.Map;

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.io.WireFeedOutput;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.view.AbstractView;

/**
 * Abstract base class for Atom and RSS Feed views, using the
 * <a href="https://github.com/rometools/rome">ROME</a> package.
 *
 * <p><b>NOTE: As of Spring 4.1, this is based on the {@code com.rometools}
 * variant of ROME, version 1.5. Please upgrade your build dependency.</b>
 *
 * <p>Application-specific view classes will typically extend from either
 * {@link AbstractRssFeedView} or {@link AbstractAtomFeedView} instead of from this class.
 *
 * <p>Thanks to Jettro Coenradie and Sergio Bossa for the original feed view prototype!
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @param <T> the {@link WireFeed} type
 * @see AbstractRssFeedView
 * @see AbstractAtomFeedView
 * @deprecated since 7.0. Libraries or applications can provide their own {@code View}
 * class or perform rendering in web handlers directly.
 */
@Deprecated(since = "7.0", forRemoval = true)
public abstract class AbstractFeedView<T extends WireFeed> extends AbstractView {

	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		T wireFeed = newFeed();
		buildFeedMetadata(model, wireFeed, request);
		buildFeedEntries(model, wireFeed, request, response);

		setResponseContentType(request, response);
		if (!StringUtils.hasText(wireFeed.getEncoding())) {
			wireFeed.setEncoding("UTF-8");
		}

		WireFeedOutput feedOutput = new WireFeedOutput();
		ServletOutputStream out = response.getOutputStream();
		feedOutput.output(wireFeed, new OutputStreamWriter(out, wireFeed.getEncoding()));
		out.flush();
	}

	/**
	 * Create a new feed to hold the entries.
	 * @return the newly created Feed instance
	 */
	protected abstract T newFeed();

	/**
	 * Populate the feed metadata (title, link, description, etc.).
	 * <p>Default is an empty implementation. Subclasses can override this method
	 * to add meta fields such as title, link description, etc.
	 * @param model the model, in case meta information must be populated from it
	 * @param feed the feed being populated
	 * @param request in case we need locale etc. Shouldn't look at attributes.
	 */
	protected void buildFeedMetadata(Map<String, Object> model, T feed, HttpServletRequest request) {
	}

	/**
	 * Subclasses must implement this method to build feed entries, given the model.
	 * <p>Note that the passed-in HTTP response is just supposed to be used for
	 * setting cookies or other HTTP headers. The built feed itself will automatically
	 * get written to the response after this method returns.
	 * @param model the model Map
	 * @param feed the feed to add entries to
	 * @param request in case we need locale etc. Shouldn't look at attributes.
	 * @param response in case we need to set cookies. Shouldn't write to it.
	 * @throws Exception any exception that occurred during building
	 */
	protected abstract void buildFeedEntries(Map<String, Object> model, T feed,
			HttpServletRequest request, HttpServletResponse response) throws Exception;

}
