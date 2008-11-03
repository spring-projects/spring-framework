package org.springframework.context.annotation;

import junit.framework.TestCase;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.test.AssertThrows;

/**
 * Unit tests for the {@link AnnotationScopeMetadataResolver} class.
 *
 * @author Rick Evans
 */
public final class AnnotationScopeMetadataResolverTests extends TestCase {

	private AnnotationScopeMetadataResolver scopeMetadataResolver;


	@Override
	protected void setUp() throws Exception {
		this.scopeMetadataResolver = new AnnotationScopeMetadataResolver();
	}


	public void testThatResolveScopeMetadataDoesNotApplyScopedProxyModeToASingleton() {
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(AnnotatedWithSingletonScope.class);
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(bd);
		assertNotNull("resolveScopeMetadata(..) must *never* return null.", scopeMetadata);
		assertEquals(BeanDefinition.SCOPE_SINGLETON, scopeMetadata.getScopeName());
		assertEquals(ScopedProxyMode.NO, scopeMetadata.getScopedProxyMode());
	}


	public void testThatResolveScopeMetadataDoesApplyScopedProxyModeToAPrototype() {
		this.scopeMetadataResolver = new AnnotationScopeMetadataResolver(ScopedProxyMode.INTERFACES);
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(AnnotatedWithPrototypeScope.class);
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(bd);
		assertNotNull("resolveScopeMetadata(..) must *never* return null.", scopeMetadata);
		assertEquals(BeanDefinition.SCOPE_PROTOTYPE, scopeMetadata.getScopeName());
		assertEquals(ScopedProxyMode.INTERFACES, scopeMetadata.getScopedProxyMode());
	}

	public void testCtorWithNullScopedProxyMode() {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				new AnnotationScopeMetadataResolver(null);
			}
		}.runTest();
	}

	public void testSetScopeAnnotationTypeWithNullType() {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				scopeMetadataResolver.setScopeAnnotationType(null);
			}
		}.runTest();
	}


	@Scope("singleton")
	private static final class AnnotatedWithSingletonScope {
	}


	@Scope("prototype")
	private static final class AnnotatedWithPrototypeScope {
	}

}
