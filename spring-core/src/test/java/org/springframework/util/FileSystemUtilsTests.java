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

package org.springframework.util;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FileSystemUtils}.
 *
 * @author Rob Harrop
 * @author Sam Brannen
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
		assertThat(new File(dest, child.getName())).exists();

		FileSystemUtils.deleteRecursively(src);
		assertThat(src).doesNotExist();
	}

}
