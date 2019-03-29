/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.validation;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Encapsulates an object error, that is, a global reason for rejecting
 * an object.
 *
 * <p>See the {@link DefaultMessageCodesResolver} javadoc for details on
 * how a message code list is built for an {@code ObjectError}.
 *
 * @author Juergen Hoeller
 * @since 10.03.2003
 * @see FieldError
 * @see DefaultMessageCodesResolver
 */
@SuppressWarnings("serial")
public class ObjectError extends DefaultMessageSourceResolvable {

	private final String objectName;

	@Nullable
	private transient Object source;


	/**
	 * Create a new instance of the ObjectError class.
	 * @param objectName the name of the affected object
	 * @param defaultMessage the default message to be used to resolve this message
	 */
	public ObjectError(String objectName, String defaultMessage) {
		this(objectName, null, null, defaultMessage);
	}

	/**
	 * Create a new instance of the ObjectError class.
	 * @param objectName the name of the affected object
	 * @param codes the codes to be used to resolve this message
	 * @param arguments	the array of arguments to be used to resolve this message
	 * @param defaultMessage the default message to be used to resolve this message
	 */
	public ObjectError(
			String objectName, @Nullable String[] codes, @Nullable Object[] arguments, @Nullable String defaultMessage) {

		super(codes, arguments, defaultMessage);
		Assert.notNull(objectName, "Object name must not be null");
		this.objectName = objectName;
	}


	/**
	 * Return the name of the affected object.
	 */
	public String getObjectName() {
		return this.objectName;
	}

	/**
	 * Preserve the source behind this error: possibly an {@link Exception}
	 * (typically {@link org.springframework.beans.PropertyAccessException})
	 * or a Bean Validation {@link javax.validation.ConstraintViolation}.
	 * <p>Note that any such source object is being stored as transient:
	 * that is, it won't be part of a serialized error representation.
	 * @param source the source object
	 * @since 5.0.4
	 */
	public void wrap(Object source) {
		if (this.source != null) {
			throw new IllegalStateException("Already wrapping " + this.source);
		}
		this.source = source;
	}

	/**
	 * Unwrap the source behind this error: possibly an {@link Exception}
	 * (typically {@link org.springframework.beans.PropertyAccessException})
	 * or a Bean Validation {@link javax.validation.ConstraintViolation}.
	 * <p>The cause of the outermost exception will be introspected as well,
	 * e.g. the underlying conversion exception or exception thrown from a setter
	 * (instead of having to unwrap the {@code PropertyAccessException} in turn).
	 * @return the source object of the given type
	 * @throws IllegalArgumentException if no such source object is available
	 * (i.e. none specified or not available anymore after deserialization)
	 * @since 5.0.4
	 */
	public <T> T unwrap(Class<T> sourceType) {
		if (sourceType.isInstance(this.source)) {
			return sourceType.cast(this.source);
		}
		else if (this.source instanceof Throwable) {
			Throwable cause = ((Throwable) this.source).getCause();
			if (sourceType.isInstance(cause)) {
				return sourceType.cast(cause);
			}
		}
		throw new IllegalArgumentException("No source object of the given type available: " + sourceType);
	}

	/**
	 * Check the source behind this error: possibly an {@link Exception}
	 * (typically {@link org.springframework.beans.PropertyAccessException})
	 * or a Bean Validation {@link javax.validation.ConstraintViolation}.
	 * <p>The cause of the outermost exception will be introspected as well,
	 * e.g. the underlying conversion exception or exception thrown from a setter
	 * (instead of having to unwrap the {@code PropertyAccessException} in turn).
	 * @return whether this error has been caused by a source object of the given type
	 * @since 5.0.4
	 */
	public boolean contains(Class<?> sourceType) {
		return (sourceType.isInstance(this.source) ||
				(this.source instanceof Throwable && sourceType.isInstance(((Throwable) this.source).getCause())));
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || other.getClass() != getClass() || !super.equals(other)) {
			return false;
		}
		ObjectError otherError = (ObjectError) other;
		return getObjectName().equals(otherError.getObjectName());
	}

	@Override
	public int hashCode() {
		return super.hashCode() * 29 + getObjectName().hashCode();
	}

	@Override
	public String toString() {
		return "Error in object '" + this.objectName + "': " + resolvableToString();
	}

}
