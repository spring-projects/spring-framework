/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.reactive.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.DigestUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@link ResourceTransformer} HTML5 AppCache manifests.
 *
 * <p>This transformer:
 * <ul>
 * <li>modifies links to match the public URL paths that should be exposed to
 * clients, using configured {@code ResourceResolver} strategies
 * <li>appends a comment in the manifest, containing a Hash
 * (e.g. "# Hash: 9de0f09ed7caf84e885f1f0f11c7e326"), thus changing the content
 * of the manifest in order to trigger an appcache reload in the browser.
 * </ul>
 *
 * <p>All files with an ".appcache" file extension (or the extension given
 * to the constructor) will be transformed by this class. The hash is computed
 * using the content of the appcache manifest so that changes in the manifest
 * should invalidate the browser cache. This should also work with changes in
 * referenced resources whose links are also versioned.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 * @see <a href="https://html.spec.whatwg.org/multipage/browsers.html#offline">HTML5 offline applications spec</a>
 * @deprecated as of 5.3 since browser support is going away
 */
@Deprecated
public class AppCacheManifestTransformer extends ResourceTransformerSupport {

	private static final String MANIFEST_HEADER = "CACHE MANIFEST";

	private static final String CACHE_HEADER = "CACHE:";

	private static final Collection<String> MANIFEST_SECTION_HEADERS =
			Arrays.asList(MANIFEST_HEADER, "NETWORK:", "FALLBACK:", CACHE_HEADER);

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final Log logger = LogFactory.getLog(AppCacheManifestTransformer.class);


	private final String fileExtension;


	/**
	 * Create an AppCacheResourceTransformer that transforms files with extension ".appcache".
	 */
	public AppCacheManifestTransformer() {
		this("appcache");
	}

	/**
	 * Create an AppCacheResourceTransformer that transforms files with the extension
	 * given as a parameter.
	 */
	public AppCacheManifestTransformer(String fileExtension) {
		this.fileExtension = fileExtension;
	}


	@Override
	public Mono<Resource> transform(ServerWebExchange exchange, Resource inputResource,
			ResourceTransformerChain chain) {

		return chain.transform(exchange, inputResource)
				.flatMap(outputResource -> {
					String name = outputResource.getFilename();
					if (!this.fileExtension.equals(StringUtils.getFilenameExtension(name))) {
						return Mono.just(outputResource);
					}
					DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
					Flux<DataBuffer> flux = DataBufferUtils
							.read(outputResource, bufferFactory, StreamUtils.BUFFER_SIZE);
					return DataBufferUtils.join(flux)
							.flatMap(dataBuffer -> {
								CharBuffer charBuffer = DEFAULT_CHARSET.decode(dataBuffer.asByteBuffer());
								DataBufferUtils.release(dataBuffer);
								String content = charBuffer.toString();
								return transform(content, outputResource, chain, exchange);
							});
				});
	}

	private Mono<? extends Resource> transform(String content, Resource resource,
			ResourceTransformerChain chain, ServerWebExchange exchange) {

		if (!content.startsWith(MANIFEST_HEADER)) {
			if (logger.isTraceEnabled()) {
				logger.trace(exchange.getLogPrefix() +
						"Skipping " + resource + ": Manifest does not start with 'CACHE MANIFEST'");
			}
			return Mono.just(resource);
		}
		return Flux.generate(new LineInfoGenerator(content))
				.concatMap(info -> processLine(info, exchange, resource, chain))
				.reduce(new ByteArrayOutputStream(), (out, line) -> {
					writeToByteArrayOutputStream(out, line + "\n");
					return out;
				})
				.map(out -> {
					String hash = DigestUtils.md5DigestAsHex(out.toByteArray());
					writeToByteArrayOutputStream(out, "\n" + "# Hash: " + hash);
					return new TransformedResource(resource, out.toByteArray());
				});
	}

	private static void writeToByteArrayOutputStream(ByteArrayOutputStream out, String toWrite) {
		try {
			byte[] bytes = toWrite.getBytes(DEFAULT_CHARSET);
			out.write(bytes);
		}
		catch (IOException ex) {
			throw Exceptions.propagate(ex);
		}
	}

	private Mono<String> processLine(LineInfo info, ServerWebExchange exchange,
			Resource resource, ResourceTransformerChain chain) {

		if (!info.isLink()) {
			return Mono.just(info.getLine());
		}

		String link = toAbsolutePath(info.getLine(), exchange);
		return resolveUrlPath(link, exchange, resource, chain);
	}


	private static class LineInfoGenerator implements Consumer<SynchronousSink<LineInfo>> {

		private final Scanner scanner;

		@Nullable
		private LineInfo previous;


		LineInfoGenerator(String content) {
			this.scanner = new Scanner(content);
		}


		@Override
		public void accept(SynchronousSink<LineInfo> sink) {
			if (this.scanner.hasNext()) {
				String line = this.scanner.nextLine();
				LineInfo current = new LineInfo(line, this.previous);
				sink.next(current);
				this.previous = current;
			}
			else {
				sink.complete();
			}
		}
	}


	private static class LineInfo {

		private final String line;

		private final boolean cacheSection;

		private final boolean link;


		LineInfo(String line, @Nullable LineInfo previousLine) {
			this.line = line;
			this.cacheSection = initCacheSectionFlag(line, previousLine);
			this.link = iniLinkFlag(line, this.cacheSection);
		}


		private static boolean initCacheSectionFlag(String line, @Nullable LineInfo previousLine) {
			String trimmedLine = line.trim();
			if (MANIFEST_SECTION_HEADERS.contains(trimmedLine)) {
				return trimmedLine.equals(CACHE_HEADER);
			}
			else if (previousLine != null) {
				return previousLine.isCacheSection();
			}
			throw new IllegalStateException(
					"Manifest does not start with " + MANIFEST_HEADER + ": " + line);
		}

		private static boolean iniLinkFlag(String line, boolean isCacheSection) {
			return (isCacheSection && StringUtils.hasText(line) && !line.startsWith("#")
					&& !line.startsWith("//") && !hasScheme(line));
		}

		private static boolean hasScheme(String line) {
			int index = line.indexOf(':');
			return (line.startsWith("//") || (index > 0 && !line.substring(0, index).contains("/")));
		}


		public String getLine() {
			return this.line;
		}

		public boolean isCacheSection() {
			return this.cacheSection;
		}

		public boolean isLink() {
			return this.link;
		}
	}

}
