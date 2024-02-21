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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a bean qualifies as a fallback autowire candidate.
 * This is a companion and alternative to the {@link Primary} annotation.
 *
 * <p>If all beans but one among multiple matching candidates are marked
 * as a fallback, the remaining bean will be selected.
 *
 * <p>Just like primary beans, fallback beans only have an effect when
 * finding multiple candidates for single injection points.
 * All type-matching beans are included when autowiring arrays,
 * collections, maps, or ObjectProvider streams.
 *
 * @author Juergen Hoeller
 * @since 6.2
 * @see Primary
 * @see Lazy
 * @see Bean
 * @see org.springframework.beans.factory.config.BeanDefinition#setFallback
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Fallback {

}
