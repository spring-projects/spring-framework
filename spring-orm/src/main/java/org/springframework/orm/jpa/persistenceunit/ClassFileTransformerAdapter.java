/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.persistenceunit;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import jakarta.persistence.spi.ClassTransformer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Simple adapter that implements the {@code java.lang.instrument.ClassFileTransformer}
 * interface based on a JPA {@code ClassTransformer} which a JPA PersistenceProvider
 * asks the {@code PersistenceUnitInfo} to install in the current runtime.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see jakarta.persistence.spi.PersistenceUnitInfo#addTransformer(jakarta.persistence.spi.ClassTransformer)
 */
class ClassFileTransformerAdapter implements ClassFileTransformer {

	private static final Log logger = LogFactory.getLog(ClassFileTransformerAdapter.class);


	private final ClassTransformer classTransformer;

	private final ThreadLocal<@Nullable Boolean> currentlyTransforming = new ThreadLocal<>();


	public ClassFileTransformerAdapter(ClassTransformer classTransformer) {
		Assert.notNull(classTransformer, "ClassTransformer must not be null");
		this.classTransformer = classTransformer;
	}


	@Override
	public byte @Nullable [] transform(
			ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) {

		if (this.currentlyTransforming.get() == Boolean.TRUE) {
			// Defensively back out when called from within the transform delegate below:
			// in particular, for the over-eager transformer implementation in Hibernate.
			return null;
		}

		this.currentlyTransforming.set(Boolean.TRUE);
		try {
			byte[] transformed = this.classTransformer.transform(
					loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
			if (transformed != null && logger.isDebugEnabled()) {
				logger.debug("Transformer of class [" + this.classTransformer.getClass().getName() +
						"] transformed class [" + className + "]; bytes in=" +
						classfileBuffer.length + "; bytes out=" + transformed.length);
			}
			return transformed;
		}
		catch (ClassCircularityError ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Circularity error while weaving class [" + className + "] with " +
						"transformer of class [" + this.classTransformer.getClass().getName() + "]", ex);
			}
			throw new IllegalStateException("Failed to weave class [" + className + "]", ex);
		}
		catch (Throwable ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error weaving class [" + className + "] with transformer of class [" +
						this.classTransformer.getClass().getName() + "]", ex);
			}
			// The exception will be ignored by the class loader, anyway...
			throw new IllegalStateException("Could not weave class [" + className + "]", ex);
		}
		finally {
			this.currentlyTransforming.remove();
		}
	}


	@Override
	public String toString() {
		return "Standard ClassFileTransformer wrapping JPA transformer: " + this.classTransformer;
	}

}
