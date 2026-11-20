/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.aot;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.javapoet.TypeSpec;
import org.springframework.util.Assert;

/**
 * {@link TypeSpec.Builder} {@link Consumer} that can be used to defer the to
 * another consumer that is set at a later point.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class DeferredTypeBuilder implements Consumer<TypeSpec.Builder> {

	private @Nullable Consumer<TypeSpec.Builder> type;

	@Override
	public void accept(TypeSpec.Builder type) {
		Assert.notNull(this.type, "No type builder set");
		this.type.accept(type);
	}

	public void set(Consumer<TypeSpec.Builder> type) {
		this.type = type;
	}

}
