/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class ResourceUtilsTests {

	@Test
	@SuppressWarnings("deprecation")  // for deprecated URL constructor on JDK 20
	void isJarURL() throws Exception {
		assertThat(ResourceUtils.isJarURL(new URL("jar:file:myjar.jar!/mypath"))).isTrue();
		assertThat(ResourceUtils.isJarURL(new URL(null, "zip:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isTrue();
		assertThat(ResourceUtils.isJarURL(new URL(null, "wsjar:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isTrue();
		assertThat(ResourceUtils.isJarURL(new URL(null, "jar:war:file:mywar.war*/myjar.jar!/mypath", new DummyURLStreamHandler()))).isTrue();
		assertThat(ResourceUtils.isJarURL(new URL("file:myjar.jar"))).isFalse();
		assertThat(ResourceUtils.isJarURL(new URL("http:myserver/myjar.jar"))).isFalse();
	}

	@Test
	@SuppressWarnings("deprecation")  // for deprecated URL constructor on JDK 20
	void extractJarFileURL() throws Exception {
		assertThat(ResourceUtils.extractJarFileURL(new URL("jar:file:myjar.jar!/mypath"))).isEqualTo(new URL("file:myjar.jar"));
		assertThat(ResourceUtils.extractJarFileURL(new URL(null, "jar:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(new URL("file:/myjar.jar"));
		assertThat(ResourceUtils.extractJarFileURL(new URL(null, "zip:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(new URL("file:myjar.jar"));
		assertThat(ResourceUtils.extractJarFileURL(new URL(null, "wsjar:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(new URL("file:myjar.jar"));

		assertThat(ResourceUtils.extractJarFileURL(new URL("file:myjar.jar"))).isEqualTo(new URL("file:myjar.jar"));
		assertThat(ResourceUtils.extractJarFileURL(new URL("jar:file:myjar.jar!/"))).isEqualTo(new URL("file:myjar.jar"));
		assertThat(ResourceUtils.extractJarFileURL(new URL(null, "zip:file:myjar.jar!/", new DummyURLStreamHandler()))).isEqualTo(new URL("file:myjar.jar"));
		assertThat(ResourceUtils.extractJarFileURL(new URL(null, "wsjar:file:myjar.jar!/", new DummyURLStreamHandler()))).isEqualTo(new URL("file:myjar.jar"));
	}

	@Test
	@SuppressWarnings("deprecation")  // for deprecated URL constructor on JDK 20
	void extractArchiveURL() throws Exception {
		assertThat(ResourceUtils.extractArchiveURL(new URL("jar:file:myjar.jar!/mypath"))).isEqualTo(new URL("file:myjar.jar"));
		assertThat(ResourceUtils.extractArchiveURL(new URL(null, "jar:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(new URL("file:/myjar.jar"));
		assertThat(ResourceUtils.extractArchiveURL(new URL(null, "zip:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(new URL("file:myjar.jar"));
		assertThat(ResourceUtils.extractArchiveURL(new URL(null, "wsjar:file:myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(new URL("file:myjar.jar"));
		assertThat(ResourceUtils.extractArchiveURL(new URL(null, "jar:war:file:mywar.war*/myjar.jar!/mypath", new DummyURLStreamHandler()))).isEqualTo(new URL("file:mywar.war"));

		assertThat(ResourceUtils.extractArchiveURL(new URL("file:myjar.jar"))).isEqualTo(new URL("file:myjar.jar"));
		assertThat(ResourceUtils.extractArchiveURL(new URL("jar:file:myjar.jar!/"))).isEqualTo(new URL("file:myjar.jar"));
		assertThat(ResourceUtils.extractArchiveURL(new URL(null, "zip:file:myjar.jar!/", new DummyURLStreamHandler()))).isEqualTo(new URL("file:myjar.jar"));
		assertThat(ResourceUtils.extractArchiveURL(new URL(null, "wsjar:file:myjar.jar!/", new DummyURLStreamHandler()))).isEqualTo(new URL("file:myjar.jar"));
		assertThat(ResourceUtils.extractArchiveURL(new URL(null, "jar:war:file:mywar.war*/myjar.jar!/", new DummyURLStreamHandler()))).isEqualTo(new URL("file:mywar.war"));
	}


	/**
	 * Dummy URLStreamHandler that's just specified to suppress the standard
	 * {@code java.net.URL} URLStreamHandler lookup, to be able to
	 * use the standard URL class for parsing "rmi:..." URLs.
	 */
	private static class DummyURLStreamHandler extends URLStreamHandler {

		@Override
		protected URLConnection openConnection(URL url) {
			throw new UnsupportedOperationException();
		}
	}

}
