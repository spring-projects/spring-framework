/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.orm.jpa.persistenceunit;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import javax.persistence.spi.ClassTransformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * Simple adapter that implements the <code>java.lang.instrument.ClassFileTransformer</code>
 * interface based on a JPA ClassTransformer which a JPA PersistenceProvider asks the
 * PersistenceUnitInfo to install in the current runtime.
 *
 * @author Rod Johnson
 * @since 2.0
 * @see javax.persistence.spi.PersistenceUnitInfo#addTransformer(javax.persistence.spi.ClassTransformer)
 */
class ClassFileTransformerAdapter implements ClassFileTransformer {

	private static final Log logger = LogFactory.getLog(ClassFileTransformerAdapter.class);

	private final ClassTransformer classTransformer;


	public ClassFileTransformerAdapter(ClassTransformer classTransformer) {
		Assert.notNull(classTransformer, "ClassTransformer must not be null");
		this.classTransformer = classTransformer;
	}


	public byte[] transform(
			ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) {

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
			logger.error("Error weaving class [" + className + "] with " +
					"transformer of class [" + this.classTransformer.getClass().getName() + "]", ex);
			throw new IllegalStateException("Could not weave class [" + className + "]", ex);
		}
		catch (Throwable ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error weaving class [" + className + "] with " +
						"transformer of class [" + this.classTransformer.getClass().getName() + "]", ex);
			}
			// The exception will be ignored by the class loader, anyway...
			throw new IllegalStateException("Could not weave class [" + className + "]", ex);
		}
	}


	@Override
	public String toString() {
		return "Standard ClassFileTransformer wrapping JPA transformer: " + this.classTransformer;
	}

}
