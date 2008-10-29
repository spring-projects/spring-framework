/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import junit.framework.TestCase;

/**
 * @author Juergen Hoeller
 */
public class ResourceUtilsTests extends TestCase {

	public void testIsJarURL() throws Exception {
		assertTrue(ResourceUtils.isJarURL(new URL("jar:file:myjar.jar!/mypath")));
		assertTrue(ResourceUtils.isJarURL(new URL(null, "zip:file:myjar.jar!/mypath", new DummyURLStreamHandler())));
		assertTrue(ResourceUtils.isJarURL(new URL(null, "wsjar:file:myjar.jar!/mypath", new DummyURLStreamHandler())));
		assertFalse(ResourceUtils.isJarURL(new URL("file:myjar.jar")));
		assertFalse(ResourceUtils.isJarURL(new URL("http:myserver/myjar.jar")));
	}

	public void testExtractJarFileURL() throws Exception {
		assertEquals(new URL("file:myjar.jar"),
				ResourceUtils.extractJarFileURL(new URL("jar:file:myjar.jar!/mypath")));
		assertEquals(new URL("file:/myjar.jar"),
				ResourceUtils.extractJarFileURL(new URL(null, "jar:myjar.jar!/mypath", new DummyURLStreamHandler())));
		assertEquals(new URL("file:myjar.jar"),
				ResourceUtils.extractJarFileURL(new URL(null, "zip:file:myjar.jar!/mypath", new DummyURLStreamHandler())));
		assertEquals(new URL("file:myjar.jar"),
				ResourceUtils.extractJarFileURL(new URL(null, "wsjar:file:myjar.jar!/mypath", new DummyURLStreamHandler())));
		assertEquals(new URL("file:myjar.jar"),
				ResourceUtils.extractJarFileURL(new URL("jar:file:myjar.jar!/")));
		assertEquals(new URL("file:myjar.jar"),
				ResourceUtils.extractJarFileURL(new URL(null, "zip:file:myjar.jar!/", new DummyURLStreamHandler())));
		assertEquals(new URL("file:myjar.jar"),
				ResourceUtils.extractJarFileURL(new URL(null, "wsjar:file:myjar.jar!/", new DummyURLStreamHandler())));
		assertEquals(new URL("file:myjar.jar"),
				ResourceUtils.extractJarFileURL(new URL("file:myjar.jar")));
	}


	/**
	 * Dummy URLStreamHandler that's just specified to suppress the standard
	 * <code>java.net.URL</code> URLStreamHandler lookup, to be able to
	 * use the standard URL class for parsing "rmi:..." URLs.
	 */
	private static class DummyURLStreamHandler extends URLStreamHandler {

		protected URLConnection openConnection(URL url) throws IOException {
			throw new UnsupportedOperationException();
		}
	}

}
