/*
 * Copyright 2002-present the original author or authors.
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

/**
 * Common enum for indicating a desired proxy type.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see Proxyable#value()
 */
public enum ProxyType {

	/**
	 * Default is a JDK dynamic proxy, or potentially a class-based CGLIB proxy
	 * when globally configured.
	 */
	DEFAULT,

	/**
	 * Suggest a JDK dynamic proxy implementing <i>all</i> interfaces exposed by
	 * the class of the target object. Overrides a globally configured default.
	 */
	INTERFACES,

	/**
	 * Suggest a class-based CGLIB proxy. Overrides a globally configured default.
	 */
	TARGET_CLASS

}
