/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client.support;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * {@link FactoryBean} that creates a {@link Proxy java.net.Proxy}.
 *
 * @author Arjen Poutsma
 * @since 3.0.4
 * @see	FactoryBean
 * @see Proxy
 */
public class ProxyFactoryBean implements FactoryBean<Proxy>, InitializingBean {

	private Proxy.Type type = Proxy.Type.HTTP;

	private @Nullable String hostname;

	private int port = -1;

	private @Nullable Proxy proxy;


	/**
	 * Set the proxy type.
	 * <p>Defaults to {@link java.net.Proxy.Type#HTTP}.
	 */
	public void setType(Proxy.Type type) {
		this.type = type;
	}

	/**
	 * Set the proxy host name.
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * Set the proxy port.
	 */
	public void setPort(int port) {
		this.port = port;
	}


	@Override
	public void afterPropertiesSet() throws IllegalArgumentException {
		Assert.notNull(this.type, "Property 'type' is required");
		Assert.notNull(this.hostname, "Property 'hostname' is required");
		if (this.port < 0 || this.port > 65535) {
			throw new IllegalArgumentException("Property 'port' value out of range: " + this.port);
		}

		SocketAddress socketAddress = new InetSocketAddress(this.hostname, this.port);
		this.proxy = new Proxy(this.type, socketAddress);
	}


	@Override
	public @Nullable Proxy getObject() {
		return this.proxy;
	}

	@Override
	public Class<?> getObjectType() {
		return Proxy.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
