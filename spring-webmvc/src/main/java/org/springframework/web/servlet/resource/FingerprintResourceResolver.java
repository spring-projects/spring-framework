/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.resource;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * A {@code ResourceResolver} that resolves request paths containing an additional
 * MD5 hash in the file name.
 *
 * <p>For example the path "styles/foo-e36d2e05253c6c7085a91522ce43a0b4.css" will
 * match to "styles/foo.css" assuming the hash computed from the content of
 * "foo.css" matches the hash in the path.
 *
 * <p>The resolver first delegates to the chain so that if
 * "foo-e36d2e05253c6c7085a91522ce43a0b4.css" has been written to disk (e.g. at
 * build time) it is simply found. Or if the chain does not find an existing
 * resource, this resolver removes the hash, attempts to find a matching resource
 * with the resulting file name ("foo.css") and then compares the hash from the
 * request path to the hash computed from the file content.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 */
public class FingerprintResourceResolver implements ResourceResolver {

	private static final Log logger = LogFactory.getLog(FingerprintResourceResolver.class);

	private static final Pattern pattern = Pattern.compile("-(\\S*)\\.");


	@Override
	public Resource resolveResource(HttpServletRequest request, String requestPath, List<? extends Resource> locations,
			ResourceResolverChain chain) {

		Resource resolved = chain.resolveResource(request, requestPath, locations);
		if (resolved != null) {
			return resolved;
		}

		String hash = extractHash(requestPath);
		if (StringUtils.isEmpty(hash)) {
			return null;
		}

		String simplePath = StringUtils.delete(requestPath, "-" + hash);
		Resource baseResource = chain.resolveResource(request, simplePath, locations);
		if (baseResource == null) {
			return null;
		}

		String candidateHash = calculateHash(baseResource);
		if (candidateHash.equals(hash)) {
			return baseResource;
		}
		else {
			logger.debug("Potential resource found for [" + requestPath + "], but fingerprint doesn't match.");
			return null;
		}
	}

	private String extractHash(String path) {
		Matcher matcher = pattern.matcher(path);
		if (matcher.find()) {
			String match = matcher.group(1);
			return match.contains("-") ? match.substring(match.lastIndexOf("-") + 1) : match;
		}
		else {
			return "";
		}
	}

	private String calculateHash(Resource resource) {
		try {
			byte[] content = FileCopyUtils.copyToByteArray(resource.getInputStream());
			return DigestUtils.md5DigestAsHex(content);
		}
		catch (IOException e) {
			logger.error("Failed to calculate hash for resource [" + resource + "]");
			return "";
		}
	}

	@Override
	public String resolvePublicUrlPath(String resourceUrlPath, List<? extends Resource> locations,
			ResourceResolverChain chain) {
		String baseUrl = chain.resolvePublicUrlPath(resourceUrlPath, locations);
		if (StringUtils.hasText(baseUrl)) {
			Resource original = chain.resolveResource(null, resourceUrlPath, locations);
			String hash = calculateHash(original);
			return StringUtils.stripFilenameExtension(baseUrl) + "-" + hash + "."
					+ StringUtils.getFilenameExtension(baseUrl);
		}
		return baseUrl;
	}

}
