/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core.io;

import java.beans.Introspector;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for the {@link ModuleResource} class.
 *
 * @author Juergen Hoeller
 * @since 6.1
 */
class ModuleResourceTests {

	@Test
	void existingResource() throws IOException {
		ModuleResource mr = new ModuleResource(Introspector.class.getModule(), "java/beans/Introspector.class");
		assertThat(mr.exists()).isTrue();
		assertThat(mr.isReadable()).isTrue();
		assertThat(mr.isOpen()).isFalse();
		assertThat(mr.isFile()).isFalse();
		assertThat(mr.getFilename()).isEqualTo("Introspector.class");
		assertThat(mr.getDescription()).contains(mr.getModule().getName());
		assertThat(mr.getDescription()).contains(mr.getPath());

		Resource cpr = new ClassPathResource("java/beans/Introspector.class");
		assertThat(mr.getContentAsByteArray()).isEqualTo(cpr.getContentAsByteArray());
		assertThat(mr.contentLength()).isEqualTo(cpr.contentLength());
	}

	@Test
	void nonExistingResource() {
		ModuleResource mr = new ModuleResource(Introspector.class.getModule(), "java/beans/Introspecter.class");
		assertThat(mr.exists()).isFalse();
		assertThat(mr.isReadable()).isFalse();
		assertThat(mr.isOpen()).isFalse();
		assertThat(mr.isFile()).isFalse();
		assertThat(mr.getFilename()).isEqualTo("Introspecter.class");
		assertThat(mr.getDescription()).contains(mr.getModule().getName());
		assertThat(mr.getDescription()).contains(mr.getPath());

		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(mr::getContentAsByteArray);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(mr::contentLength);
	}

	@Test
	void equalsAndHashCode() {
		Resource mr1 = new ModuleResource(Introspector.class.getModule(), "java/beans/Introspector.class");
		Resource mr2 = new ModuleResource(Introspector.class.getModule(), "java/beans/Introspector.class");
		Resource mr3 = new ModuleResource(Introspector.class.getModule(), "java/beans/Introspecter.class");
		assertThat(mr1).isEqualTo(mr2);
		assertThat(mr1).isNotEqualTo(mr3);
		assertThat(mr1).hasSameHashCodeAs(mr2);
		assertThat(mr1).doesNotHaveSameHashCodeAs(mr3);
	}

}
