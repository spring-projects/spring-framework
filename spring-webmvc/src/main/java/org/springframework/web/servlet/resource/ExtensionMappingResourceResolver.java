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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;


/**
 *
 * @author Jeremy Grelle
 * @since 4.0
 */
public class ExtensionMappingResourceResolver extends AbstractResourceResolver {

	private static final Log logger = LogFactory.getLog(ExtensionMappingResourceResolver.class);

	private final boolean compareTimeStamp;


	public ExtensionMappingResourceResolver() {
		this.compareTimeStamp = false;
	}

	public ExtensionMappingResourceResolver(boolean compareTimeStamp) {
		this.compareTimeStamp = compareTimeStamp;
	}


	@Override
	protected Resource resolveInternal(HttpServletRequest request, String path,
			List<Resource> locations, ResourceResolverChain chain, Resource resource) {

		if ((resource != null) && !this.compareTimeStamp) {
			return resource;
		}

		for (Resource location : locations) {
			String baseFilename = StringUtils.getFilename(path);
			try {
				Resource basePath = location.createRelative(StringUtils.delete(path, baseFilename));
				if (basePath.getFile().isDirectory()) {
					for (String fileName : basePath.getFile().list(new ExtensionFilenameFilter(baseFilename))) {
						//Always use the first match
						Resource matched = basePath.createRelative(fileName);
						if ((resource == null) || (matched.lastModified() > resource.lastModified())) {
							return matched;
						}
						else {
							return resource;
						}
					}
				}
			}
			catch (IOException e) {
				logger.trace("Error occurred locating resource based on file extension mapping", e);
			}
		}

		return resource;
	}

	@Override
	public String resolveUrl(String resourcePath, List<Resource> locations,
			ResourceResolverChain chain) {

		String resolved = super.resolveUrl(resourcePath, locations, chain);
		if (StringUtils.hasText(resolved)) {
			return resolved;
		}

		Resource mappedResource = resolveInternal(null, resourcePath, locations, chain, null);
		if (mappedResource != null) {
			return resourcePath;
		}
		return null;
	}


	private static final class ExtensionFilenameFilter implements FilenameFilter {

		private final String filename;

		private final String extension;

		private final int extensionLength;


		public ExtensionFilenameFilter(String filename) {
			this.filename = filename;
			this.extension = "." + StringUtils.getFilenameExtension(filename);
			this.extensionLength = this.extension.length();
		}

		@Override
		public boolean accept(File directory, String name) {
			return (name.contains(this.extension)
					&& this.filename.equals(name.substring(0, name.lastIndexOf(this.extension) + this.extensionLength)));
		}
	}

}
