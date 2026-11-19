/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;

/**
 * Abstract base class for filename suffix based {@link VersionStrategy}
 * implementations, for example, "static/myresource-version.js".
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class AbstractFileNameVersionStrategy implements VersionStrategy {

	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public @Nullable String extractVersion(String requestPath) {
		// Equivalent to the first match of "-(\S*)\." without the backtracking
		// of a regular expression, which is super-linear on paths with many '-'
		int length = requestPath.length();
		int dashIndex = requestPath.indexOf('-');
		while (dashIndex != -1) {
			int dotIndex = -1;
			int index = dashIndex + 1;
			for (; index < length && !isWhitespace(requestPath.charAt(index)); index++) {
				if (requestPath.charAt(index) == '.') {
					dotIndex = index;
				}
			}
			if (dotIndex != -1) {
				String match = requestPath.substring(dashIndex + 1, dotIndex);
				return (match.contains("-") ? match.substring(match.lastIndexOf('-') + 1) : match);
			}
			dashIndex = requestPath.indexOf('-', index);
		}
		return null;
	}

	private static boolean isWhitespace(char ch) {
		return (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\u000B' || ch == '\f' || ch == '\r');
	}

	@Override
	public String removeVersion(String requestPath, String version) {
		String versionString = "-" + version;
		int index = requestPath.lastIndexOf(versionString);
		if (index != -1) {
			return requestPath.substring(0, index) + requestPath.substring(index + versionString.length());
		}
		return requestPath;
	}

	@Override
	public String addVersion(String requestPath, String version) {
		String baseFilename = StringUtils.stripFilenameExtension(requestPath);
		String extension = StringUtils.getFilenameExtension(requestPath);
		return (baseFilename + '-' + version + '.' + extension);
	}

}
