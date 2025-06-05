/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.aop.interceptor;

import java.lang.reflect.InvocationTargetException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.AdapterFinderBean;
/**
 * A proxy interceptor for finding a concrete adapter implementation of an abstracted interface using a
 * {@link AdapterFinderBean}. This is in the vein of
 * {@link org.springframework.beans.factory.config.ServiceLocatorFactoryBean}, but also without the client code
 * pollution of acquiring the service prior to calling the intended interceptor.  The {@code AdapterFinderBean} uses
 * the method and arguments to determine the appropriate concrete adapter to call.
 *
 * <p>By way of an example, consider the following adapter interface.
 * Note that this interface is not dependent on any Spring APIs.
 *
 * <pre class="code">package a.b.c;
 *
 *public interface MyService {
 *
 * byte[] convert(IMAGE_TYPE to, IMAGE_TYPE from, byte[] source);
 * enum IMAGE_TYPE {
 *  GIF,
 *  JPEG,
 *  PNG
 * }
 *}</pre>
 *
 * <p>An {@link AdapterFinderBean}.
 * <pre class="code">package a.b.c;
 *
 *public class MyServiceFinder implements AdapterFinderBean&lt;MyService&gt; {
 *
 * private final MyService gifService;
 * private final MyService jpgService;
 * private final MyService pngService;
 *
 * public MyServiceFinder(MyService gifService, MyService jpgService, MyService pngService) {
 *   this.gifService = gifService;
 *   this.jpgService = jpgService;
 *   this.pngService = pngService;
 * }
 *
 * &#064;Nullable MyService findBean(Method method, Object[] args) {
 *  IMAGE_TYPE type = (IMAGE_TYPE) args[0];
 *  if (type == GIF) {
 *    return gifService;
 *  }
 *
 *  if (type == JPEG) {
 *    return jpgService;
 *  }
 *
 *  if (type == PNG) {
 *    return pngService;
 *  }
 *
 *  return null; // will throw an IllegalArgumentException!
 * }
 *}</pre>
 *
 * <p>A spring configuration file.
 * <pre class="code">package a.b.c;
 *
 *&#064;Configuration
 *class MyServiceConfiguration {
 *
 * &#064;Bean
 * MyServiceFinder myServiceFinder(MyGifService gifService, MyJpegService jpgService, MyPngService pngService) {
 *  return new MyServiceFinder(gifService, jpgService, pngService);
 * }
 *
 * &#064;Bean
 * &#064;Primary
 * MyService myService(MyServiceFinder finder) {
 *  return AdapterFinderInterceptor.proxyOf(finder, MyService.class);
 * }
 *}
 * </pre>
 *
 * <p>A client bean may look something like this:
 *
 * <pre class="code">package a.b.c;
 *
 *public class MyClientBean {
 *
 * private final MyService myService;
 *
 * public MyClientBean(MyService myService) {
 *  this.myService = myService;
 * }
 *
 * public void doSomeBusinessMethod(byte[] background, byte[] foreground, byte[] border) {
 *  byte[] gifBackground = myService.convert(PNG, GIF, background);
 *  byte[] gifForeground = myService.convert(PNG, GIF, foreground);
 *  byte[] gifBorder = myService.convert(PNG, GIF, border);
 *
 *  // no do something with the gif stuff.
 * }
 *}</pre>
 *
 * @author Joe Chambers
 * @param <T> the service the interceptor proxy's.
 */
public final class AdapterFinderInterceptor<T> implements MethodInterceptor {

	private final AdapterFinderBean<T> finder;

	/**
	 * Constructor.
	 * @param finder the {@code AdapterFinder} to use for obtaining concrete instances
	 */
	private AdapterFinderInterceptor(AdapterFinderBean<T> finder) {
		this.finder = finder;
	}

	/**
	 * The implementation of the {@link MethodInterceptor#invoke(MethodInvocation)} method called by the proxy.
	 * @param invocation the method invocation joinpoint
	 * @return the results of the concrete invocation call
	 * @throws Throwable if no adapter is found will throw {@link IllegalArgumentException} otherwise will re-throw what the concrete invocation throws
	 */
	@Override
	public @Nullable Object invoke(@NonNull MethodInvocation invocation) throws Throwable {
		T implementation = this.finder.findAdapter(invocation.getMethod(), invocation.getArguments());
		if (implementation == null) {
			throw new IllegalArgumentException("Adapter not found: " + invocation.getMethod());
		}

		try {
			return invocation.getMethod().invoke(implementation, invocation.getArguments());
		}
		catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
	}

	/**
	 * Create a proxy using an {@code AdapterFinderInterceptor}.
	 * @param finder the finder bean to create the proxy around.
	 * @param proxyClass the {@link Class} of the {@code Interface} being exposed.
	 * @param <T> the type of the interface the proxy exposes.
	 * @return a {@code Proxy} that uses the finder to determine which adapter to direct on a call by call basis.
	 */
	public static <T> T proxyOf(AdapterFinderBean<T> finder, Class<T> proxyClass) {
		return ProxyFactory.getProxy(proxyClass, new AdapterFinderInterceptor<>(finder));
	}
}
