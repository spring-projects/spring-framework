/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link AliasFors} is a supplement to {@link AliasFor}.
 * Originally, it could only be a single alias for each other,
 * but now supports multiple aliases.
 * <p>
 * This is a example of using {@link AliasFors}:
 * <pre class="code">
 * public &#064;interface Father {
 *
 *    &#064;AliasFor(annotation = NameConfig.class, attribute = "name")
 *    String name() default "";
 * }
 *
 * public &#064;interface Teacher {
 *
 *    &#064;AliasFor(annotation = NameConfig.class, attribute = "name")
 *    String name() default "";
 * }
 *
 * &#064;Teacher
 * &#064;Father
 * public &#064;interface People {
 *
 *    &#064;AliasFor(annotation = Teacher.class, attribute = "name")
 *    &#064;AliasFor(annotation = Father.class, attribute = "name")
 *    String name() default "";
 * }</pre>
 *
 * @author Zicheng Zhang
 * @date 2020/08/08
 * @see AliasFor
 * @since 4.3.28
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface AliasFors {

    AliasFor[] value() default {};

}
