/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.remoting.rmi;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedExporter;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Abstract base class for remote service exporters that explicitly deserialize
 * {@link org.springframework.remoting.support.RemoteInvocation} objects and serialize
 * {@link org.springframework.remoting.support.RemoteInvocationResult} objects,
 * for example Spring's HTTP invoker.
 *
 * <p>Provides template methods for {@code ObjectInputStream} and
 * {@code ObjectOutputStream} handling.
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 * @see java.io.ObjectInputStream
 * @see java.io.ObjectOutputStream
 * @see #doReadRemoteInvocation
 * @see #doWriteRemoteInvocationResult
 */
public abstract class RemoteInvocationSerializingExporter extends RemoteInvocationBasedExporter
		implements InitializingBean {

	/**
	 * Default content type: "application/x-java-serialized-object"
	 */
	public static final String CONTENT_TYPE_SERIALIZED_OBJECT = "application/x-java-serialized-object";


	private String contentType = CONTENT_TYPE_SERIALIZED_OBJECT;

	private boolean acceptProxyClasses = true;

	private Object proxy;


	/**
	 * Specify the content type to use for sending remote invocation responses.
	 * <p>Default is "application/x-java-serialized-object".
	 */
	public void setContentType(String contentType) {
		Assert.notNull(contentType, "'contentType' must not be null");
		this.contentType = contentType;
	}

	/**
	 * Return the content type to use for sending remote invocation responses.
	 */
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * Set whether to accept deserialization of proxy classes.
	 * <p>Default is "true". May be deactivated as a security measure.
	 */
	public void setAcceptProxyClasses(boolean acceptProxyClasses) {
		this.acceptProxyClasses = acceptProxyClasses;
	}

	/**
	 * Return whether to accept deserialization of proxy classes.
	 */
	public boolean isAcceptProxyClasses() {
		return this.acceptProxyClasses;
	}


	@Override
	public void afterPropertiesSet() {
		prepare();
	}

	/**
	 * Initialize this service exporter.
	 */
	public void prepare() {
		this.proxy = getProxyForService();
	}

	protected final Object getProxy() {
		if (this.proxy == null) {
			throw new IllegalStateException(ClassUtils.getShortName(getClass()) + " has not been initialized");
		}
		return this.proxy;
	}


	/**
	 * Create an ObjectInputStream for the given InputStream.
	 * <p>The default implementation creates a Spring {@link CodebaseAwareObjectInputStream}.
	 * @param is the InputStream to read from
	 * @return the new ObjectInputStream instance to use
	 * @throws java.io.IOException if creation of the ObjectInputStream failed
	 */
	protected ObjectInputStream createObjectInputStream(InputStream is) throws IOException {
		return new CodebaseAwareObjectInputStream(is, getBeanClassLoader(), isAcceptProxyClasses());
	}

	/**
	 * Perform the actual reading of an invocation result object from the
	 * given ObjectInputStream.
	 * <p>The default implementation simply calls
	 * {@link java.io.ObjectInputStream#readObject()}.
	 * Can be overridden for deserialization of a custom wrapper object rather
	 * than the plain invocation, for example an encryption-aware holder.
	 * @param ois the ObjectInputStream to read from
	 * @return the RemoteInvocationResult object
	 * @throws java.io.IOException in case of I/O failure
	 * @throws ClassNotFoundException if case of a transferred class not
	 * being found in the local ClassLoader
	 */
	protected RemoteInvocation doReadRemoteInvocation(ObjectInputStream ois)
			throws IOException, ClassNotFoundException {

		Object obj = ois.readObject();
		if (!(obj instanceof RemoteInvocation)) {
			throw new RemoteException("Deserialized object needs to be assignable to type [" +
					RemoteInvocation.class.getName() + "]: " + ClassUtils.getDescriptiveType(obj));
		}
		return (RemoteInvocation) obj;
	}

	/**
	 * Create an ObjectOutputStream for the given OutputStream.
	 * <p>The default implementation creates a plain
	 * {@link java.io.ObjectOutputStream}.
	 * @param os the OutputStream to write to
	 * @return the new ObjectOutputStream instance to use
	 * @throws java.io.IOException if creation of the ObjectOutputStream failed
	 */
	protected ObjectOutputStream createObjectOutputStream(OutputStream os) throws IOException {
		return new ObjectOutputStream(os);
	}

	/**
	 * Perform the actual writing of the given invocation result object
	 * to the given ObjectOutputStream.
	 * <p>The default implementation simply calls
	 * {@link java.io.ObjectOutputStream#writeObject}.
	 * Can be overridden for serialization of a custom wrapper object rather
	 * than the plain invocation, for example an encryption-aware holder.
	 * @param result the RemoteInvocationResult object
	 * @param oos the ObjectOutputStream to write to
	 * @throws java.io.IOException if thrown by I/O methods
	 */
	protected void doWriteRemoteInvocationResult(RemoteInvocationResult result, ObjectOutputStream oos)
			throws IOException {

		oos.writeObject(result);
	}

}
