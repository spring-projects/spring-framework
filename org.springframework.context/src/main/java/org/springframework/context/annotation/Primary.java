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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a bean should be given preference when multiple candidates
 * are qualified to autowire a single-valued dependency. If exactly one 'primary'
 * bean exists among the candidates, it will be the autowired value.
 * 
 * <p>May be used on any class directly or indirectly annotated with
 * {@link org.springframework.stereotype.Component} or on methods annotated
 * with {@link Bean}.
 * 
 * <p>Using {@link Primary} at the class level has no effect unless component-scanning
 * is being used. If a {@link Primary}-annotated class is declared via XML,
 * {@link Primary} annotation metadata is ignored, and
 * {@literal <bean primary="true|false"/>} is respected instead.
 * 
 * @author Chris Beams
 * @since 3.0
 * @see Lazy
 * @see Bean
 * @see org.springframework.stereotype.Component
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Primary {

}
