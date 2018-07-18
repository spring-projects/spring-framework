/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.beans.factory;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.FatalBeanException;
import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;

/**
 * Exception thrown when a BeanFactory encounters an error when
 * attempting to create a bean from a bean definition.
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class BeanCreationException extends FatalBeanException {

	@Nullable
	private String beanName;

	@Nullable
	private String resourceDescription;

	@Nullable
	private List<Throwable> relatedCauses;


	/**
	 * Create a new BeanCreationException.
	 * @param msg the detail message
	 */
	public BeanCreationException(String msg) {
		super(msg);
	}

	/**
	 * Create a new BeanCreationException.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public BeanCreationException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * Create a new BeanCreationException.
	 * @param beanName the name of the bean requested
	 * @param msg the detail message
	 */
	public BeanCreationException(String beanName, String msg) {
		super("Error creating bean with name '" + beanName + "': " + msg);
		this.beanName = beanName;
	}

	/**
	 * Create a new BeanCreationException.
	 * @param beanName the name of the bean requested
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public BeanCreationException(String beanName, String msg, Throwable cause) {
		this(beanName, msg);
		initCause(cause);
	}

	/**
	 * Create a new BeanCreationException.
	 * @param resourceDescription description of the resource
	 * that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param msg the detail message
	 */
	public BeanCreationException(@Nullable String resourceDescription, @Nullable String beanName, String msg) {
		super("Error creating bean with name '" + beanName + "'" +
				(resourceDescription != null ? " defined in " + resourceDescription : "") + ": " + msg);
		this.resourceDescription = resourceDescription;
		this.beanName = beanName;
	}

	/**
	 * Create a new BeanCreationException.
	 * @param resourceDescription description of the resource
	 * that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public BeanCreationException(@Nullable String resourceDescription, String beanName, String msg, Throwable cause) {
		this(resourceDescription, beanName, msg);
		initCause(cause);
	}


	/**
	 * Return the description of the resource that the bean
	 * definition came from, if any.
	 */
	@Nullable
	public String getResourceDescription() {
		return this.resourceDescription;
	}

	/**
	 * Return the name of the bean requested, if any.
	 */
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * Add a related cause to this bean creation exception,
	 * not being a direct cause of the failure but having occurred
	 * earlier in the creation of the same bean instance.
	 * @param ex the related cause to add
	 */
	public void addRelatedCause(Throwable ex) {
		if (this.relatedCauses == null) {
			this.relatedCauses = new ArrayList<>();
		}
		this.relatedCauses.add(ex);
	}

	/**
	 * Return the related causes, if any.
	 * @return the array of related causes, or {@code null} if none
	 */
	@Nullable
	public Throwable[] getRelatedCauses() {
		if (this.relatedCauses == null) {
			return null;
		}
		return this.relatedCauses.toArray(new Throwable[0]);
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		if (this.relatedCauses != null) {
			for (Throwable relatedCause : this.relatedCauses) {
				sb.append("\nRelated cause: ");
				sb.append(relatedCause);
			}
		}
		return sb.toString();
	}

	@Override
	public void printStackTrace(PrintStream ps) {
		synchronized (ps) {
			super.printStackTrace(ps);
			if (this.relatedCauses != null) {
				for (Throwable relatedCause : this.relatedCauses) {
					ps.println("Related cause:");
					relatedCause.printStackTrace(ps);
				}
			}
		}
	}

	@Override
	public void printStackTrace(PrintWriter pw) {
		synchronized (pw) {
			super.printStackTrace(pw);
			if (this.relatedCauses != null) {
				for (Throwable relatedCause : this.relatedCauses) {
					pw.println("Related cause:");
					relatedCause.printStackTrace(pw);
				}
			}
		}
	}

	@Override
	public boolean contains(@Nullable Class<?> exClass) {
		if (super.contains(exClass)) {
			return true;
		}
		if (this.relatedCauses != null) {
			for (Throwable relatedCause : this.relatedCauses) {
				if (relatedCause instanceof NestedRuntimeException &&
						((NestedRuntimeException) relatedCause).contains(exClass)) {
					return true;
				}
			}
		}
		return false;
	}

}
