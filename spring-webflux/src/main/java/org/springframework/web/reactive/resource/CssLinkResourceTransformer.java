/*
 * Copyright 2002-present the original author or authors.
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
 * file to match the public URL paths that should be exposed to clients (for example,
 * with an MD5 content-based hash inserted in the URL).
 *
 * <p>The implementation looks for links in CSS {@code @import} statements and
 * also inside CSS {@code url()} functions. All links are then passed through the
 * {@link ResourceResolverChain} and resolved relative to the location of the
 * containing CSS file. If successfully resolved, the link is modified, otherwise
 * the original link is preserved.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class CssLinkResourceTransformer extends ResourceTransformerSupport {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


	@Override
	@SuppressWarnings("deprecation")
	public Mono<Resource> transform(ServerWebExchange exchange, Resource inputResource,
			ResourceTransformerChain transformerChain) {

		return transformerChain.transform(exchange, inputResource)
				.flatMap(outputResource -> {
					String filename = outputResource.getFilename();
					if (!"css".equals(StringUtils.getFilenameExtension(filename)) ||
							inputResource instanceof EncodedResourceResolver.EncodedResource) {
						return Mono.just(outputResource);
					}

					DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
					return transformContent(outputResource, bufferFactory, transformerChain, exchange);
				});
	}

	private Mono<? extends Resource> transformContent(Resource resource, DataBufferFactory bufferFactory,
			ResourceTransformerChain chain, ServerWebExchange exchange) {

		// Parsing state is created per subscription
		return Mono.defer(() -> {
			CssLinkParser parser = new CssLinkParser();
			Flux<DataBuffer> flux = DataBufferUtils.read(resource, bufferFactory, StreamUtils.BUFFER_SIZE);
			return flux
					.concatMap(buffer -> Flux.fromIterable(feedAndRelease(parser, buffer)))
					.concatWith(Flux.defer(() -> Flux.fromIterable(parser.end())))
					.concatMap(token -> resolveToken(token, resource, chain, exchange))
					.reduceWith(ByteArrayOutputStream::new, (out, bytes) -> {
						out.write(bytes, 0, bytes.length);
						return out;
					})
					.map(out -> parser.hasLinks() ? new TransformedResource(resource, out.toByteArray()) : resource);
		});
	}

	private List<CssLinkParser.Token> feedAndRelease(CssLinkParser parser, DataBuffer buffer) {
		try {
			return parser.feed(buffer);
		}
		finally {
			DataBufferUtils.release(buffer);
		}
	}

	private Mono<byte[]> resolveToken(CssLinkParser.Token token, Resource resource,
			ResourceTransformerChain chain, ServerWebExchange exchange) {

		if (!token.link()) {
			return Mono.just(token.bytes());
		}
		String link = new String(token.bytes(), DEFAULT_CHARSET);
		if (hasScheme(link)) {
			return Mono.just(token.bytes());
		}
		String absolutePath = toAbsolutePath(link, exchange);
		return resolveUrlPath(absolutePath, exchange, resource, chain)
				.defaultIfEmpty(link)
				.map(resolved -> resolved.getBytes(DEFAULT_CHARSET));
	}

	private boolean hasScheme(String link) {
		int schemeIndex = link.indexOf(':');
		return (schemeIndex > 0 && !link.substring(0, schemeIndex).contains("/")) || link.indexOf("//") == 0;
	}

}
