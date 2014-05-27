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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;


/**
 * A {@link FactoryBean} for creating a Google Gson 2.x {@link Gson} instance.
 *
 * @author Roy Clarkson
 * @author Juergen Hoeller
 * @since 4.1
 */
public class GsonFactoryBean implements FactoryBean<Gson>, InitializingBean {

	/** Apache Commons Codec present on the classpath, for Base64 encoding? */
	private static final boolean commonsCodecPresent = ClassUtils.isPresent(
			"org.apache.commons.codec.binary.Base64", GsonFactoryBean.class.getClassLoader());


	private GsonBuilder gsonBuilder;

	private boolean serializeNulls;

	private boolean prettyPrinting;

	private boolean disableHtmlEscaping;

	private String dateFormatPattern;

	private boolean base64EncodeByteArrays;

	private Gson gson;


	/**
	 * Set the GsonBuilder instance to use.
	 * If not set, the GsonBuilder will be created using its default constructor.
	 */
	public void setGsonBuilder(GsonBuilder gsonBuilder) {
		this.gsonBuilder = gsonBuilder;
	}

	/**
	 * Whether to use the {@link GsonBuilder#serializeNulls()} option when writing
	 * JSON. This is a shortcut for setting up a {@code Gson} as follows:
	 * <pre class="code">
	 * new GsonBuilder().serializeNulls().create();
	 * </pre>
	 */
	public void setSerializeNulls(boolean serializeNulls) {
		this.serializeNulls = serializeNulls;
	}

	/**
	 * Whether to use the {@link GsonBuilder#setPrettyPrinting()} when writing
	 * JSON. This is a shortcut for setting up a {@code Gson} as follows:
	 * <pre class="code">
	 * new GsonBuilder().setPrettyPrinting().create();
	 * </pre>
	 */
	public void setPrettyPrinting(boolean prettyPrinting) {
		this.prettyPrinting = prettyPrinting;
	}

	/**
	 * Whether to use the {@link GsonBuilder#disableHtmlEscaping()} when writing
	 * JSON. Set to {@code true} to disable HTML escaping in JSON. This is a
	 * shortcut for setting up a {@code Gson} as follows:
	 * <pre class="code">
	 * new GsonBuilder().disableHtmlEscaping().create();
	 * </pre>
	 */
	public void setDisableHtmlEscaping(boolean disableHtmlEscaping) {
		this.disableHtmlEscaping = disableHtmlEscaping;
	}

	/**
	 * Define the date/time format with a {@link SimpleDateFormat}-style pattern.
	 * This is a shortcut for setting up a {@code Gson} as follows:
	 * <pre class="code">
	 * new GsonBuilder().setDateFormat(dateFormatPattern).create();
	 * </pre>
	 */
	public void setDateFormatPattern(String dateFormatPattern) {
		this.dateFormatPattern = dateFormatPattern;
	}

	/**
	 * Whether to Base64-encode {@code byte[]} properties when reading and
	 * writing JSON.
	 * <p>When set to {@code true} a custom {@link com.google.gson.TypeAdapter} is
	 * registered via {@link GsonBuilder#registerTypeHierarchyAdapter(Class, Object)}
	 * that serializes a {@code byte[]} property to and from a Base64-encoded String
	 * instead of a JSON array.
	 * <p><strong>NOTE:</strong> Use of this option requires the presence of the
	 * Apache Commons Codec library on the classpath.
	 * @see GsonBase64ByteArrayJsonTypeAdapter
	 */
	public void setBase64EncodeByteArrays(boolean base64EncodeByteArrays) {
		this.base64EncodeByteArrays = base64EncodeByteArrays;
	}


	@Override
	public void afterPropertiesSet() {
		if (this.gsonBuilder == null) {
			this.gsonBuilder = new GsonBuilder();
		}
		if (this.serializeNulls) {
			this.gsonBuilder.serializeNulls();
		}
		if (this.prettyPrinting) {
			this.gsonBuilder.setPrettyPrinting();
		}
		if (this.disableHtmlEscaping) {
			this.gsonBuilder.disableHtmlEscaping();
		}
		if (this.dateFormatPattern != null) {
			this.gsonBuilder.setDateFormat(this.dateFormatPattern);
		}
		if (this.base64EncodeByteArrays) {
			if (commonsCodecPresent) {
				this.gsonBuilder.registerTypeHierarchyAdapter(byte[].class, new GsonBase64ByteArrayJsonTypeAdapter());
			}
			else {
				throw new IllegalStateException(
						"Apache Commons Codec is not available on the classpath - cannot enable Gson Base64 encoding");
			}
		}
		this.gson = this.gsonBuilder.create();
	}


	/**
	 * Return the created Gson instance.
	 */
	@Override
	public Gson getObject() {
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
