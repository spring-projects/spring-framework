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
 */
public class FingerprintingResourceResolver extends AbstractResourceResolver {

	private final Log logger = LogFactory.getLog(getClass());
	
	private Pattern pattern = Pattern.compile("-(\\S*)\\.");
	
	@Override
	protected Resource resolveInternal(HttpServletRequest request, String path, List<Resource> locations,
			ResourceResolverChain chain, Resource resolved) {
		//First try the resolved full path, in case resource has been written that way to disk at build-time
		//or the resource is requested without fingerprint
		if (resolved != null) {
			return resolved;
		}
		
		//Now try extracting and matching the hash for dev mode
		String hash = extractHash(path);
		String simplePath = !StringUtils.isEmpty(hash) ? StringUtils.delete(path, "-" + hash) : path;
		Resource baseResource = chain.next(this).resolve(request, simplePath, locations, chain);
		
		if (StringUtils.isEmpty(hash) || baseResource == null) {
			return baseResource;
		}
		
		String candidateHash = calculateHash(baseResource);
		
		if (candidateHash.equals(hash)) {
			this.logger.debug("Fingerprint match succeeded.");
			return baseResource;
		} else {
			this.logger.debug("Potential resource found, but fingerprint doesn't match.");
			return null;
		}
	}
	
	@Override
	public String resolveUrl(String resourcePath, List<Resource> locations,
			ResourceResolverChain chain) {
		//TODO - Consider caching here for better efficiency
		String baseUrl = chain.next(this).resolveUrl(resourcePath, locations, chain);
		if (StringUtils.hasText(baseUrl)) {
			Resource original = chain.next(this).resolve(null, resourcePath, locations, chain);
			String hash = calculateHash(original);
			return StringUtils.stripFilenameExtension(baseUrl) + "-" + hash + "." + StringUtils.getFilenameExtension(baseUrl);
		}
		return baseUrl;
	}

	/**
	 * @param candidate
	 * @return
	 */
	private String calculateHash(Resource resource) {
		try {
			byte[] content = FileCopyUtils.copyToByteArray(resource.getInputStream());
			return DigestUtils.md5DigestAsHex(content);
		}
		catch (IOException e) {
			this.logger.error("Failed to calculate hash on resource " + resource.toString());
			return "";
		}
	}

	/**
	 * @param path
	 * @return
	 */
	private String extractHash(String path) {
		Matcher matcher = pattern.matcher(path);
		if (matcher.find()) {
			this.logger.debug("Found fingerprint in path: " + matcher.group(1));
			String match = matcher.group(1);
			return match.contains("-") ? match.substring(match.lastIndexOf("-") + 1) : match;
		} else {
			return "";
		}
	}

}
