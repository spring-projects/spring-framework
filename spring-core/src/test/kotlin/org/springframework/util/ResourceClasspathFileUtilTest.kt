package org.springframework.util

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class ResourceClasspathFileUtilTest {

	@Test
	fun correctReadFromClasspath() {
		val file = ResourceClasspathFileUtil("org/springframework/util/classpathfileutildata.txt")
		Assertions.assertThat(file.readText()).isEqualTo("Hello Spring!")
	}
}