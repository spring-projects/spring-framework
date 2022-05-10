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

package org.springframework.aot.generate;

/**
 * Generates new {@link GeneratedMethod} instances.
 *
 * @author Phillip Webb
 * @since 6.0
 * @see GeneratedMethods
 */
@FunctionalInterface
public interface MethodGenerator {

	/**
	 * Generate a new {@link GeneratedMethod}. The returned instance must define
	 * the method spec by calling {@code using(builder -> ...)}.
	 * @param methodNameParts the method name parts that should be used to
	 * generate a unique method name
	 * @return the newly added {@link GeneratedMethod}
	 */
	GeneratedMethod generateMethod(Object... methodNameParts);

	/**
	 * Return a new {@link MethodGenerator} instance that generates method with
	 * additional implicit method name parts. The final generated name will be
	 * of the following form:
	 * <p>
	 * <table border="1">
	 * <tr>
	 * <th>Original</th>
	 * <th>Updated</th>
	 * </tr>
	 * <tr>
	 * <td>run</td>
	 * <td>&lt;name&gt;Run</td>
	 * </tr>
	 * <tr>
	 * <td>getValue</td>
	 * <td>get&lt;Name&gt;Value</td>
	 * </tr>
	 * <tr>
	 * <td>setValue</td>
	 * <td>set&lt;Name&gt;Value</td>
	 * </tr>
	 * <tr>
	 * <td>isEnabled</td>
	 * <td>is&lt;Name&gt;Enabled</td>
	 * </tr>
	 * </table>
	 * @param nameParts the implicit name parts
	 * @return a new {@link MethodGenerator} instance
	 */
	default MethodGenerator withName(Object... nameParts) {
		return new MethodGeneratorWithName(this, nameParts);
	}

}
