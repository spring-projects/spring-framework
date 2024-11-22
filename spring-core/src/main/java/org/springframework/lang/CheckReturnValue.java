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

package org.springframework.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Specifies that the method return value must be used.
 *
 * <p>Inspired by {@code org.jetbrains.annotations.CheckReturnValue}, this variant
 * has been introduced in the {@code org.springframework.lang} package to avoid
 * requiring an extra dependency, while still following similar semantics.
 *
 * <p>This annotation should not be used if the return value of the method
 * provides only <i>additional</i> information. For example, the main purpose
 * of {@link java.util.Collection#add(Object)} is to modify the collection
 * and the return value is only interesting when adding an element to a set,
 * to see if the set already contained that element before.
 *
 * @author Sebastien Deleuze
 * @since 6.2
 */
@Documented
@Target(ElementType.METHOD)
public @interface CheckReturnValue {
}
