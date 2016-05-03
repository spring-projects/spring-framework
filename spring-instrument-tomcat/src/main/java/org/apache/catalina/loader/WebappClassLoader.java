/*
 * Copyright 2002-2015 the original author or authors.
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

package org.apache.catalina.loader;

/**
 * A mock of Tomcat's {@code WebappClassLoader} just for Spring's compilation purposes.
 * Exposes both pre-7.0.63 as well as 7.0.63+ variants of {@code findResourceInternal}.
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
public class WebappClassLoader extends ClassLoader {

	public WebappClassLoader() {
	}

	public WebappClassLoader(ClassLoader parent) {
		super(parent);
	}


	protected ResourceEntry findResourceInternal(String name, String path) {
		throw new UnsupportedOperationException();
	}

	protected ResourceEntry findResourceInternal(String name, String path, boolean manifestRequired) {
		throw new UnsupportedOperationException();
	}

}
