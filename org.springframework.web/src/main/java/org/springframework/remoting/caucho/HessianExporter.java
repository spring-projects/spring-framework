/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.remoting.caucho;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;

import com.caucho.hessian.io.SerializerFactory;
import com.caucho.hessian.server.HessianSkeleton;
import org.apache.commons.logging.Log;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.support.RemoteExporter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * General stream-based protocol exporter for a Hessian endpoint.
 *
 * <p>Hessian is a slim, binary RPC protocol.
 * For information on Hessian, see the
 * <a href="http://www.caucho.com/hessian">Hessian website</a>.
 *
 * <p>This exporter will work with both Hessian 2.x and 3.x (respectively
 * Resin 2.x and 3.x), autodetecting the corresponding skeleton class.
 * As of Spring 2.0, it is also compatible with the new Hessian 2 protocol
 * (a.k.a. Hessian 3.0.20+), while remaining compatible with older versions.
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 * @see #invoke(java.io.InputStream, java.io.OutputStream)
 * @see HessianServiceExporter
 * @see SimpleHessianServiceExporter
 */
public class HessianExporter extends RemoteExporter implements InitializingBean {

	private static final boolean hessian2Available =
			ClassUtils.isPresent("com.caucho.hessian.io.Hessian2Input", HessianServiceExporter.class.getClassLoader());


	private SerializerFactory serializerFactory = new SerializerFactory();

	private Log debugLogger;

	private HessianSkeletonInvoker skeletonInvoker;


	/**
	 * Specify the Hessian SerializerFactory to use.
	 * <p>This will typically be passed in as an inner bean definition
	 * of type <code>com.caucho.hessian.io.SerializerFactory</code>,
	 * with custom bean property values applied.
	 */
	public void setSerializerFactory(SerializerFactory serializerFactory) {
		this.serializerFactory = (serializerFactory != null ? serializerFactory : new SerializerFactory());
	}

	/**
	 * Set whether to send the Java collection type for each serialized
	 * collection. Default is "true".
	 */
	public void setSendCollectionType(boolean sendCollectionType) {
		this.serializerFactory.setSendCollectionType(sendCollectionType);
	}

	/**
	 * Set whether Hessian's debug mode should be enabled, logging to
	 * this exporter's Commons Logging log. Default is "false".
	 * @see com.caucho.hessian.client.HessianProxyFactory#setDebug
	 */
	public void setDebug(boolean debug) {
		this.debugLogger = (debug ? logger : null);
	}


	public void afterPropertiesSet() {
		prepare();
	}

	/**
	 * Initialize this exporter.
	 */
	public void prepare() {
		HessianSkeleton skeleton = null;

		try {
			try {
				// Try Hessian 3.x (with service interface argument).
				Constructor ctor = HessianSkeleton.class.getConstructor(new Class[] {Object.class, Class.class});
				checkService();
				checkServiceInterface();
				skeleton = (HessianSkeleton)
						ctor.newInstance(new Object[] {getProxyForService(), getServiceInterface()});
			}
			catch (NoSuchMethodException ex) {
				// Fall back to Hessian 2.x (without service interface argument).
				Constructor ctor = HessianSkeleton.class.getConstructor(new Class[] {Object.class});
				skeleton = (HessianSkeleton) ctor.newInstance(new Object[] {getProxyForService()});
			}
		}
		catch (Throwable ex) {
			throw new BeanInitializationException("Hessian skeleton initialization failed", ex);
		}

		if (hessian2Available) {
			// Hessian 2 (version 3.0.20+).
			this.skeletonInvoker = new Hessian2SkeletonInvoker(skeleton, this.serializerFactory, this.debugLogger);
		}
		else {
			// Hessian 1 (version 3.0.19-).
			this.skeletonInvoker = new Hessian1SkeletonInvoker(skeleton, this.serializerFactory);
		}
	}


	/**
	 * Perform an invocation on the exported object.
	 * @param inputStream the request stream
	 * @param outputStream the response stream
	 * @throws Throwable if invocation failed
	 */
	public void invoke(InputStream inputStream, OutputStream outputStream) throws Throwable {
		Assert.notNull(this.skeletonInvoker, "Hessian exporter has not been initialized");
		ClassLoader originalClassLoader = overrideThreadContextClassLoader();
		try {
			this.skeletonInvoker.invoke(inputStream, outputStream);
		}
		finally {
			resetThreadContextClassLoader(originalClassLoader);
		}
	}

}
