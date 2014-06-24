package org.springframework.web.servlet.resource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.StringUtils;

/**
 * Abstract base class for {@link VersionStrategy} implementations.
 * Supports versions as:
 * <ul>
 *     <li>prefix in the request path, like "version/static/myresource.js"
 *     <li>file name suffix in the request path, like "static/myresource-version.js"
 * </ul>
 *
 * <p>Note: This base class does <i>not</i> provide support for generating the
 * version string.
 *
 * @author Brian Clozel
 * @since 4.1
 */
public abstract class AbstractVersionStrategy implements VersionStrategy {

	private static final Pattern pattern = Pattern.compile("-(\\S*)\\.");

	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Extracts a version string from the request path, as a suffix in the resource
	 * file name.
	 * @param requestPath the request path to extract the version string from
	 * @return a version string or an empty string if none was found
	 */
	protected String extractVersionFromFilename(String requestPath) {
		Matcher matcher = pattern.matcher(requestPath);
		if (matcher.find()) {
			String match = matcher.group(1);
			return match.contains("-") ? match.substring(match.lastIndexOf("-") + 1) : match;
		}
		else {
			return "";
		}
	}

	/**
	 * Deletes the given candidate version string from the request path.
	 * The version string should be a suffix in the resource file name.
	 */
	protected String deleteVersionFromFilename(String requestPath, String candidateVersion) {
		return StringUtils.delete(requestPath, "-" + candidateVersion);
	}

	/**
	 * Adds the given version string to the baseUrl, as a file name suffix.
	 */
	protected String addVersionToFilename(String baseUrl, String version) {
		String baseFilename = StringUtils.stripFilenameExtension(baseUrl);
		String extension = StringUtils.getFilenameExtension(baseUrl);
		return baseFilename + "-" + version + "." + extension;
	}

	/**
	 * Extracts a version string from the request path, as a prefix in the request path.
	 * @param requestPath the request path to extract the version string from
	 * @return a version string or an empty string if none was found
	 */
	protected String extractVersionAsPrefix(String requestPath, String prefix) {
		if (requestPath.startsWith(prefix)) {
			return prefix;
		}
		return "";
	}

	/**
	 * Deletes the given candidate version string from the request path.
	 * The version string should be a prefix in the request path.
	 */
	protected String deleteVersionAsPrefix(String requestPath, String version) {
		return requestPath.substring(version.length());
	}

	/**
	 * Adds the given version string to the baseUrl, as a prefix in the request path.
	 */
	protected String addVersionAsPrefix(String baseUrl, String version) {
		return version + baseUrl;
	}

}
