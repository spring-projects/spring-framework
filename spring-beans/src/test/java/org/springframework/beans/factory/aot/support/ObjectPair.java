/*
 * Copyright 2024-2024 the original author or authors.
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

package org.springframework.beans.factory.aot.support;

import java.util.Objects;

/**
 * Test class for testing {@link org.springframework.aot.generate.ValueCodeGenerator}
 * with generic object pairs.
 *
 * @author Christopher Chianelli
 */
public class ObjectPair<L, R> {
	L left;
	R right;

	public ObjectPair() {
	}

	public ObjectPair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	public L getLeft() {
		return left;
	}

	public void setLeft(L left) {
		this.left = left;
	}

	public R getRight() {
		return right;
	}

	public void setRight(R right) {
		this.right = right;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (object == null || getClass() != object.getClass())
			return false;
		ObjectPair<?, ?> that = (ObjectPair<?, ?>) object;
		return Objects.equals(left, that.left) && Objects.equals(right, that.right);
	}

	@Override
	public int hashCode() {
		return Objects.hash(left, right);
	}
}
