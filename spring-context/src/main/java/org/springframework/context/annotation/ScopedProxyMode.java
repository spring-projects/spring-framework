/*
 * Copyright 2002-2013 the original author or authors.
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

/**
 * Enumerates the various scoped-proxy options.
 *
 * 枚举各种范围代理选项
 *
 * <p>For a more complete discussion of exactly what a scoped proxy is, see the
 * section of the Spring reference documentation entitled '<em>Scoped beans as
 * dependencies</em>'.
 *
 * <p>有关范围代理的确切内容的更完整讨论，请参阅Spring参考文档中名为'<em>作为依赖关系的范围'</ em>'的部分。
 *
 * @author Mark Fisher
 * @since 2.5
 * @see ScopeMetadata
 */
public enum ScopedProxyMode {

	/**
	 * Default typically equals {@link #NO}, unless a different default
	 * has been configured at the component-scan instruction level.
	 *
	 * 默认情况下，通常等于{@link #NO}，除非在组件扫描指令级别配置了不同的默认值。
	 *
	 */
	DEFAULT,

	/**
	 * Do not create a scoped proxy.
	 *
	 * 不创建代理
	 *
	 * <p>This proxy-mode is not typically useful when used with a
	 * non-singleton scoped instance, which should favor the use of the
	 * {@link #INTERFACES} or {@link #TARGET_CLASS} proxy-modes instead if it
	 * is to be used as a dependency.
	 */
	NO,

	/**
	 * Create a JDK dynamic proxy implementing <i>all</i> interfaces exposed by
	 * the class of the target object.
	 *
	 * 创建一个JDK动态代理，实现目标对象的类公开的<i>所有</ i>接口。
	 *
	 */
	INTERFACES,

	/**
	 * Create a class-based proxy (uses CGLIB).
	 *
	 * 创建一个基于类的代理（使用CGLIB）。
	 *
	 */
	TARGET_CLASS;

}
