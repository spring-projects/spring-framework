/*
 * Copyright 2002-2009 the original author or authors.
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

/**
 * Configuration object for the SpEL expression parser.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.expression.spel.standard.SpelExpressionParser#SpelExpressionParser(SpelParserConfiguration)
 */
public class SpelParserConfiguration {

	private final boolean autoGrowNullReferences;

	private final boolean autoGrowCollections;


	public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections) {
		this.autoGrowNullReferences = autoGrowNullReferences;
		this.autoGrowCollections = autoGrowCollections;
	}


	public boolean isAutoGrowNullReferences() {
		return this.autoGrowNullReferences;
	}

	public boolean isAutoGrowCollections() {
		return this.autoGrowCollections;
	}

}
