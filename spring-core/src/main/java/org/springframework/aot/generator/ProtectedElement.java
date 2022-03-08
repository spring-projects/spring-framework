/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.generator;

import java.lang.reflect.Member;

import org.springframework.lang.Nullable;

/**
 * A {@link Member} that is non-public, with the related type.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class ProtectedElement {

	private final Class<?> type;

	@Nullable
	private final Member target;


	private ProtectedElement(Class<?> type, @Nullable Member member) {
		this.type = type;
		this.target = member;
	}

	/**
	 * Return the {@link Class type} that is non-public. For a plain
	 * protected {@link Member member} access, the type of the declaring class
	 * is used. Otherwise, the type in the member signature, such as a parameter
	 * type for an executable, or the return type of a field is used. If the
	 * type is generic, the type that is protected in the generic signature is
	 * used.
	 * @return the type that is not public
	 */
	public Class<?> getType() {
		return this.type;
	}

	/**
	 * Return the {@link Member} that is not publicly accessible.
	 * @return the member
	 */
	@Nullable
	public Member getMember() {
		return this.target;
	}

	static ProtectedElement of(Class<?> type, @Nullable Member member) {
		return new ProtectedElement(type, member);
	}

}
