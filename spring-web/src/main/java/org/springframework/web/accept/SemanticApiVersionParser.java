/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.accept;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * Parser for semantic API versioning with major, minor, and patch values.
 * For example, "1", "1.0", "1.2", "1.2.0", "1.2.3". Leading, non-integer
 * characters, as in "v1.0", are skipped.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class SemanticApiVersionParser implements ApiVersionParser<SemanticApiVersionParser.Version> {

	private static final Pattern semanticVersionPattern = Pattern.compile("^(\\d+)(\\.(\\d+))?(\\.(\\d+))?$");


	@Override
	public Version parseVersion(String version) {
		Assert.notNull(version, "'version' is required");

		version = skipNonDigits(version);

		Matcher matcher = semanticVersionPattern.matcher(version);
		Assert.state(matcher.matches(), "Invalid API version format");

		String major = matcher.group(1);
		String minor = matcher.group(3);
		String patch = matcher.group(5);

		return new Version(
				Integer.parseInt(major),
				(minor != null ? Integer.parseInt(minor) : 0),
				(patch != null ? Integer.parseInt(patch) : 0));
	}

	private static String skipNonDigits(String value) {
		for (int i = 0; i < value.length(); i++) {
			if (Character.isDigit(value.charAt(i))) {
				return value.substring(i);
			}
		}
		return "";
	}


	/**
	 * Representation of a semantic version.
	 */
	public static final class Version implements Comparable<Version> {

		private final int major;

		private final int minor;

		private final int patch;

		Version(int major, int minor, int patch) {
			this.major = major;
			this.minor = minor;
			this.patch = patch;
		}

		public int getMajor() {
			return this.major;
		}

		public int getMinor() {
			return this.minor;
		}

		public int getPatch() {
			return this.patch;
		}

		@Override
		public int compareTo(SemanticApiVersionParser.Version other) {
			int result = Integer.compare(this.major, other.major);
			if (result != 0) {
				return result;
			}
			result = Integer.compare(this.minor, other.minor);
			if (result != 0) {
				return result;
			}
			return Integer.compare(this.patch, other.patch);
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof Version otherVersion &&
					this.major == otherVersion.major &&
					this.minor == otherVersion.minor &&
					this.patch == otherVersion.patch));
		}

		@Override
		public int hashCode() {
			int result = this.major;
			result = 31 * result + this.minor;
			result = 31 * result + this.patch;
			return result;
		}

		@Override
		public String toString() {
			return this.major + "." + this.minor + "." + this.patch;
		}
	}

}
