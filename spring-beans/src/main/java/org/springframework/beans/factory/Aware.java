/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.beans.factory;

/**
 * Marker superinterface indicating that a bean is eligible to be
 * notified by the Spring container of a particular framework object
 * through a callback-style method.
 *
 *
 * 标记的超级接口，表明bean有资格成为由一个特定框架对象的Spring容器通知通过回叫形式的方法
 *
 * *****我的理解：也就是如果一个bean被这个接口标记了，这个bean就可以收到Spring的通知，通知的形式是通过回调
 *
 *
 *
 * Actual method signature is
 * determined by individual subinterfaces,实际的方法签名是通过单独的子接口完成的
 * but should typically consist of just one void-returning method that accepts a single argument.
 * 但是，通常应该只包含一个接受单个参数的void-返回方法。
 *
 * <p>Note that merely implementing {@link Aware} provides no default
 * functionality.
 * 注意，仅仅实现@link感知不提供缺省值方法
 *
 * Rather, processing must be done explicitly（显式地）, for example
 * in a {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor}.
 * Refer to {@link org.springframework.context.support.ApplicationContextAwareProcessor}
 * and {@link org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory}
 * for examples of processing {@code *Aware} interface callbacks.
 *
 * @author Chris Beams
 * @since 3.1
 */
public interface Aware {

}
