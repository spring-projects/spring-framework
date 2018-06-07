/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that the annotated element represents a stereotype for the index.
 *
 * 指示注释元素表示索引的构造型。
 *
 * <p>The {@code CandidateComponentsIndex} is an alternative to classpath
 * scanning that uses a metadata file generated at compilation time. The
 * index allows retrieving the candidate components (i.e. fully qualified
 * name) based on a stereotype. This annotation instructs the generator to
 * index the element on which the annotated element is present or if it
 * implements or extends from the annotated element. The stereotype is the
 * fully qualified name of the annotated element.
 *
 * <p> CandidateComponentsIndex是类路径扫描的替代方法，它使用编译时生成的元数据文件。
 * 该索引允许基于刻板印象检索候选组件（即全限定名称）。
 * 此注解指示生成器对注释元素所在的元素进行索引，或者如果它实现或从注释元素扩展。 原型是注释元素的完全限定名称。
 *
 * <p>Consider the default {@link Component} annotation that is meta-annotated
 * with this annotation. If a component is annotated with {@link Component},
 * an entry for that component will be added to the index using the
 * {@code org.springframework.stereotype.Component} stereotype.
 *
 * 考虑使用此批注进行元注释的默认{@link Component}批注。
 * 如果组件使用{@link Component}注释，
 * 则该组件的条目将使用org.springframework.stereotype.Component构造型添加到索引中。
 *
 * <p>This annotation is also honored on meta-annotations. Consider this
 * custom annotation:
 * <pre class="code">
 * package com.example;
 *
 * &#064;Target(ElementType.TYPE)
 * &#064;Retention(RetentionPolicy.RUNTIME)
 * &#064;Documented
 * &#064;Indexed
 * &#064;Service
 * public @interface PrivilegedService { ... }
 * </pre>
 *
 * If the above annotation is present on a type, it will be indexed with two
 * stereotypes: {@code org.springframework.stereotype.Component} and
 * {@code com.example.PrivilegedService}. While {@link Service} isn't directly
 * annotated with {@code Indexed}, it is meta-annotated with {@link Component}.
 *
 * 如果上述注释出现在某个类型上，则它将使用两种构造型索引：
 * {@code org.springframework.stereotype.Component}和{@code com.example.PrivilegedService}。
 * 虽然{@link Service}没有直接使用Indexed}进行注释，但是使用{@link Component}进行元注释。
 *
 * <p>It is also possible to index all implementations of a certain interface or
 * all the subclasses of a given class by adding {@code @Indexed} on it.
 *
 * Consider this base interface:
 * <pre class="code">
 * package com.example;
 *
 * &#064;Indexed
 * public interface AdminService { ... }
 * </pre>
 *
 * Now, consider an implementation of this {@code AdminService} somewhere:
 * <pre class="code">
 * package com.example.foo;
 *
 * import com.example.AdminService;
 *
 * public class ConfigurationAdminService implements AdminService { ... }
 * </pre>
 *
 * Because this class implements an interface that is indexed, it will be
 * automatically included with the {@code com.example.AdminService} stereotype.
 * If there are more {@code @Indexed} interfaces and/or superclasses in the
 * hierarchy, the class will map to all their stereotypes.
 *
 * 由于此类实现了已编制索引的接口，因此它将自动包含在com.example.AdminService构造型中。
 * 如果层次结构中有更多{@Indexed}接口和/或超类，则该类将映射到所有的构造型。
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Indexed {
}
