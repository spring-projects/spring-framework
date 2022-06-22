/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.core.testfixture.aot.generate;

import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;

/**
 * Test {@link GenerationContext} implementation that uses
 * {@link TestTarget} as the main target.
 *
 * @author Stephane Nicoll
 */
public class TestGenerationContext extends DefaultGenerationContext {

	public TestGenerationContext(GeneratedFiles generatedFiles) {
		super(new ClassNameGenerator(TestTarget.class), generatedFiles);
	}

	public TestGenerationContext() {
		this(new InMemoryGeneratedFiles());
	}
}
