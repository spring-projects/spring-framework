/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FileSystemUtils}.
 *
 * @author Rob Harrop
 * @author Sam Brannen
 * @author Juergen Hoeller
 */
class FileSystemUtilsTests {

	@Test
	void deleteRecursively(@TempDir File tempDir) throws Exception {
		File root = new File(tempDir, "root");
		File child = new File(root, "child");
		File grandchild = new File(child, "grandchild");

		grandchild.mkdirs();

		File bar = new File(child, "bar.txt");
		bar.createNewFile();

		assertThat(root).exists();
		assertThat(child).exists();
		assertThat(grandchild).exists();
		assertThat(bar).exists();

		FileSystemUtils.deleteRecursively(root);

		assertThat(root).doesNotExist();
		assertThat(child).doesNotExist();
		assertThat(grandchild).doesNotExist();
		assertThat(bar).doesNotExist();
	}

	@Test
	void copyRecursively(@TempDir File tempDir) throws Exception {
		File src = new File(tempDir, "src");
		File child = new File(src, "child");
		File grandchild = new File(child, "grandchild");

		grandchild.mkdirs();

		File bar = new File(child, "bar.txt");
		bar.createNewFile();

		assertThat(src).exists();
		assertThat(child).exists();
		assertThat(grandchild).exists();
		assertThat(bar).exists();

		File dest = new File(tempDir, "/dest");
		FileSystemUtils.copyRecursively(src, dest);

		assertThat(dest).exists();
		assertThat(new File(dest, "child")).exists();
		assertThat(new File(dest, "child/bar.txt")).exists();

		String destPath = dest.toString().replace('\\', '/');
		if (!destPath.startsWith("/")) {
			destPath = "/" + destPath;
		}
		URI uri = URI.create("jar:file:" + destPath + "/archive.zip");
		Map<String, String> env = Map.of("create", "true");
		FileSystem zipfs = FileSystems.newFileSystem(uri, env);
		Path ziproot = zipfs.getPath("/");
		FileSystemUtils.copyRecursively(src.toPath(), ziproot);

		assertThat(zipfs.getPath("/child")).exists();
		assertThat(zipfs.getPath("/child/bar.txt")).exists();

		zipfs.close();
		FileSystemUtils.deleteRecursively(src);
		assertThat(src).doesNotExist();
	}

}
