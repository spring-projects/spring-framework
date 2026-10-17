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

package org.springframework.web.servlet.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

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
 * @since 4.1
 */
public class CssLinkResourceTransformer extends ResourceTransformerSupport {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


	@SuppressWarnings("deprecation")
	@Override
	public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain)
			throws IOException {

		resource = transformerChain.transform(request, resource);

		String filename = resource.getFilename();
		if (!"css".equals(StringUtils.getFilenameExtension(filename)) ||
				resource instanceof EncodedResourceResolver.EncodedResource) {
			return resource;
		}

		CssLinkParser parser = new CssLinkParser();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[StreamUtils.BUFFER_SIZE];
		try (InputStream in = resource.getInputStream()) {
			int read;
			while ((read = in.read(buffer)) != -1) {
				writeTokens(parser.feed(buffer, 0, read), request, resource, transformerChain, output);
			}
		}
		writeTokens(parser.end(), request, resource, transformerChain, output);

		if (!parser.hasLinks()) {
			return resource;
		}
		return new TransformedResource(resource, output.toByteArray());
	}

	private void writeTokens(List<CssLinkParser.Token> tokens, HttpServletRequest request, Resource resource,
			ResourceTransformerChain transformerChain, ByteArrayOutputStream output) {

		for (CssLinkParser.Token token : tokens) {
			byte[] bytes = (token.link() ? resolveLink(token.bytes(), request, resource, transformerChain) : token.bytes());
			output.write(bytes, 0, bytes.length);
		}
	}

	private byte[] resolveLink(byte[] linkBytes, HttpServletRequest request, Resource resource,
			ResourceTransformerChain transformerChain) {

		String link = new String(linkBytes, DEFAULT_CHARSET);
		String newLink = null;
		if (!hasScheme(link)) {
			String absolutePath = toAbsolutePath(link, request);
			newLink = resolveUrlPath(absolutePath, request, resource, transformerChain);
		}
		return (newLink != null ? newLink.getBytes(DEFAULT_CHARSET) : linkBytes);
	}

	private boolean hasScheme(String link) {
		int schemeIndex = link.indexOf(':');
		return ((schemeIndex > 0 && !link.substring(0, schemeIndex).contains("/")) || link.indexOf("//") == 0);
	}

}
