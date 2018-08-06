/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.StringWriter;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.StreamUtils;
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


	@SuppressWarnings("deprecation")
	@Override
	public Mono<Resource> transform(ServerWebExchange exchange, Resource inputResource,
			ResourceTransformerChain transformerChain) {

		return transformerChain.transform(exchange, inputResource)
				.flatMap(ouptputResource -> {
					String filename = ouptputResource.getFilename();
					if (!"css".equals(StringUtils.getFilenameExtension(filename)) ||
							inputResource instanceof EncodedResourceResolver.EncodedResource ||
							inputResource instanceof GzipResourceResolver.GzippedResource) {
						return Mono.just(ouptputResource);
					}

					DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
					Flux<DataBuffer> flux = DataBufferUtils
							.read(ouptputResource, bufferFactory, StreamUtils.BUFFER_SIZE);
					return DataBufferUtils.join(flux)
							.flatMap(dataBuffer -> {
								CharBuffer charBuffer = DEFAULT_CHARSET.decode(dataBuffer.asByteBuffer());
								DataBufferUtils.release(dataBuffer);
								String cssContent = charBuffer.toString();
								return transformContent(cssContent, ouptputResource, transformerChain, exchange);
							});
				});
	}

	private Mono<? extends Resource> transformContent(String cssContent, Resource resource,
			ResourceTransformerChain chain, ServerWebExchange exchange) {

		List<ContentChunkInfo> contentChunkInfos = parseContent(cssContent);
		if (contentChunkInfos.isEmpty()) {
			return Mono.just(resource);
		}

		return Flux.fromIterable(contentChunkInfos)
				.concatMap(contentChunkInfo -> {
					String contentChunk = contentChunkInfo.getContent(cssContent);
					if (contentChunkInfo.isLink() && !hasScheme(contentChunk)) {
						String link = toAbsolutePath(contentChunk, exchange);
						return resolveUrlPath(link, exchange, resource, chain).defaultIfEmpty(contentChunk);
					}
					else {
						return Mono.just(contentChunk);
					}
				})
				.reduce(new StringWriter(), (writer, chunk) -> {
					writer.write(chunk);
					return writer;
				})
				.map(writer -> {
					byte[] newContent = writer.toString().getBytes(DEFAULT_CHARSET);
					return new TransformedResource(resource, newContent);
				});
	}

	private List<ContentChunkInfo> parseContent(String cssContent) {
		SortedSet<ContentChunkInfo> links = new TreeSet<>();
		this.linkParsers.forEach(parser -> parser.parse(cssContent, links));
		if (links.isEmpty()) {
			return Collections.emptyList();
		}
		int index = 0;
		List<ContentChunkInfo> result = new ArrayList<>();
		for (ContentChunkInfo link : links) {
			result.add(new ContentChunkInfo(index, link.getStart(), false));
			result.add(link);
			index = link.getEnd();
		}
		if (index < cssContent.length()) {
			result.add(new ContentChunkInfo(index, cssContent.length(), false));
		}
		return result;
	}

	private boolean hasScheme(String link) {
		int schemeIndex = link.indexOf(':');
		return (schemeIndex > 0 && !link.substring(0, schemeIndex).contains("/")) || link.indexOf("//") == 0;
	}


	/**
	 * Extract content chunks that represent links.
	 */
	@FunctionalInterface
	protected interface LinkParser {

		void parse(String cssContent, SortedSet<ContentChunkInfo> result);

	}


	/**
	 * Abstract base class for {@link LinkParser} implementations.
	 */
	protected abstract static class AbstractLinkParser implements LinkParser {

		/** Return the keyword to use to search for links, e.g. "@import", "url(" */
		protected abstract String getKeyword();

		@Override
		public void parse(String content, SortedSet<ContentChunkInfo> result) {
			int position = 0;
			while (true) {
				position = content.indexOf(getKeyword(), position);
				if (position == -1) {
					return;
				}
				position += getKeyword().length();
				while (Character.isWhitespace(content.charAt(position))) {
					position++;
				}
				if (content.charAt(position) == '\'') {
					position = extractLink(position, '\'', content, result);
				}
				else if (content.charAt(position) == '"') {
					position = extractLink(position, '"', content, result);
				}
				else {
					position = extractUnquotedLink(position, content, result);

				}
			}
		}

		protected int extractLink(int index, char endChar, String content, Set<ContentChunkInfo> result) {
			int start = index + 1;
			int end = content.indexOf(endChar, start);
			result.add(new ContentChunkInfo(start, end, true));
			return end + 1;
		}

		/**
		 * Invoked after a keyword match, after whitespaces removed, and when
		 * the next char is neither a single nor double quote.
		 */
		protected abstract int extractUnquotedLink(int position, String content,
				Set<ContentChunkInfo> linksToAdd);

	}


	private static class ImportLinkParser extends AbstractLinkParser {

		@Override
		protected String getKeyword() {
			return "@import";
		}

		@Override
		protected int extractUnquotedLink(int position, String content, Set<ContentChunkInfo> result) {
			if (content.substring(position, position + 4).equals("url(")) {
				// Ignore, UrlFunctionContentParser will take care
			}
			else if (logger.isTraceEnabled()) {
				logger.trace("Unexpected syntax for @import link at index " + position);
			}
			return position;
		}
	}


	private static class UrlFunctionLinkParser extends AbstractLinkParser {

		@Override
		protected String getKeyword() {
			return "url(";
		}

		@Override
		protected int extractUnquotedLink(int position, String content, Set<ContentChunkInfo> result) {
			// A url() function without unquoted
			return extractLink(position - 1, ')', content, result);
		}
	}


	private static class ContentChunkInfo implements Comparable<ContentChunkInfo> {

		private final int start;

		private final int end;

		private final boolean isLink;


		ContentChunkInfo(int start, int end, boolean isLink) {
			this.start = start;
			this.end = end;
			this.isLink = isLink;
		}


		public int getStart() {
			return this.start;
		}

		public int getEnd() {
			return this.end;
		}

		public boolean isLink() {
			return this.isLink;
		}

		public String getContent(String fullContent) {
			return fullContent.substring(this.start, this.end);
		}

		@Override
		public int compareTo(ContentChunkInfo other) {
			return Integer.compare(this.start, other.start);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ContentChunkInfo)) {
				return false;
			}
			ContentChunkInfo otherCci = (ContentChunkInfo) other;
			return (this.start == otherCci.start && this.end == otherCci.end);
		}

		@Override
		public int hashCode() {
			return this.start * 31 + this.end;
		}
	}

}
