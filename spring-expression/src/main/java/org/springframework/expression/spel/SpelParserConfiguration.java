/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.spel;

import org.springframework.core.SpringProperties;


/**
 * Configuration object for the SpEL expression parser.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Andy Clement
 * @since 3.0
 * @see org.springframework.expression.spel.standard.SpelExpressionParser#SpelExpressionParser(SpelParserConfiguration)
 */
public class SpelParserConfiguration {

	private final boolean autoGrowNullReferences;

	private final boolean autoGrowCollections;
	
	private static SpelCompilerMode defaultCompilerMode = SpelCompilerMode.off;
	
	private SpelCompilerMode compilerMode;

	private final int maximumAutoGrowSize;

	static {
		String compilerMode = SpringProperties.getProperty("spring.expression.compiler.mode");
		if (compilerMode != null) {
			defaultCompilerMode = SpelCompilerMode.valueOf(compilerMode.toLowerCase());
			// System.out.println("SpelCompiler: switched to "+defaultCompilerMode+" mode");
		}
	}
	
	/**
	 * Create a new {@link SpelParserConfiguration} instance.
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @see #SpelParserConfiguration(boolean, boolean, int)
	 */
	public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections) {
		this(autoGrowNullReferences, autoGrowCollections, Integer.MAX_VALUE);
	}

	/**
	 * Create a new {@link SpelParserConfiguration} instance.
	 * @param autoGrowNullReferences if null references should automatically grow
	 * @param autoGrowCollections if collections should automatically grow
	 * @param maximumAutoGrowSize the maximum size that the collection can auto grow
	 */
	public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize) {
		this.autoGrowNullReferences = autoGrowNullReferences;
		this.autoGrowCollections = autoGrowCollections;
		this.maximumAutoGrowSize = maximumAutoGrowSize;
		this.compilerMode = defaultCompilerMode;
	}
	
	/**
	 * @param compilerMode the compiler mode that parsers using this configuration object should use
	 */
	public void setCompilerMode(SpelCompilerMode compilerMode) {
		this.compilerMode = compilerMode;
	}

	/**
	 * @return the configuration mode for parsers using this configuration object
	 */
	public SpelCompilerMode getCompilerMode() {
		return this.compilerMode;
	}

	/**
	 * @return {@code true} if {@code null} references should be automatically grown
	 */
	public boolean isAutoGrowNullReferences() {
		return this.autoGrowNullReferences;
	}

	/**
	 * @return {@code true} if collections should be automatically grown
	 */
	public boolean isAutoGrowCollections() {
		return this.autoGrowCollections;
	}

	/**
	 * @return the maximum size that a collection can auto grow
	 */
	public int getMaximumAutoGrowSize() {
		return this.maximumAutoGrowSize;
	}

}
