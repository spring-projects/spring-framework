/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.resource;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link ResourceTransformer} implementation that modifies links in a CSS
 * file to match the public URL paths that should be exposed to clients (e.g.
 * with an MD5 content-based hash inserted in the URL).
 *
 * <p>The implementation looks for links in CSS {@code @import} statements and
 * also inside CSS {@code url()} functions. All links are then passed through the
 * {@link ResourceResolverChain} and resolved relative to the location of the
 * containing CSS file. If successfully resolved, the link is modified, otherwise
 * the original link is preserved.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class CssLinkResourceTransformer extends ResourceTransformerSupport {

	private static final Log logger = LogFactory.getLog(CssLinkResourceTransformer.class);

	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");


	private final List<CssLinkParser> linkParsers = new ArrayList<>();


	public CssLinkResourceTransformer() {
		this.linkParsers.add(new ImportStatementCssLinkParser());
		this.linkParsers.add(new UrlFunctionCssLinkParser());
	}


	@Override
	public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain)
			throws IOException {

		resource = transformerChain.transform(request, resource);

		String filename = resource.getFilename();
		if (!"css".equals(StringUtils.getFilenameExtension(filename))) {
			return resource;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Transforming resource: " + resource);
		}

		byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
		String content = new String(bytes, DEFAULT_CHARSET);

		Set<CssLinkInfo> infos = new HashSet<>(5);
		for (CssLinkParser parser : this.linkParsers) {
			parser.parseLink(content, infos);
		}

		if (infos.isEmpty()) {
			if (logger.isTraceEnabled()) {
				logger.trace("No links found.");
			}
			return resource;
		}

		List<CssLinkInfo> sortedInfos = new ArrayList<>(infos);
		Collections.sort(sortedInfos);

		int index = 0;
		StringWriter writer = new StringWriter();
		for (CssLinkInfo info : sortedInfos) {
			writer.write(content.substring(index, info.getStart()));
			String link = content.substring(info.getStart(), info.getEnd());
			String newLink = null;
			if (!hasScheme(link)) {
				newLink = resolveUrlPath(link, request, resource, transformerChain);
			}
			if (logger.isTraceEnabled()) {
				if (newLink != null && !link.equals(newLink)) {
					logger.trace("Link modified: " + newLink + " (original: " + link + ")");
				}
				else {
					logger.trace("Link not modified: " + link);
				}
			}
			writer.write(newLink != null ? newLink : link);
			index = info.getEnd();
		}
		writer.write(content.substring(index));

		return new TransformedResource(resource, writer.toString().getBytes(DEFAULT_CHARSET));
	}

	private boolean hasScheme(String link) {
		int schemeIndex = link.indexOf(":");
		return (schemeIndex > 0 && !link.substring(0, schemeIndex).contains("/"))
				|| link.indexOf("//") == 0;
	}


	@FunctionalInterface
	protected interface CssLinkParser {

		void parseLink(String content, Set<CssLinkInfo> linkInfos);

	}

	protected static abstract class AbstractCssLinkParser implements CssLinkParser {

		/**
		 * Return the keyword to use to search for links.
		 */
		protected abstract String getKeyword();

		@Override
		public void parseLink(String content, Set<CssLinkInfo> linkInfos) {
			int index = 0;
			do {
				index = content.indexOf(getKeyword(), index);
				if (index == -1) {
					break;
				}
				index = skipWhitespace(content, index + getKeyword().length());
				if (content.charAt(index) == '\'') {
					index = addLink(index, "'", content, linkInfos);
				}
				else if (content.charAt(index) == '"') {
					index = addLink(index, "\"", content, linkInfos);
				}
				else {
					index = extractLink(index, content, linkInfos);

				}
			}
			while (true);
		}

		private int skipWhitespace(String content, int index) {
			while (true) {
				if (Character.isWhitespace(content.charAt(index))) {
					index++;
					continue;
				}
				return index;
			}
		}

		protected int addLink(int index, String endKey, String content, Set<CssLinkInfo> linkInfos) {
			int start = index + 1;
			int end = content.indexOf(endKey, start);
			linkInfos.add(new CssLinkInfo(start, end));
			return end + endKey.length();
		}

		/**
		 * Invoked after a keyword match, after whitespaces removed, and when
		 * the next char is neither a single nor double quote.
		 */
		protected abstract int extractLink(int index, String content, Set<CssLinkInfo> linkInfos);

	}

	private static class ImportStatementCssLinkParser extends AbstractCssLinkParser {

		@Override
		protected String getKeyword() {
			return "@import";
		}

		@Override
		protected int extractLink(int index, String content, Set<CssLinkInfo> linkInfos) {
			if (content.substring(index, index + 4).equals("url(")) {
				// Ignore, UrlLinkParser will take care
			}
			else if (logger.isErrorEnabled()) {
				logger.error("Unexpected syntax for @import link at index " + index);
			}
			return index;
		}
	}

	private static class UrlFunctionCssLinkParser extends AbstractCssLinkParser {

		@Override
		protected String getKeyword() {
			return "url(";
		}

		@Override
		protected int extractLink(int index, String content, Set<CssLinkInfo> linkInfos) {
			// A url() function without unquoted
			return addLink(index - 1, ")", content, linkInfos);
		}
	}


	private static class CssLinkInfo implements Comparable<CssLinkInfo> {

		private final int start;

		private final int end;


		private CssLinkInfo(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public int getStart() {
			return this.start;
		}

		public int getEnd() {
			return this.end;
		}

		@Override
		public int compareTo(CssLinkInfo other) {
			return (this.start < other.start ? -1 : (this.start == other.start ? 0 : 1));
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj != null && obj instanceof CssLinkInfo) {
				CssLinkInfo other = (CssLinkInfo) obj;
				return (this.start == other.start && this.end == other.end);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.start * 31 + this.end;
		}
	}

}
