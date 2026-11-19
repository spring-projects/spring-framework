/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for {@link VersionStrategy} implementations.
 *
 * <p>Supports versions as:
 * <ul>
 * <li>prefix in the request path, like "version/static/myresource.js"
 * <li>file name suffix in the request path, like "static/myresource-version.js"
 * </ul>
 *
 * <p>Note: This base class does <i>not</i> provide support for generating the
 * version string.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public abstract class AbstractVersionStrategy implements VersionStrategy {

	protected final Log logger = LogFactory.getLog(getClass());

	private final VersionPathStrategy pathStrategy;


	protected AbstractVersionStrategy(VersionPathStrategy pathStrategy) {
		Assert.notNull(pathStrategy, "VersionPathStrategy is required");
		this.pathStrategy = pathStrategy;
	}


	public VersionPathStrategy getVersionPathStrategy() {
		return this.pathStrategy;
	}


	@Override
	public @Nullable String extractVersion(String requestPath) {
		return this.pathStrategy.extractVersion(requestPath);
	}

	@Override
	public String removeVersion(String requestPath, String version) {
		return this.pathStrategy.removeVersion(requestPath, version);
	}

	@Override
	public String addVersion(String requestPath, String version) {
		return this.pathStrategy.addVersion(requestPath, version);
	}


	/**
	 * A prefix-based {@code VersionPathStrategy},
	 * for example, {@code "{version}/path/foo.js"}.
	 */
	protected static class PrefixVersionPathStrategy implements VersionPathStrategy {

		private final String prefix;

		public PrefixVersionPathStrategy(String version) {
			Assert.hasText(version, "Version must not be empty");
			this.prefix = version;
		}

		@Override
		public @Nullable String extractVersion(String requestPath) {
			return (requestPath.startsWith(this.prefix) ? this.prefix : null);
		}

		@Override
		public String removeVersion(String requestPath, String version) {
			return requestPath.substring(this.prefix.length());
		}

		@Override
		public String addVersion(String path, String version) {
			if (path.startsWith(".")) {
				return path;
			}
			else {
				return (this.prefix.endsWith("/") || path.startsWith("/") ?
						this.prefix + path : this.prefix + '/' + path);
			}
		}
	}


	/**
	 * File name-based {@code VersionPathStrategy},
	 * for example, {@code "path/foo-{version}.css"}.
	 */
	protected static class FileNameVersionPathStrategy implements VersionPathStrategy {

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

}
