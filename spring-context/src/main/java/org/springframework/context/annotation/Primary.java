/*
 * Copyright 2002-2016 the original author or authors.
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a bean should be given preference when multiple candidates
 * are qualified to autowire a single-valued dependency. If exactly one
 * 'primary' bean exists among the candidates, it will be the autowired value.
 *
 * <p>This annotation is semantically equivalent to the {@code <bean>} element's
 * {@code primary} attribute in Spring XML.
 *
 * <p>May be used on any class directly or indirectly annotated with
 * {@code @Component} or on methods annotated with @{@link Bean}.
 *
 * <h2>Example</h2>
 * <pre class="code">
 * &#064;Component
 * public class FooService {
 *
 *     private FooRepository fooRepository;
 *
 *     &#064;Autowired
 *     public FooService(FooRepository fooRepository) {
 *         this.fooRepository = fooRepository;
 *     }
 * }
 *
 * &#064;Component
 * public class JdbcFooRepository {
 *
 *     public JdbcFooService(DataSource dataSource) {
 *         // ...
 *     }
 * }
 *
 * &#064;Primary
 * &#064;Component
 * public class HibernateFooRepository {
 *
 *     public HibernateFooService(SessionFactory sessionFactory) {
 *         // ...
 *     }
 * }
 * </pre>
 *
 * <p>Because {@code HibernateFooRepository} is marked with {@code @Primary},
 * it will be injected preferentially over the jdbc-based variant assuming both
 * are present as beans within the same Spring application context, which is
 * often the case when component-scanning is applied liberally.
 *
 * <p>Note that using {@code @Primary} at the class level has no effect unless
 * component-scanning is being used. If a {@code @Primary}-annotated class is
 * declared via XML, {@code @Primary} annotation metadata is ignored, and
 * {@code <bean primary="true|false"/>} is respected instead.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see Lazy
 * @see Bean
 * @see ComponentScan
 * @see org.springframework.stereotype.Component
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Primary {

}
