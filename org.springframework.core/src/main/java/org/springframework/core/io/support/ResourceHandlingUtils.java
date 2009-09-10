/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.core.io.support;

import java.net.URL;
import java.net.URI;
import java.util.Set;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.ResourceUtils;
import org.springframework.util.Assert;
import org.springframework.core.io.support.jboss.VfsResourceHandlingDelegate;
import org.springframework.core.io.Resource;

/**
 * Utility class for determining whether a given URL is a resource
 * location that should receive special treatmen, such as looking up a
 * resource in JBoss VFS.
 *
 * @author Thomas Risberg
 * @author Marius Bogoevici
 * @since 3.0
 */
public abstract class ResourceHandlingUtils {

	private static ResourceHandlingDelegate resourceHandlingDelegate;

	static {
		try {
			Class jBossVersionClass = PathMatchingResourcePatternResolver.class.getClassLoader().loadClass("org.jboss.Version");
			Object versionObject = ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(jBossVersionClass, "getInstance"), null);
			Integer majorVersion = (Integer) ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(jBossVersionClass, "getMajor"), versionObject);
			// For JBoss AS versions 5 and higher
			if (majorVersion >= 5) {
				resourceHandlingDelegate = new VfsResourceHandlingDelegate();
			}
		}
		catch (Throwable ex) {
			// do nothing
		}
	}

	public static File getFile(URL resourceUrl) throws FileNotFoundException {
		return getFile(resourceUrl, "URL");
	}

	public static File getFile(URL resourceUrl, String description) throws FileNotFoundException {
		Assert.notNull(resourceUrl, "Resource URL must not be null");
		if (useResourceHandlingDelegate(resourceUrl)) {
			try {
				return resourceHandlingDelegate.loadResource(resourceUrl).getFile();
			}
			catch (IOException e) {
				throw new FileNotFoundException(description + " cannot be resolved as a file resource");
			}
		}
		return ResourceUtils.getFile(resourceUrl, description);
	}

	public static File getFile(URI resourceUri) throws FileNotFoundException {
		return getFile(resourceUri, "URI");
	}

	public static File getFile(URI resourceUri, String description) throws FileNotFoundException {
		Assert.notNull(resourceUri, "Resource URI must not be null");
		if (useResourceHandlingDelegate(resourceUri)) {
			try {
				return resourceHandlingDelegate.loadResource(resourceUri).getFile();
			}
			catch (IOException e) {
				throw new FileNotFoundException(description + " cannot be resolved as a file resource");
			}
		}
		return ResourceUtils.getFile(resourceUri, description);
	}

	public static boolean useResourceHandlingDelegate(URL url) {
		return resourceHandlingDelegate != null && resourceHandlingDelegate.canHandleResource(url);
	}

	public static boolean useResourceHandlingDelegate(URI uri) {
		return resourceHandlingDelegate != null && resourceHandlingDelegate.canHandleResource(uri);
	}

	public static Set<Resource> findMatchingResourcesByDelegate(Resource resource, String pattern, PathMatcher matcher) throws
			IOException {
		return resourceHandlingDelegate.findMatchingResources(resource, pattern, matcher);
	}
}