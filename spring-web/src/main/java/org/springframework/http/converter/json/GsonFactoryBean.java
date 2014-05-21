/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.http.converter.json;

import java.text.SimpleDateFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * A {@link FactoryBean} for creating a Google Gson 2.x {@link Gson} instance.
 *
 * @author Roy Clarkson
 * @since 4.1
 */
public class GsonFactoryBean implements FactoryBean<Gson>, BeanClassLoaderAware, InitializingBean {

	private static final boolean base64Present = ClassUtils.isPresent(
			"org.apache.commons.codec.binary.Base64", GsonFactoryBean.class.getClassLoader());

	private final Log logger = LogFactory.getLog(getClass());


	private Gson gson;

	private GsonBuilder gsonBuilder;

	private Boolean prettyPrint;

	private Boolean serializeNulls;

	private Boolean disableHtmlEscaping;

	private SimpleDateFormat dateFormat;

	private Boolean base64EncodeByteArrays;

	private ClassLoader beanClassLoader;


	/**
	 * Set the GsonBuilder instance to use. If not set, the GsonBuilder will be
	 * created using its default constructor.
	 */
	public void setGsonBuilder(GsonBuilder gsonBuilder) {
		this.gsonBuilder = gsonBuilder;
	}

	/**
	 * Return the configured GsonBuilder instance to use, if any.
	 * @return the GsonBuilder instance
	 */
	public GsonBuilder getGsonBuilder() {
		return this.gsonBuilder;
	}

	/**
	 * Whether to use the {@link GsonBuilder#setPrettyPrinting()} when writing
	 * JSON. This is a shortcut for setting up a {@code Gson} as follows:
	 *
	 * <pre class="code">
	 * new GsonBuilder().setPrettyPrinting().create();
	 * </pre>
	 */
	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
	}

	/**
	 * Whether to use the {@link GsonBuilder#serializeNulls()} option when
	 * writing JSON. This is a shortcut for setting up a {@code Gson} as
	 * follows:
	 *
	 * <pre class="code">
	 * new GsonBuilder().serializeNulls().create();
	 * </pre>
	 */
	public void setSerializeNulls(boolean serializeNulls) {
		this.serializeNulls = serializeNulls;
	}

	/**
	 * Whether to use the {@link GsonBuilder#disableHtmlEscaping()} when writing
	 * JSON. Set to {@code true} to disable HTML escaping in JSON. This is a
	 * shortcut for setting up a {@code Gson} as follows:
	 *
	 * <pre class="code">
	 * new GsonBuilder().disableHtmlEscaping().create();
	 * </pre>
	 */
	public void setDisableHtmlEscaping(boolean disableHtmlEscaping) {
		this.disableHtmlEscaping = disableHtmlEscaping;
	}

	/**
	 * Define the format for date/time with the given {@link SimpleDateFormat}.
	 * This is a shortcut for setting up a {@code Gson} as follows:
	 *
	 * <pre class="code">
	 * new GsonBuilder().setDateFormat(dateFormatPattern).create();
	 * </pre>
	 *
	 * @see #setSimpleDateFormat(String)
	 */
	public void setSimpleDateFormat(SimpleDateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}

	/**
	 * Define the date/time format with a {@link SimpleDateFormat}.
	 * This is a shortcut for setting up a {@code Gson} as follows:
	 *
	 * <pre class="code">
	 * new GsonBuilder().setDateFormat(dateFormatPattern).create();
	 * </pre>
	 *
	 * @see #setSimpleDateFormat(SimpleDateFormat)
	 */
	public void setSimpleDateFormat(String format) {
		this.dateFormat = new SimpleDateFormat(format);
	}

	/**
	 * Whether to Base64 encode {@code byte[]} properties when reading and
	 * writing JSON.
	 *
	 * <p>When set to {@code true} a custom {@link com.google.gson.TypeAdapter}
	 * is registered via
	 * {@link GsonBuilder#registerTypeHierarchyAdapter(Class, Object)}
	 * that serializes a {@code byte[]} property to and from a Base64 encoded
	 * string instead of a JSON array.
	 *
	 * <p><strong>NOTE:</strong> Use of this option requires the presence of
	 * Apache commons-codec on the classpath. Otherwise it is ignored.
	 *
	 * @see org.springframework.http.converter.json.GsonBase64ByteArrayJsonTypeAdapter
	 */
	public void setBase64EncodeByteArrays(boolean base64EncodeByteArrays) {
		this.base64EncodeByteArrays = base64EncodeByteArrays;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.gsonBuilder == null) {
			this.gsonBuilder = new GsonBuilder();
		}
		if (this.prettyPrint != null && this.prettyPrint) {
			this.gsonBuilder = this.gsonBuilder.setPrettyPrinting();
		}
		if (this.serializeNulls != null && this.serializeNulls) {
			this.gsonBuilder = this.gsonBuilder.serializeNulls();
		}
		if (this.disableHtmlEscaping != null && this.disableHtmlEscaping) {
			this.gsonBuilder = this.gsonBuilder.disableHtmlEscaping();
		}
		if (this.dateFormat != null) {
			this.gsonBuilder.setDateFormat(this.dateFormat.toPattern());
		}
		if (base64Present) {
			if (this.base64EncodeByteArrays != null && this.base64EncodeByteArrays) {
				this.gsonBuilder.registerTypeHierarchyAdapter(byte[].class, new GsonBase64ByteArrayJsonTypeAdapter());
			}
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("org.apache.commons.codec.binary.Base64 is not " +
					"available on the class path. Gson Base64 encoding is disabled.");
		}
		this.gson = this.gsonBuilder.create();
	}

	/**
	 * Return the created Gson instance.
	 */
	@Override
	public Gson getObject() throws Exception {
		return this.gson;
	}

	@Override
	public Class<?> getObjectType() {
		return Gson.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
