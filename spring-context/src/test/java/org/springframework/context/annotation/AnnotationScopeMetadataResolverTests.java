/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.context.annotation.ScopedProxyMode.INTERFACES;
import static org.springframework.context.annotation.ScopedProxyMode.NO;
import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;

/**
 * Tests for {@link AnnotationScopeMetadataResolver}.
 *
 * @author Rick Evans
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class AnnotationScopeMetadataResolverTests {

	private AnnotationScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();


	@Test
	void resolveScopeMetadataShouldNotApplyScopedProxyModeToSingleton() {
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(AnnotatedWithSingletonScope.class);
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(bd);
		assertThat(scopeMetadata).as("resolveScopeMetadata(..) must *never* return null.").isNotNull();
		assertThat(scopeMetadata.getScopeName()).isEqualTo(BeanDefinition.SCOPE_SINGLETON);
		assertThat(scopeMetadata.getScopedProxyMode()).isEqualTo(NO);
	}

	@Test
	void resolveScopeMetadataShouldApplyScopedProxyModeToPrototype() {
		this.scopeMetadataResolver = new AnnotationScopeMetadataResolver(INTERFACES);
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(AnnotatedWithPrototypeScope.class);
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(bd);
		assertThat(scopeMetadata).as("resolveScopeMetadata(..) must *never* return null.").isNotNull();
		assertThat(scopeMetadata.getScopeName()).isEqualTo(BeanDefinition.SCOPE_PROTOTYPE);
		assertThat(scopeMetadata.getScopedProxyMode()).isEqualTo(INTERFACES);
	}

	@Test
	void resolveScopeMetadataShouldReadScopedProxyModeFromAnnotation() {
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(AnnotatedWithScopedProxy.class);
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(bd);
		assertThat(scopeMetadata).as("resolveScopeMetadata(..) must *never* return null.").isNotNull();
		assertThat(scopeMetadata.getScopeName()).isEqualTo("request");
		assertThat(scopeMetadata.getScopedProxyMode()).isEqualTo(TARGET_CLASS);
	}

	@Test
	void customRequestScope() {
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(AnnotatedWithCustomRequestScope.class);
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(bd);
		assertThat(scopeMetadata).as("resolveScopeMetadata(..) must *never* return null.").isNotNull();
		assertThat(scopeMetadata.getScopeName()).isEqualTo("request");
		assertThat(scopeMetadata.getScopedProxyMode()).isEqualTo(NO);
	}

	@Test
	void customRequestScopeViaAsm() throws IOException {
		MetadataReaderFactory readerFactory = new SimpleMetadataReaderFactory();
		MetadataReader reader = readerFactory.getMetadataReader(AnnotatedWithCustomRequestScope.class.getName());
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(reader.getAnnotationMetadata());
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(bd);
		assertThat(scopeMetadata).as("resolveScopeMetadata(..) must *never* return null.").isNotNull();
		assertThat(scopeMetadata.getScopeName()).isEqualTo("request");
		assertThat(scopeMetadata.getScopedProxyMode()).isEqualTo(NO);
	}

	@Test
	void customRequestScopeWithAttribute() {
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(
			AnnotatedWithCustomRequestScopeWithAttributeOverride.class);
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(bd);
		assertThat(scopeMetadata).as("resolveScopeMetadata(..) must *never* return null.").isNotNull();
		assertThat(scopeMetadata.getScopeName()).isEqualTo("request");
		assertThat(scopeMetadata.getScopedProxyMode()).isEqualTo(TARGET_CLASS);
	}

	@Test
	void customRequestScopeWithAttributeViaAsm() throws IOException {
		MetadataReaderFactory readerFactory = new SimpleMetadataReaderFactory();
		MetadataReader reader = readerFactory.getMetadataReader(AnnotatedWithCustomRequestScopeWithAttributeOverride.class.getName());
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(reader.getAnnotationMetadata());
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(bd);
		assertThat(scopeMetadata).as("resolveScopeMetadata(..) must *never* return null.").isNotNull();
		assertThat(scopeMetadata.getScopeName()).isEqualTo("request");
		assertThat(scopeMetadata.getScopedProxyMode()).isEqualTo(TARGET_CLASS);
	}

	@Test
	void ctorWithNullScopedProxyMode() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new AnnotationScopeMetadataResolver(null));
	}

	@Test
	void setScopeAnnotationTypeWithNullType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				scopeMetadataResolver.setScopeAnnotationType(null));
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Scope("request")
	@interface CustomRequestScope {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Scope("request")
	@interface CustomRequestScopeWithAttributeOverride {

		@AliasFor(annotation = Scope.class)
		ScopedProxyMode proxyMode();
	}

	@Scope("singleton")
	private static class AnnotatedWithSingletonScope {
	}

	@Scope("prototype")
	private static class AnnotatedWithPrototypeScope {
	}

	@Scope(scopeName = "request", proxyMode = TARGET_CLASS)
	private static class AnnotatedWithScopedProxy {
	}

	@CustomRequestScope
	private static class AnnotatedWithCustomRequestScope {
	}

	@CustomRequestScopeWithAttributeOverride(proxyMode = TARGET_CLASS)
	private static class AnnotatedWithCustomRequestScopeWithAttributeOverride {
	}

}
