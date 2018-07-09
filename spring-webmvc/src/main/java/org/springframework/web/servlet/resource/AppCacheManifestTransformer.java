/*
 * Copyright 2002-2017 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Scanner;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link ResourceTransformer} implementation that helps handling resources
 * within HTML5 AppCache manifests for HTML5 offline applications.
 *
 * <p>This transformer:
 * <ul>
 * <li>modifies links to match the public URL paths that should be exposed to clients,
 * using configured {@code ResourceResolver} strategies
 * <li>appends a comment in the manifest, containing a Hash (e.g. "# Hash: 9de0f09ed7caf84e885f1f0f11c7e326"),
 * thus changing the content of the manifest in order to trigger an appcache reload in the browser.
 * </ul>
 *
 * <p>All files that have the ".appcache" file extension, or the extension given in the constructor,
 * will be transformed by this class. This hash is computed using the content of the appcache manifest
 * and the content of the linked resources; so changing a resource linked in the manifest
 * or the manifest itself should invalidate the browser cache.
 *
 * <p>In order to serve manifest files with the proper {@code "text/manifest"} content type,
 * it is required to configure it with
 * {@code contentNegotiationConfigurer.mediaType("appcache", MediaType.valueOf("text/manifest")}
 * in a {@code WebMvcConfigurer}.
 *
 * @author Brian Clozel
 * @since 4.1
 * @see <a href="https://html.spec.whatwg.org/multipage/browsers.html#offline">HTML5 offline applications spec</a>
 */
public class AppCacheManifestTransformer extends ResourceTransformerSupport {

	private static final Collection<String> MANIFEST_SECTION_HEADERS =
			Arrays.asList("CACHE MANIFEST", "NETWORK:", "FALLBACK:", "CACHE:");

	private static final String MANIFEST_HEADER = "CACHE MANIFEST";

	private static final String CACHE_HEADER = "CACHE:";

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
	public Resource transform(HttpServletRequest request, Resource resource,
			ResourceTransformerChain chain) throws IOException {

		resource = chain.transform(request, resource);
		if (!this.fileExtension.equals(StringUtils.getFilenameExtension(resource.getFilename()))) {
			return resource;
		}

		byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
		String content = new String(bytes, DEFAULT_CHARSET);

		if (!content.startsWith(MANIFEST_HEADER)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Skipping " + resource + ": Manifest does not start with 'CACHE MANIFEST'");
			}
			return resource;
		}

		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(content);
		LineInfo previous = null;
		LineAggregator aggregator = new LineAggregator(resource, content);

		while (scanner.hasNext()) {
			String line = scanner.nextLine();
			LineInfo current = new LineInfo(line, previous);
			LineOutput lineOutput = processLine(current, request, resource, chain);
			aggregator.add(lineOutput);
			previous = current;
		}

		return aggregator.createResource();
	}

	private static byte[] getResourceBytes(Resource resource) throws IOException {
		return FileCopyUtils.copyToByteArray(resource.getInputStream());
	}

	private LineOutput processLine(LineInfo info, HttpServletRequest request,
			Resource resource, ResourceTransformerChain transformerChain) {

		if (!info.isLink()) {
			return new LineOutput(info.getLine(), null);
		}

		Resource appCacheResource = transformerChain.getResolverChain()
				.resolveResource(null, info.getLine(), Collections.singletonList(resource));

		String path = info.getLine();
		String absolutePath = toAbsolutePath(path, request);
		String newPath = resolveUrlPath(absolutePath, request, resource, transformerChain);

		return new LineOutput((newPath != null ? newPath : path), appCacheResource);
	}


	private static class LineInfo {

		private final String line;

		private final boolean cacheSection;

		private final boolean link;

		public LineInfo(String line, @Nullable LineInfo previous) {
			this.line = line;
			this.cacheSection = initCacheSectionFlag(line, previous);
			this.link = iniLinkFlag(line, this.cacheSection);
		}

		private static boolean initCacheSectionFlag(String line, @Nullable LineInfo previousLine) {
			if (MANIFEST_SECTION_HEADERS.contains(line.trim())) {
				return line.trim().equals(CACHE_HEADER);
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


	private static class LineOutput {

		private final String line;

		@Nullable
		private final Resource resource;

		public LineOutput(String line, @Nullable Resource resource) {
			this.line = line;
			this.resource = resource;
		}

		public String getLine() {
			return this.line;
		}

		@Nullable
		public Resource getResource() {
			return this.resource;
		}
	}


	private static class LineAggregator {

		private final StringWriter writer = new StringWriter();

		private final ByteArrayOutputStream baos;

		private final Resource resource;

		public LineAggregator(Resource resource, String content) {
			this.resource = resource;
			this.baos = new ByteArrayOutputStream(content.length());
		}

		public void add(LineOutput lineOutput) throws IOException {
			this.writer.write(lineOutput.getLine() + "\n");
			byte[] bytes = (lineOutput.getResource() != null ?
					DigestUtils.md5Digest(getResourceBytes(lineOutput.getResource())) :
					lineOutput.getLine().getBytes(DEFAULT_CHARSET));
			this.baos.write(bytes);
		}

		public TransformedResource createResource() {
			String hash = DigestUtils.md5DigestAsHex(this.baos.toByteArray());
			this.writer.write("\n" + "# Hash: " + hash);
			byte[] bytes = this.writer.toString().getBytes(DEFAULT_CHARSET);
			return new TransformedResource(this.resource, bytes);
		}
	}

}
