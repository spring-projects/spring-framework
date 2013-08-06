/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.http;


/**
 * Representation of a cookie value parsed from a "Cookie" request header or a
 * "Set-Cookie" response header.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 *
 * @see http://www.ietf.org/rfc/rfc2109.txt
 */
public interface Cookie {

	/**
	 * Returns the name of the cookie.
	 */
	String getName();

	/**
	 * Returns the value of the cookie.
	 */
	String getValue();

	/**
	 * Returns the path on the server to which the browser returns this cookie.
	 */
	String getPath();

	/**
	 * Returns the comment describing the purpose of this cookie.
	 */
	String getComment();

	/**
	 * Returns the domain name set for this cookie.
	 */
	String getDomain();

	/**
	 * Returns the maximum age of the cookie, specified in seconds.
	 */
	int getMaxAge();

	/**
     * Returns <code>true</code> if the browser is sending cookies only over a
     * secure protocol, or <code>false</code> if the browser can send cookies
     * using any protocol.
     */
    boolean isSecure();

    /**
     * Sets the version of the cookie protocol this cookie complies with.
     */
    int getVersion();

}
