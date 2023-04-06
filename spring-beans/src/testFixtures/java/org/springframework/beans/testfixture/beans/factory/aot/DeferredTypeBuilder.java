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

package org.springframework.beans.testfixture.beans.factory.aot;

import java.util.function.Consumer;

import org.springframework.javapoet.TypeSpec;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link TypeSpec.Builder} {@link Consumer} that can be used to defer the to
 * another consumer that is set at a later point.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class DeferredTypeBuilder implements Consumer<TypeSpec.Builder> {

	@Nullable
	private Consumer<TypeSpec.Builder> type;

	@Override
	public void accept(TypeSpec.Builder type) {
		Assert.notNull(this.type, "No type builder set");
		this.type.accept(type);
	}

	public void set(Consumer<TypeSpec.Builder> type) {
		this.type = type;
	}

}
