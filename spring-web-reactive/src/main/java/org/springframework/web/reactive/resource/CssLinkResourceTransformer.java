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

package org.springframework.web.reactive.resource;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

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
 * @since 5.0
 */
public class CssLinkResourceTransformer extends ResourceTransformerSupport {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final Log logger = LogFactory.getLog(CssLinkResourceTransformer.class);

	private final List<LinkParser> linkParsers = new ArrayList<>(2);


	public CssLinkResourceTransformer() {
		this.linkParsers.add(new ImportLinkParser());
		this.linkParsers.add(new UrlFunctionLinkParser());
	}


	@Override
	public Mono<Resource> transform(ServerWebExchange exchange, Resource resource,
			ResourceTransformerChain transformerChain) {

		return transformerChain.transform(exchange, resource)
				.then(newResource -> {
					String filename = newResource.getFilename();
					if (!"css".equals(StringUtils.getFilenameExtension(filename))) {
						return Mono.just(newResource);
					}

					if (logger.isTraceEnabled()) {
						logger.trace("Transforming resource: " + newResource);
					}

					byte[] bytes = new byte[0];
					try {
						bytes = FileCopyUtils.copyToByteArray(newResource.getInputStream());
					}
					catch (IOException ex) {
						return Mono.error(Exceptions.propagate(ex));
					}
					String fullContent = new String(bytes, DEFAULT_CHARSET);
					List<Segment> segments = parseContent(fullContent);

					if (segments.isEmpty()) {
						if (logger.isTraceEnabled()) {
							logger.trace("No links found.");
						}
						return Mono.just(newResource);
					}

					return Flux.fromIterable(segments)
							.concatMap(segment -> {
								String segmentContent = segment.getContent(fullContent);
								if (segment.isLink() && !hasScheme(segmentContent)) {
									String link = toAbsolutePath(segmentContent, exchange.getRequest());
									return resolveUrlPath(link, exchange, newResource, transformerChain)
											.defaultIfEmpty(segmentContent);
								}
								else {
									return Mono.just(segmentContent);
								}
							})
							.reduce(new StringWriter(), (writer, chunk) -> {
								writer.write(chunk);
								return writer;
							})
							.then(writer -> {
								byte[] newContent = writer.toString().getBytes(DEFAULT_CHARSET);
								return Mono.just(new TransformedResource(resource, newContent));
							});
				});
	}

	private List<Segment> parseContent(String fullContent) {

		List<Segment> links = new ArrayList<>();
		for (LinkParser parser : this.linkParsers) {
			links.addAll(parser.parseLinks(fullContent));
		}

		if (links.isEmpty()) {
			return Collections.emptyList();
		}

		Collections.sort(links);

		int index = 0;
		List<Segment> allSegments = new ArrayList<>(links);
		for (Segment link : links) {
			allSegments.add(new Segment(index, link.getStart(), false));
			index = link.getEnd();
		}
		if (index < fullContent.length()) {
			allSegments.add(new Segment(index, fullContent.length(), false));
		}

		Collections.sort(allSegments);

		return allSegments;
	}

	private boolean hasScheme(String link) {
		int schemeIndex = link.indexOf(":");
		return (schemeIndex > 0 && !link.substring(0, schemeIndex).contains("/")) || link.indexOf("//") == 0;
	}


	@FunctionalInterface
	protected interface LinkParser {

		Set<Segment> parseLinks(String fullContent);

	}


	protected static abstract class AbstractLinkParser implements LinkParser {

		/** Return the keyword to use to search for links. */
		protected abstract String getKeyword();

		@Override
		public Set<Segment> parseLinks(String fullContent) {
			Set<Segment> linksToAdd = new HashSet<>(8);
			int index = 0;
			do {
				index = fullContent.indexOf(getKeyword(), index);
				if (index == -1) {
					break;
				}
				index = skipWhitespace(fullContent, index + getKeyword().length());
				if (fullContent.charAt(index) == '\'') {
					index = addLink(index, "'", fullContent, linksToAdd);
				}
				else if (fullContent.charAt(index) == '"') {
					index = addLink(index, "\"", fullContent, linksToAdd);
				}
				else {
					index = extractLink(index, fullContent, linksToAdd);

				}
			}
			while (true);
			return linksToAdd;
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

		protected int addLink(int index, String endKey, String content, Set<Segment> linksToAdd) {
			int start = index + 1;
			int end = content.indexOf(endKey, start);
			linksToAdd.add(new Segment(start, end, true));
			return end + endKey.length();
		}

		/**
		 * Invoked after a keyword match, after whitespaces removed, and when
		 * the next char is neither a single nor double quote.
		 */
		protected abstract int extractLink(int index, String content, Set<Segment> linksToAdd);

	}


	private static class ImportLinkParser extends AbstractLinkParser {

		@Override
		protected String getKeyword() {
			return "@import";
		}

		@Override
		protected int extractLink(int index, String content, Set<Segment> linksToAdd) {
			if (content.substring(index, index + 4).equals("url(")) {
				// Ignore, UrlLinkParser will take care
			}
			else if (logger.isErrorEnabled()) {
				logger.error("Unexpected syntax for @import link at index " + index);
			}
			return index;
		}
	}


	private static class UrlFunctionLinkParser extends AbstractLinkParser {

		@Override
		protected String getKeyword() {
			return "url(";
		}

		@Override
		protected int extractLink(int index, String content, Set<Segment> linksToAdd) {
			// A url() function without unquoted
			return addLink(index - 1, ")", content, linksToAdd);
		}
	}


	private static class Segment implements Comparable<Segment> {

		private final int start;

		private final int end;

		private final boolean link;


		public Segment(int start, int end, boolean isLink) {
			this.start = start;
			this.end = end;
			this.link = isLink;
		}


		public int getStart() {
			return this.start;
		}

		public int getEnd() {
			return this.end;
		}

		public boolean isLink() {
			return this.link;
		}

		public String getContent(String fullContent) {
			return fullContent.substring(this.start, this.end);
		}

		@Override
		public int compareTo(Segment other) {
			return (this.start < other.start ? -1 : (this.start == other.start ? 0 : 1));
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj != null && obj instanceof Segment) {
				Segment other = (Segment) obj;
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
