/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.beans.factory;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

public class ImportAwareBeanRegistrar implements BeanRegistrar, ImportAware {

	@Nullable
	private AnnotationMetadata importMetadata;

	@Override
	public void register(BeanRegistry registry, Environment env) {
		registry.registerBean(ClassNameHolder.class, spec -> spec.supplier(context ->
				new ClassNameHolder(this.importMetadata == null ? null : this.importMetadata.getClassName())));
	}

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.importMetadata = importMetadata;
	}

	public @Nullable AnnotationMetadata getImportMetadata() {
		return this.importMetadata;
	}

	public record ClassNameHolder(@Nullable String className) {}

}
