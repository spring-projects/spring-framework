/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 */
class FileSystemUtilsTests {

	@Test
	void deleteRecursively() throws Exception {
		File root = new File("./tmp/root");
		File child = new File(root, "child");
		File grandchild = new File(child, "grandchild");

		grandchild.mkdirs();

		File bar = new File(child, "bar.txt");
		bar.createNewFile();

		assertThat(root.exists()).isTrue();
		assertThat(child.exists()).isTrue();
		assertThat(grandchild.exists()).isTrue();
		assertThat(bar.exists()).isTrue();

		FileSystemUtils.deleteRecursively(root);

		assertThat(root.exists()).isFalse();
		assertThat(child.exists()).isFalse();
		assertThat(grandchild.exists()).isFalse();
		assertThat(bar.exists()).isFalse();
	}

	@Test
	void copyRecursively() throws Exception {
		File src = new File("./tmp/src");
		File child = new File(src, "child");
		File grandchild = new File(child, "grandchild");

		grandchild.mkdirs();

		File bar = new File(child, "bar.txt");
		bar.createNewFile();

		assertThat(src.exists()).isTrue();
		assertThat(child.exists()).isTrue();
		assertThat(grandchild.exists()).isTrue();
		assertThat(bar.exists()).isTrue();

		File dest = new File("./dest");
		FileSystemUtils.copyRecursively(src, dest);

		assertThat(dest.exists()).isTrue();
		assertThat(new File(dest, child.getName()).exists()).isTrue();

		FileSystemUtils.deleteRecursively(src);
		assertThat(src.exists()).isFalse();
	}


	@AfterEach
	void tearDown() throws Exception {
		File tmp = new File("./tmp");
		if (tmp.exists()) {
			FileSystemUtils.deleteRecursively(tmp);
		}
		File dest = new File("./dest");
		if (dest.exists()) {
			FileSystemUtils.deleteRecursively(dest);
		}
	}

}
