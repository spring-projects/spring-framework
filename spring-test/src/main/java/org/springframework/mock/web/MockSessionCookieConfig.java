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

package org.springframework.mock.web;

import javax.servlet.SessionCookieConfig;

/**
 * Mock implementation of the {@link javax.servlet.SessionCookieConfig} interface.
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see javax.servlet.ServletContext#getSessionCookieConfig()
 */
public class MockSessionCookieConfig implements SessionCookieConfig {

	private String name;

	private String domain;

	private String path;

	private String comment;

	private boolean httpOnly;

	private boolean secure;

	private int maxAge;


	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getDomain() {
		return this.domain;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getPath() {
		return this.path;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getComment() {
		return this.comment;
	}

	public void setHttpOnly(boolean httpOnly) {
		this.httpOnly = httpOnly;
	}

	public boolean isHttpOnly() {
		return this.httpOnly;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public boolean isSecure() {
		return this.secure;
	}

	public void setMaxAge(int maxAge) {
		this.maxAge = maxAge;
	}

	public int getMaxAge() {
		return this.maxAge;
	}

}
