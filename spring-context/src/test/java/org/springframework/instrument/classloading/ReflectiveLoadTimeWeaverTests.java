/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ReflectiveLoadTimeWeaver}.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
class ReflectiveLoadTimeWeaverTests {

	@Test
	void ctorWithNullClassLoader() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ReflectiveLoadTimeWeaver(null));
	}

	@Test
	void ctorWithClassLoaderThatDoesNotExposeAnAddTransformerMethod() {
		assertThatIllegalStateException().isThrownBy(() ->
				new ReflectiveLoadTimeWeaver(getClass().getClassLoader()));
	}

	@Test
	void ctorWithClassLoaderThatDoesNotExposeAGetThrowawayClassLoaderMethodIsOkay() {
		JustAddTransformerClassLoader classLoader = new JustAddTransformerClassLoader();
		ReflectiveLoadTimeWeaver weaver = new ReflectiveLoadTimeWeaver(classLoader);
		weaver.addTransformer(new ClassFileTransformer() {
			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
				return "CAFEDEAD".getBytes();
			}
		});
		assertThat(classLoader.getNumTimesGetThrowawayClassLoaderCalled()).isEqualTo(1);
	}

	@Test
	void addTransformerWithNullTransformer() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ReflectiveLoadTimeWeaver(new JustAddTransformerClassLoader()).addTransformer(null));
	}

	@Test
	void getThrowawayClassLoaderWithClassLoaderThatDoesNotExposeAGetThrowawayClassLoaderMethodYieldsFallbackClassLoader() {
		ReflectiveLoadTimeWeaver weaver = new ReflectiveLoadTimeWeaver(new JustAddTransformerClassLoader());
		ClassLoader throwawayClassLoader = weaver.getThrowawayClassLoader();
		assertThat(throwawayClassLoader).isNotNull();
	}

	@Test
	void getThrowawayClassLoaderWithTotallyCompliantClassLoader() {
		TotallyCompliantClassLoader classLoader = new TotallyCompliantClassLoader();
		ReflectiveLoadTimeWeaver weaver = new ReflectiveLoadTimeWeaver(classLoader);
		ClassLoader throwawayClassLoader = weaver.getThrowawayClassLoader();
		assertThat(throwawayClassLoader).isNotNull();
		assertThat(classLoader.getNumTimesGetThrowawayClassLoaderCalled()).isEqualTo(1);
	}


	public static class JustAddTransformerClassLoader extends ClassLoader {

		private int numTimesAddTransformerCalled = 0;

		public int getNumTimesGetThrowawayClassLoaderCalled() {
			return this.numTimesAddTransformerCalled;
		}

		public void addTransformer(ClassFileTransformer transformer) {
			++this.numTimesAddTransformerCalled;
		}
	}


	public static final class TotallyCompliantClassLoader extends JustAddTransformerClassLoader {

		private int numTimesGetThrowawayClassLoaderCalled = 0;

		@Override
		public int getNumTimesGetThrowawayClassLoaderCalled() {
			return this.numTimesGetThrowawayClassLoaderCalled;
		}

		public ClassLoader getThrowawayClassLoader() {
			++this.numTimesGetThrowawayClassLoaderCalled;
			return getClass().getClassLoader();
		}
	}

}
