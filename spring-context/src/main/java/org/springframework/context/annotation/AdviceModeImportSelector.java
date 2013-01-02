 /*
 * Copyright 2002-2012 the original author or authors.
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

import java.lang.annotation.Annotation;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

import static org.springframework.context.annotation.MetadataUtils.*;

/**
 * Convenient base class for {@link ImportSelector} implementations that select imports
 * based on an {@link AdviceMode} value from an annotation (such as the {@code @Enable*}
 * annotations).
 *
 * @param <A> Annotation containing {@linkplain #getAdviceModeAttributeName() AdviceMode
 * attribute}
 *
 * @author Chris Beams
 * @since 3.1
 */
public abstract class AdviceModeImportSelector<A extends Annotation> implements ImportSelector {

	public static final String DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME = "mode";

	/**
	 * The name of the {@link AdviceMode} attribute for the annotation specified by the
	 * generic type {@code A}. The default is {@value #DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME},
	 * but subclasses may override in order to customize.
	 */
	protected String getAdviceModeAttributeName() {
		return DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>This implementation resolves the type of annotation from generic metadata and
	 * validates that (a) the annotation is in fact present on the importing
	 * {@code @Configuration} class and (b) that the given annotation has an
	 * {@linkplain #getAdviceModeAttributeName() advice mode attribute} of type
	 * {@link AdviceMode}.
	 *
	 * <p>The {@link #selectImports(AdviceMode)} method is then invoked, allowing the
	 * concrete implementation to choose imports in a safe and convenient fashion.
	 *
	 * @throws IllegalArgumentException if expected annotation {@code A} is not present
	 * on the importing {@code @Configuration} class or if {@link #selectImports(AdviceMode)}
	 * returns {@code null}
	 */
	public final String[] selectImports(AnnotationMetadata importingClassMetadata) {
		Class<?> annoType = GenericTypeResolver.resolveTypeArgument(this.getClass(), AdviceModeImportSelector.class);

		AnnotationAttributes attributes = attributesFor(importingClassMetadata, annoType);
		Assert.notNull(attributes, String.format(
				"@%s is not present on importing class '%s' as expected",
				annoType.getSimpleName(), importingClassMetadata.getClassName()));

		AdviceMode adviceMode = attributes.getEnum(this.getAdviceModeAttributeName());

		String[] imports = selectImports(adviceMode);
		Assert.notNull(imports, String.format("Unknown AdviceMode: '%s'", adviceMode));

		return imports;
	}

	/**
	 * Determine which classes should be imported based on the given {@code AdviceMode}.
	 *
	 * <p>Returning {@code null} from this method indicates that the {@code AdviceMode} could
	 * not be handled or was unknown and that an {@code IllegalArgumentException} should
	 * be thrown.
	 *
	 * @param adviceMode the value of the {@linkplain #getAdviceModeAttributeName()
	 * advice mode attribute} for the annotation specified via generics.
	 *
	 * @return array containing classes to import; empty array if none, {@code null} if
	 * the given {@code AdviceMode} is unknown.
	 */
	protected abstract String[] selectImports(AdviceMode adviceMode);

}
