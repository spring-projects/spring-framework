/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.http.server;

import org.springframework.http.Cookie;


/**
 * A {@link Cookie} that wraps a {@link javax.servlet.http.Cookie}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ServletServerCookie implements Cookie {

	private final javax.servlet.http.Cookie servletCookie;


	public ServletServerCookie(javax.servlet.http.Cookie servletCookie) {
		this.servletCookie = servletCookie;
	}

	@Override
	public String getName() {
		return this.servletCookie.getName();
	}

	@Override
	public String getValue() {
		return this.servletCookie.getValue();
	}

	@Override
	public String getPath() {
		return this.servletCookie.getPath();
	}

	@Override
	public String getComment() {
		return this.servletCookie.getComment();
	}

	@Override
	public String getDomain() {
		return this.servletCookie.getDomain();
	}

	@Override
	public int getMaxAge() {
		return this.servletCookie.getMaxAge();
	}

	@Override
	public boolean isSecure() {
		return this.servletCookie.getSecure();
	}

	@Override
	public int getVersion() {
		return this.servletCookie.getVersion();
	}

	@Override
	public String toString() {
		return "ServletServerCookie [servletCookie=" + this.servletCookie + "]";
	}
}
