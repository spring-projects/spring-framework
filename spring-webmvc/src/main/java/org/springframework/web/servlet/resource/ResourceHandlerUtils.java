/*
 * Copyright 2002-2025 the original author or authors.
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

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.util.UriUtils;

/**
 * Resource handling utility methods to share common logic between
 * {@link ResourceHttpRequestHandler} and {@link org.springframework.web.servlet.function}.
 *
 * @author Rossen Stoyanchev
 * @since 6.2
 */
public abstract class ResourceHandlerUtils {

	private static final Log logger = LogFactory.getLog(ResourceHandlerUtils.class);

	private static final String FOLDER_SEPARATOR = "/";

	private static final String WINDOWS_FOLDER_SEPARATOR = "\\";


	/**
	 * Assert the given location is not null, and its path ends on slash.
	 */
	@SuppressWarnings("removal")
	public static void assertResourceLocation(@Nullable Resource location) {
		Assert.notNull(location, "Resource location must not be null");
		try {
			String path;
			if (location instanceof org.springframework.core.io.PathResource) {
				return;
			}
			else if (location instanceof FileSystemResource fileSystemResource) {
				path = fileSystemResource.getPath();
			}
			else if (location instanceof ClassPathResource classPathResource) {
				path = classPathResource.getPath();
			}
			else if (location instanceof UrlResource) {
				path = location.getURL().toExternalForm();
			}
			else {
				path = location.getURL().getPath();
			}
			Assert.isTrue(path.endsWith(FOLDER_SEPARATOR) || path.endsWith(WINDOWS_FOLDER_SEPARATOR),
					"Resource location does not end with slash: " + path);
		}
		catch (IOException ignored) {
		}
	}

	/**
	 * Check if the given static resource location path ends with a trailing
	 * slash, and append it if necessary.
	 * @param path the location path
	 * @return the resulting path to use
	 */
	public static String initLocationPath(String path) {
		String separator = (path.contains(FOLDER_SEPARATOR) ? FOLDER_SEPARATOR : WINDOWS_FOLDER_SEPARATOR);
		if (!path.endsWith(separator)) {
			path = path.concat(separator);
			logger.warn("Appended trailing slash to static resource location: " + path);
		}
		return path;
	}

	/**
	 * Normalize the given resource path replacing the following:
	 * <ul>
	 * <li>Backslash with forward slash.
	 * <li>Duplicate occurrences of slash with a single slash.
	 * <li>Any combination of leading slash and control characters (00-1F and 7F)
	 * with a single "/" or "". For example {@code "  / // foo/bar"}
	 * becomes {@code "/foo/bar"}.
	 * </ul>
	 */
	public static String normalizeInputPath(String path) {
		path = StringUtils.replace(path, "\\", "/");
		path = cleanDuplicateSlashes(path);
		return cleanLeadingSlash(path);
	}

	private static String cleanDuplicateSlashes(String path) {
		StringBuilder sb = null;
		char prev = 0;
		for (int i = 0; i < path.length(); i++) {
			char curr = path.charAt(i);
			try {
				if ((curr == '/') && (prev == '/')) {
					if (sb == null) {
						sb = new StringBuilder(path.substring(0, i));
					}
					continue;
				}
				if (sb != null) {
					sb.append(path.charAt(i));
				}
			}
			finally {
				prev = curr;
			}
		}
		return (sb != null ? sb.toString() : path);
	}

	private static String cleanLeadingSlash(String path) {
		boolean slash = false;
		for (int i = 0; i < path.length(); i++) {
			if (path.charAt(i) == '/') {
				slash = true;
			}
			else if (path.charAt(i) > ' ' && path.charAt(i) != 127) {
				if (i == 0 || (i == 1 && slash)) {
					return path;
				}
				return (slash ? "/" + path.substring(i) : path.substring(i));
			}
		}
		return (slash ? "/" : "");
	}

	/**
	 * Whether the given input path is invalid as determined by
	 * {@link #isInvalidPath(String)}. The path is also decoded and the same
	 * checks are performed again.
	 */
	public static boolean shouldIgnoreInputPath(String path) {
		return (!StringUtils.hasText(path) || isInvalidPath(path) || isInvalidEncodedPath(path));
	}

	/**
	 * Checks for invalid resource input paths rejecting the following:
	 * <ul>
	 * <li>Paths that contain "WEB-INF" or "META-INF"
	 * <li>Paths that contain "../" after a call to
	 * {@link org.springframework.util.StringUtils#cleanPath}.
	 * <li>Paths that represent a {@link org.springframework.util.ResourceUtils#isUrl
	 * valid URL} or would represent one after the leading slash is removed.
	 * </ul>
	 * <p><strong>Note:</strong> this method assumes that leading, duplicate '/'
	 * or control characters (e.g. white space) have been trimmed so that the
	 * path starts predictably with a single '/' or does not have one.
	 * @param path the path to validate
	 * @return {@code true} if the path is invalid, {@code false} otherwise
	 */
	public static boolean isInvalidPath(String path) {
		if (path.contains("WEB-INF") || path.contains("META-INF")) {
			if (logger.isWarnEnabled()) {
				logger.warn(LogFormatUtils.formatValue(
						"Path with \"WEB-INF\" or \"META-INF\": [" + path + "]", -1, true));
			}
			return true;
		}
		if (path.contains(":/")) {
			String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
			if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
				if (logger.isWarnEnabled()) {
					logger.warn(LogFormatUtils.formatValue(
							"Path represents URL or has \"url:\" prefix: [" + path + "]", -1, true));
				}
				return true;
			}
		}
		if (path.contains("../")) {
			if (logger.isWarnEnabled()) {
				logger.warn(LogFormatUtils.formatValue(
						"Path contains \"../\" after call to StringUtils#cleanPath: [" + path + "]", -1, true));
			}
			return true;
		}
		return false;
	}

	/**
	 * Check whether the given path contains invalid escape sequences.
	 * @param path the path to validate
	 * @return {@code true} if the path is invalid, {@code false} otherwise
	 */
	private static boolean isInvalidEncodedPath(String path) {
		String decodedPath = decode(path);
		if (decodedPath.contains("%")) {
			decodedPath = decode(decodedPath);
		}
		if (!StringUtils.hasText(decodedPath)) {
			return true;
		}
		if (isInvalidPath(decodedPath)) {
			return true;
		}
		decodedPath = normalizeInputPath(decodedPath);
		return isInvalidPath(decodedPath);
	}

	private static String decode(String path) {
		try {
			return UriUtils.decode(path, StandardCharsets.UTF_8);
		}
		catch (Exception ex) {
			return "";
		}
	}


	/**
	 * Check whether the resource is under the given location.
	 */
	public static boolean isResourceUnderLocation(Resource location, Resource resource) throws IOException {
		if (resource.getClass() != location.getClass()) {
			return false;
		}

		String resourcePath;
		String locationPath;

		if (resource instanceof UrlResource) {
			resourcePath = resource.getURL().toExternalForm();
			locationPath = StringUtils.cleanPath(location.getURL().toString());
		}
		else if (resource instanceof ClassPathResource classPathResource) {
			resourcePath = classPathResource.getPath();
			locationPath = StringUtils.cleanPath(((ClassPathResource) location).getPath());
		}
		else if (resource instanceof ServletContextResource servletContextResource) {
			resourcePath = servletContextResource.getPath();
			locationPath = StringUtils.cleanPath(((ServletContextResource) location).getPath());
		}
		else {
			resourcePath = resource.getURL().getPath();
			locationPath = StringUtils.cleanPath(location.getURL().getPath());
		}

		if (locationPath.equals(resourcePath)) {
			return true;
		}
		locationPath = (locationPath.endsWith("/") || locationPath.isEmpty() ? locationPath : locationPath + "/");
		return (resourcePath.startsWith(locationPath) && !isInvalidEncodedResourcePath(resourcePath));
	}

	private static boolean isInvalidEncodedResourcePath(String resourcePath) {
		if (resourcePath.contains("%")) {
			// Use URLDecoder (vs UriUtils) to preserve potentially decoded UTF-8 chars...
			try {
				String decodedPath = URLDecoder.decode(resourcePath, StandardCharsets.UTF_8);
				if (decodedPath.contains("../") || decodedPath.contains("..\\")) {
					logger.warn(LogFormatUtils.formatValue(
							"Resolved resource path contains encoded \"../\" or \"..\\\": " + resourcePath, -1, true));
					return true;
				}
			}
			catch (IllegalArgumentException ex) {
				// May not be possible to decode...
			}
		}
		return false;
	}

}
