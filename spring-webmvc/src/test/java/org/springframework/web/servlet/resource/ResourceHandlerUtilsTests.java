/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.servlet.resource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResourceHandlerUtils}.
 *
 * @author Edoardo Patti
 */
public class ResourceHandlerUtilsTests {

	@Test
	void testTrailingSlash() {
		var windowsDirectory = "META-INF\\resources\\webjars";
		var directory = "META-INF/resources/webjars";
		var directoryWithoutSlash = "META-INF";
		var correctWindowsDirectory = "META-INF\\resources\\webjars\\";
		var correctDirectory = "META-INF/resources/webjars/";
		assertThat(ResourceHandlerUtils.addTrailingSlashIfAbsent(windowsDirectory).endsWith("\\")).isTrue();
		assertThat(ResourceHandlerUtils.addTrailingSlashIfAbsent(directory).endsWith("/")).isTrue();
		assertThat(ResourceHandlerUtils.addTrailingSlashIfAbsent(directoryWithoutSlash).endsWith("/")).isTrue();
		assertThat(ResourceHandlerUtils.addTrailingSlashIfAbsent(correctWindowsDirectory)).isEqualTo(correctWindowsDirectory);
		assertThat(ResourceHandlerUtils.addTrailingSlashIfAbsent(correctDirectory)).isEqualTo(correctDirectory);
	}
}
