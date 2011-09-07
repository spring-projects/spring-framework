/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.util;

/**
 * Enumeration used to identify the parts of a URI.
 *
 * <p>Contains methods to indicate whether a given character is valid in a specific URI component.
 *
 * @author Arjen Poutsma
 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
 * @since 3.1
 */
public enum UriComponent {

	SCHEME {
		@Override
		public boolean isAllowed(int c) {
			return isAlpha(c) || isDigit(c) || '+' == c || '-' == c || '.' == c;
		}
	},
	AUTHORITY {
		@Override
		public boolean isAllowed(int c) {
			return isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c;
		}
	},
	USER_INFO {
		@Override
		public boolean isAllowed(int c) {
			return isUnreserved(c) || isSubDelimiter(c) || ':' == c;
		}
	},
	HOST {
		@Override
		public boolean isAllowed(int c) {
			return isUnreserved(c) || isSubDelimiter(c);
		}
	},
	PORT {
		@Override
		public boolean isAllowed(int c) {
			return isDigit(c);
		}
	},
	PATH {
		@Override
		public boolean isAllowed(int c) {
			return isPchar(c) || '/' == c;
		}
	},
	PATH_SEGMENT {
		@Override
		public boolean isAllowed(int c) {
			return isPchar(c);
		}
	},
	QUERY {
		@Override
		public boolean isAllowed(int c) {
			return isPchar(c) || '/' == c || '?' == c;
		}
	},
	QUERY_PARAM {
		@Override
		public boolean isAllowed(int c) {
			if ('=' == c || '+' == c || '&' == c) {
				return false;
			}
			else {
				return isPchar(c) || '/' == c || '?' == c;
			}
		}
	},
	FRAGMENT {
		@Override
		public boolean isAllowed(int c) {
			return isPchar(c) || '/' == c || '?' == c;
		}
	};

	/**
	 * Indicates whether the given character is allowed in this URI component.
	 *
	 * @param c the character
	 * @return {@code true} if the character is allowed; {@code false} otherwise
	 */
	public abstract boolean isAllowed(int c);

	/**
	 * Indicates whether the given character is in the {@code ALPHA} set.
	 *
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
	 */
	protected boolean isAlpha(int c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
	}

	/**
	 * Indicates whether the given character is in the {@code DIGIT} set.
	 *
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
	 */
	protected boolean isDigit(int c) {
		return c >= '0' && c <= '9';
	}

	/**
	 * Indicates whether the given character is in the {@code gen-delims} set.
	 *
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
	 */
	protected boolean isGenericDelimiter(int c) {
		return ':' == c || '/' == c || '?' == c || '#' == c || '[' == c || ']' == c || '@' == c;
	}

	/**
	 * Indicates whether the given character is in the {@code sub-delims} set.
	 *
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
	 */
	protected boolean isSubDelimiter(int c) {
		return '!' == c || '$' == c || '&' == c || '\'' == c || '(' == c || ')' == c || '*' == c || '+' == c ||
				',' == c || ';' == c || '=' == c;
	}

	/**
	 * Indicates whether the given character is in the {@code reserved} set.
	 *
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
	 */
	protected boolean isReserved(char c) {
		return isGenericDelimiter(c) || isReserved(c);
	}

	/**
	 * Indicates whether the given character is in the {@code unreserved} set.
	 *
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
	 */
	protected boolean isUnreserved(int c) {
		return isAlpha(c) || isDigit(c) || '-' == c || '.' == c || '_' == c || '~' == c;
	}

	/**
	 * Indicates whether the given character is in the {@code pchar} set.
	 *
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
	 */
	protected boolean isPchar(int c) {
		return isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c;
	}

}
