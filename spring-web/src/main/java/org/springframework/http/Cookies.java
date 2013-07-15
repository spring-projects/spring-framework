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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Cookies {

	private final List<Cookie> cookies;


	public Cookies() {
		this.cookies = new ArrayList<Cookie>();
	}

	private Cookies(Cookies cookies) {
		this.cookies = Collections.unmodifiableList(cookies.getCookies());
	}

	public static Cookies readOnlyCookies(Cookies cookies) {
		return new Cookies(cookies);
	}

	public List<Cookie> getCookies() {
		return this.cookies;
	}

	public Cookie getCookie(String name) {
		for (Cookie c : this.cookies) {
			if (c.getName().equals(name)) {
				return c;
			}
		}
		return null;
	}

	public Cookie addCookie(String name, String value) {
		DefaultCookie cookie = new DefaultCookie(name, value);
		this.cookies.add(cookie);
		return cookie;
	}

}
