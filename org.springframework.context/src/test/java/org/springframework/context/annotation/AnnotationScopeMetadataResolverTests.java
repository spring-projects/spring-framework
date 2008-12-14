package org.springframework.context.annotation;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Unit tests for the {@link AnnotationScopeMetadataResolver} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public final class AnnotationScopeMetadataResolverTests {

	private AnnotationScopeMetadataResolver scopeMetadataResolver;


	@Before
	public void setUp() throws Exception {
		this.scopeMetadataResolver = new AnnotationScopeMetadataResolver();
	}


	@Test
	public void testThatResolveScopeMetadataDoesNotApplyScopedProxyModeToASingleton() {
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(AnnotatedWithSingletonScope.class);
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(bd);
		assertNotNull("resolveScopeMetadata(..) must *never* return null.", scopeMetadata);
		assertEquals(BeanDefinition.SCOPE_SINGLETON, scopeMetadata.getScopeName());
		assertEquals(ScopedProxyMode.NO, scopeMetadata.getScopedProxyMode());
	}


	@Test
	public void testThatResolveScopeMetadataDoesApplyScopedProxyModeToAPrototype() {
		this.scopeMetadataResolver = new AnnotationScopeMetadataResolver(ScopedProxyMode.INTERFACES);
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(AnnotatedWithPrototypeScope.class);
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(bd);
		assertNotNull("resolveScopeMetadata(..) must *never* return null.", scopeMetadata);
		assertEquals(BeanDefinition.SCOPE_PROTOTYPE, scopeMetadata.getScopeName());
		assertEquals(ScopedProxyMode.INTERFACES, scopeMetadata.getScopedProxyMode());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCtorWithNullScopedProxyMode() {
		new AnnotationScopeMetadataResolver(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetScopeAnnotationTypeWithNullType() {
		scopeMetadataResolver.setScopeAnnotationType(null);
	}


	@Scope("singleton")
	private static final class AnnotatedWithSingletonScope {
	}


	@Scope("prototype")
	private static final class AnnotatedWithPrototypeScope {
	}

}
