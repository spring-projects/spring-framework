/*
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cglib.core;

import org.springframework.asm.Type;

abstract public class MethodInfo {

	protected MethodInfo() {
	}

	abstract public ClassInfo getClassInfo();
	abstract public int getModifiers();
	abstract public Signature getSignature();
	abstract public Type[] getExceptionTypes();

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (!(o instanceof MethodInfo other)) {
			return false;
		}
		return getSignature().equals(other.getSignature());
	}

	@Override
	public int hashCode() {
		return getSignature().hashCode();
	}

	@Override
	public String toString() {
		// TODO: include modifiers, exceptions
		return getSignature().toString();
	}

}
