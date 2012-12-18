/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.aop.framework;

import java.io.Serializable;

import org.springframework.util.Assert;

/**
 * Convenience superclass for configuration used in creating proxies,
 * to ensure that all proxy creators have consistent properties.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AdvisedSupport
 */
public class ProxyConfig implements Serializable {

	/** use serialVersionUID from Spring 1.2 for interoperability */
	private static final long serialVersionUID = -8409359707199703185L;


	private boolean proxyTargetClass = false;

	private boolean optimize = false;

	boolean opaque = false;

	boolean exposeProxy = false;

	private boolean frozen = false;


	/**
	 * Set whether to proxy the target class directly, instead of just proxying
	 * specific interfaces. Default is "false".
	 * <p>Set this to "true" to force proxying for the TargetSource's exposed
	 * target class. If that target class is an interface, a JDK proxy will be
	 * created for the given interface. If that target class is any other class,
	 * a CGLIB proxy will be created for the given class.
	 * <p>Note: Depending on the configuration of the concrete proxy factory,
	 * the proxy-target-class behavior will also be applied if no interfaces
	 * have been specified (and no interface autodetection is activated).
	 * @see org.springframework.aop.TargetSource#getTargetClass()
	 */
	public void setProxyTargetClass(boolean proxyTargetClass) {
		this.proxyTargetClass = proxyTargetClass;
	}

	/**
	 * Return whether to proxy the target class directly as well as any interfaces.
	 */
	public boolean isProxyTargetClass() {
		return this.proxyTargetClass;
	}

	/**
	 * Set whether proxies should perform aggressive optimizations.
	 * The exact meaning of "aggressive optimizations" will differ
	 * between proxies, but there is usually some tradeoff.
	 * Default is "false".
	 * <p>For example, optimization will usually mean that advice changes won't
	 * take effect after a proxy has been created. For this reason, optimization
	 * is disabled by default. An optimize value of "true" may be ignored
	 * if other settings preclude optimization: for example, if "exposeProxy"
	 * is set to "true" and that's not compatible with the optimization.
	 */
	public void setOptimize(boolean optimize) {
		this.optimize = optimize;
	}

	/**
	 * Return whether proxies should perform aggressive optimizations.
	 */
	public boolean isOptimize() {
		return this.optimize;
	}

	/**
	 * Set whether proxies created by this configuration should be prevented
	 * from being cast to {@link Advised} to query proxy status.
	 * <p>Default is "false", meaning that any AOP proxy can be cast to
	 * {@link Advised}.
	 */
	public void setOpaque(boolean opaque) {
		this.opaque = opaque;
	}

	/**
	 * Return whether proxies created by this configuration should be
	 * prevented from being cast to {@link Advised}.
	 */
	public boolean isOpaque() {
		return this.opaque;
	}

	/**
	 * Set whether the proxy should be exposed by the AOP framework as a
	 * ThreadLocal for retrieval via the AopContext class. This is useful
	 * if an advised object needs to call another advised method on itself.
	 * (If it uses {@code this}, the invocation will not be advised).
	 * <p>Default is "false", in order to avoid unnecessary extra interception.
	 * This means that no guarantees are provided that AopContext access will
	 * work consistently within any method of the advised object.
	 */
	public void setExposeProxy(boolean exposeProxy) {
		this.exposeProxy = exposeProxy;
	}

	/**
	 * Return whether the AOP proxy will expose the AOP proxy for
	 * each invocation.
	 */
	public boolean isExposeProxy() {
		return this.exposeProxy;
	}

	/**
	 * Set whether this config should be frozen.
	 * <p>When a config is frozen, no advice changes can be made. This is
	 * useful for optimization, and useful when we don't want callers to
	 * be able to manipulate configuration after casting to Advised.
	 */
	public void setFrozen(boolean frozen) {
		this.frozen = frozen;
	}

	/**
	 * Return whether the config is frozen, and no advice changes can be made.
	 */
	public boolean isFrozen() {
		return this.frozen;
	}


	/**
	 * Copy configuration from the other config object.
	 * @param other object to copy configuration from
	 */
	public void copyFrom(ProxyConfig other) {
		Assert.notNull(other, "Other ProxyConfig object must not be null");
		this.proxyTargetClass = other.proxyTargetClass;
		this.optimize = other.optimize;
		this.exposeProxy = other.exposeProxy;
		this.frozen = other.frozen;
		this.opaque = other.opaque;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("proxyTargetClass=").append(this.proxyTargetClass).append("; ");
		sb.append("optimize=").append(this.optimize).append("; ");
		sb.append("opaque=").append(this.opaque).append("; ");
		sb.append("exposeProxy=").append(this.exposeProxy).append("; ");
		sb.append("frozen=").append(this.frozen);
		return sb.toString();
	}

}
