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

package org.springframework.core.io.support;

import java.io.IOException;
import java.util.List;

/**
 * Class packaged into a temporary jar to test
 * {@link PathMatchingResourcePatternResolver} detection of classpath manifest
 * entries.
 *
 * @author Phillip Webb
 */
public class ClassPathManifestEntriesTestApplication {

	public static void main(String[] args) throws IOException {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		System.out.println("!!!!" + List.of(resolver.getResources("classpath*:/**/*.txt")));
	}

}
