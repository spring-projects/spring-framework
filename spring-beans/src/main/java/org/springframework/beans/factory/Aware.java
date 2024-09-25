/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.beans.factory;

/**
 * A marker superinterface indicating that a bean is eligible to be notified by the
 * Spring container of a particular framework object through a callback-style method.
 * The actual method signature is determined by individual subinterfaces but should
 * typically consist of just one void-returning method that accepts a single argument.
 *
 * <p>Note that merely implementing {@link Aware} provides no default functionality.
 * Rather, processing must be done explicitly, for example in a
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}.
 * Refer to {@link org.springframework.context.support.ApplicationContextAwareProcessor}
 * for an example of processing specific {@code *Aware} interface callbacks.
 *
 * 中文解释：
 * 这个接口的作用是标记接口（marker interface），用于指示一个类具备某种特定的能力或属性。
 * 在这个特定的接口Aware中，并没有定义任何方法或属性，它只是一个空接口。
 * 在Spring框架中，Aware接口被用作其他接口的父接口，以表示实现这些子接口的类具备某种特定的能力。
 * 具体来说，实现了Aware接口的类可以通过回调方法接收到Spring容器传递的相关信息。
 * 例如，BeanFactoryAware接口继承了Aware接口，表示实现了BeanFactoryAware接口的类具备获取BeanFactory的能力。
 * 当一个Bean实例实现了BeanFactoryAware接口后，Spring容器在创建该Bean实例时会调用setBeanFactory方法，将所属的BeanFactory对象传递给该实例。
 * 总的来说，Aware接口的作用是作为一个标记接口，用于表示实现了该接口的类具备某种特定的能力或属性。
 * 具体的功能和行为是由继承了Aware接口的子接口来定义和实现的。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public interface Aware {

}
