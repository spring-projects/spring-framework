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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@link ResourceTransformer} implementation that helps handling resources
 * within HTML5 AppCache manifests for HTML5 offline applications.
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
 * All files that have the ".appcache" file extension, or the extension given
 * in the constructor, will be transformed by this class.
 *
 * <p>This hash is computed using the content of the appcache manifest and the
 * content of the linked resources; so changing a resource linked in the manifest
 * or the manifest itself should invalidate the browser cache.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 * @see <a href="https://html.spec.whatwg.org/multipage/browsers.html#offline">HTML5 offline applications spec</a>
 */
public class AppCacheManifestTransformer extends ResourceTransformerSupport {

	private static final String MANIFEST_HEADER = "CACHE MANIFEST";

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final Log logger = LogFactory.getLog(AppCacheManifestTransformer.class);


	private final Map<String, SectionTransformer> sectionTransformers = new HashMap<>();

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

		SectionTransformer noOpSection = new NoOpSection();
		this.sectionTransformers.put(MANIFEST_HEADER, noOpSection);
		this.sectionTransformers.put("NETWORK:", noOpSection);
		this.sectionTransformers.put("FALLBACK:", noOpSection);
		this.sectionTransformers.put("CACHE:", new CacheSection());
	}


	@Override
	public Resource transform(ServerWebExchange exchange, Resource resource,
			ResourceTransformerChain transformerChain) throws IOException {

		resource = transformerChain.transform(exchange, resource);
		if (!this.fileExtension.equals(StringUtils.getFilenameExtension(resource.getFilename()))) {
			return resource;
		}

		byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
		String content = new String(bytes, DEFAULT_CHARSET);

		if (!content.startsWith(MANIFEST_HEADER)) {
			if (logger.isTraceEnabled()) {
				logger.trace("AppCache manifest does not start with 'CACHE MANIFEST', skipping: " + resource);
			}
			return resource;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Transforming resource: " + resource);
		}

		StringWriter contentWriter = new StringWriter();
		HashBuilder hashBuilder = new HashBuilder(content.length());

		Scanner scanner = new Scanner(content);
		SectionTransformer currentTransformer = this.sectionTransformers.get(MANIFEST_HEADER);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (this.sectionTransformers.containsKey(line.trim())) {
				currentTransformer = this.sectionTransformers.get(line.trim());
				contentWriter.write(line + "\n");
				hashBuilder.appendString(line);
			}
			else {
				contentWriter.write(
						currentTransformer.transform(
								line, hashBuilder, resource, transformerChain, exchange)  + "\n");
			}
		}

		String hash = hashBuilder.build();
		contentWriter.write("\n" + "# Hash: " + hash);
		if (logger.isTraceEnabled()) {
			logger.trace("AppCache file: [" + resource.getFilename()+ "] hash: [" + hash + "]");
		}

		return new TransformedResource(resource, contentWriter.toString().getBytes(DEFAULT_CHARSET));
	}


	@FunctionalInterface
	private interface SectionTransformer {

		/**
		 * Transforms a line in a section of the manifest.
		 * <p>The actual transformation depends on the chosen transformation strategy
		 * for the current manifest section (CACHE, NETWORK, FALLBACK, etc).
		 */
		String transform(String line, HashBuilder builder, Resource resource,
				ResourceTransformerChain transformerChain, ServerWebExchange exchange) throws IOException;
	}


	private static class NoOpSection implements SectionTransformer {

		public String transform(String line, HashBuilder builder, Resource resource,
				ResourceTransformerChain transformerChain, ServerWebExchange exchange) throws IOException {

			builder.appendString(line);
			return line;
		}
	}


	private class CacheSection implements SectionTransformer {

		private static final String COMMENT_DIRECTIVE = "#";

		@Override
		public String transform(String line, HashBuilder builder, Resource resource,
				ResourceTransformerChain transformerChain, ServerWebExchange exchange) throws IOException {

			if (isLink(line) && !hasScheme(line)) {
				ResourceResolverChain resolverChain = transformerChain.getResolverChain();
				Resource appCacheResource =
						resolverChain.resolveResource(null, line, Collections.singletonList(resource));
				String path = resolveUrlPath(line, exchange, resource, transformerChain);
				builder.appendResource(appCacheResource);
				if (logger.isTraceEnabled()) {
					logger.trace("Link modified: " + path + " (original: " + line + ")");
				}
				return path;
			}
			builder.appendString(line);
			return line;
		}

		private boolean hasScheme(String link) {
			int schemeIndex = link.indexOf(":");
			return (link.startsWith("//") || (schemeIndex > 0 && !link.substring(0, schemeIndex).contains("/")));
		}

		private boolean isLink(String line) {
			return (StringUtils.hasText(line) && !line.startsWith(COMMENT_DIRECTIVE));
		}
	}


	private static class HashBuilder {

		private final ByteArrayOutputStream baos;

		public HashBuilder(int initialSize) {
			this.baos = new ByteArrayOutputStream(initialSize);
		}

		public void appendResource(Resource resource) throws IOException {
			byte[] content = FileCopyUtils.copyToByteArray(resource.getInputStream());
			this.baos.write(DigestUtils.md5Digest(content));
		}

		public void appendString(String content) throws IOException {
			this.baos.write(content.getBytes(DEFAULT_CHARSET));
		}

		public String build() {
			return DigestUtils.md5DigestAsHex(this.baos.toByteArray());
		}
	}

}
