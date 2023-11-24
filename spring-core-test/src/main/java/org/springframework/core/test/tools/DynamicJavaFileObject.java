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

package org.springframework.core.test.tools;

import java.net.URI;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

/**
 * Adapts a {@link SourceFile} instance to a {@link JavaFileObject}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class DynamicJavaFileObject extends SimpleJavaFileObject {


	private final SourceFile sourceFile;


	DynamicJavaFileObject(SourceFile sourceFile) {
		super(URI.create("java:///" + sourceFile.getPath()), Kind.SOURCE);
		this.sourceFile = sourceFile;
	}


	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) {
		return this.sourceFile.getContent();
	}

}
