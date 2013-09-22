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
 */
public class ExtensionMappingResourceResolver extends AbstractResourceResolver {
	
	private final Log logger = LogFactory.getLog(getClass());
	
	private final boolean compareTimeStamp;
	
	public ExtensionMappingResourceResolver() {
		this.compareTimeStamp = false;
	}
	
	public ExtensionMappingResourceResolver(boolean compareTimeStamp) {
		this.compareTimeStamp = compareTimeStamp;
	}
	
	@Override
	protected Resource resolveInternal(HttpServletRequest request, String path,
			List<Resource> locations, ResourceResolverChain chain, Resource resolved) {
		if (resolved != null && !compareTimeStamp) {
			return resolved;
		}
		
		for (Resource location : locations) {
			String baseFilename = StringUtils.getFilename(path);
			
			try {
				Resource basePath = location.createRelative(StringUtils.delete(path, baseFilename));
				if (basePath.getFile().isDirectory()) {
					for (String fileName : basePath.getFile().list(new ExtensionFilter(baseFilename))) {
						//Always use the first match
						Resource matched = basePath.createRelative(fileName);
						if (resolved == null || matched.lastModified() > resolved.lastModified()) {
							return matched;
						} else {
							return resolved;
						}
					}
				}
			}
			catch (IOException e) {
				this.logger.trace("Error occurred locating resource based on file extension mapping", e);
			}
			
		}
		return resolved;
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



	private static final class ExtensionFilter implements FilenameFilter{

		private final String baseFilename;
		private final String baseExtension;
		private final int baseExtLen;
		
		
		public ExtensionFilter(String baseFilename) {
			this.baseFilename = baseFilename;
			this.baseExtension = "." + StringUtils.getFilenameExtension(baseFilename);
			this.baseExtLen = this.baseExtension.length();
		}
		
		@Override
		public boolean accept(File dir, String name) {
			return name.contains(baseExtension) && baseFilename.equals(name.substring(0, name.lastIndexOf(baseExtension) + this.baseExtLen));  
		}
	}
}
