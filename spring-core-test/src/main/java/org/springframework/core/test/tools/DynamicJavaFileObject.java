/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
