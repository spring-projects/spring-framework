/*
 * Copyright 2002-2013 the original author or authors.
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
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class FingerprintResourceResolver implements ResourceResolver {

	private static final Log logger = LogFactory.getLog(FingerprintResourceResolver.class);

	private Pattern pattern = Pattern.compile("-(\\S*)\\.");


	@Override
	public Resource resolveResource(HttpServletRequest request, String requestPath,
			List<Resource> locations, ResourceResolverChain chain) {

		// First try the resolved full path, in case resource has been written that way to disk at build-time
		// or the resource is requested without fingerprint

		Resource resolved = chain.resolveResource(request, requestPath, locations);
		if (resolved != null) {
			return resolved;
		}

		// Now try extracting and matching the hash for dev mode
		String hash = extractHash(requestPath);
		if (StringUtils.isEmpty(hash)) {
			return null;
		}

		String simplePath = StringUtils.delete(requestPath, "-" + hash);
		Resource baseResource = chain.resolveResource(request, simplePath, locations);
		if (baseResource == null) {
			logger.debug("Failed to find resource after removing fingerprint: " + simplePath);
			return null;
		}

		String candidateHash = calculateHash(baseResource);
		if (candidateHash.equals(hash)) {
			logger.debug("Fingerprint match succeeded.");
			return baseResource;
		}
		else {
			logger.debug("Potential resource found, but fingerprint doesn't match.");
			return null;
		}
	}

	private String extractHash(String path) {
		Matcher matcher = this.pattern.matcher(path);
		if (matcher.find()) {
			logger.debug("Found fingerprint in path: " + matcher.group(1));
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
			logger.error("Failed to calculate hash on resource " + resource.toString());
			return "";
		}
	}

	@Override
	public String resolveUrlPath(String resourcePath, List<Resource> locations, ResourceResolverChain chain) {
		// TODO - Consider caching here for better efficiency
		String baseUrl = chain.resolveUrlPath(resourcePath, locations);
		if (StringUtils.hasText(baseUrl)) {
			Resource original = chain.resolveResource(null, resourcePath, locations);
			String hash = calculateHash(original);
			return StringUtils.stripFilenameExtension(baseUrl)
					+ "-" + hash + "." + StringUtils.getFilenameExtension(baseUrl);
		}
		return baseUrl;
	}

}
