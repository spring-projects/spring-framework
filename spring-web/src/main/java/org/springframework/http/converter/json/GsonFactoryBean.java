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

package org.springframework.http.converter.json;

import java.text.SimpleDateFormat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * A {@link FactoryBean} for creating a Google Gson 2.x {@link Gson} instance.
 *
 * @author Roy Clarkson
 * @author Juergen Hoeller
 * @since 4.1
 */
public class GsonFactoryBean implements FactoryBean<Gson>, InitializingBean {

	private boolean base64EncodeByteArrays = false;

	private boolean serializeNulls = false;

	private boolean prettyPrinting = false;

	private boolean disableHtmlEscaping = false;

	@Nullable
	private String dateFormatPattern;

	@Nullable
	private Gson gson;


	/**
	 * Whether to Base64-encode {@code byte[]} properties when reading and
	 * writing JSON.
	 * <p>When set to {@code true}, a custom {@link com.google.gson.TypeAdapter} will be
	 * registered via {@link GsonBuilder#registerTypeHierarchyAdapter(Class, Object)}
	 * which serializes a {@code byte[]} property to and from a Base64-encoded String
	 * instead of a JSON array.
	 * @see GsonBuilderUtils#gsonBuilderWithBase64EncodedByteArrays()
	 */
	public void setBase64EncodeByteArrays(boolean base64EncodeByteArrays) {
		this.base64EncodeByteArrays = base64EncodeByteArrays;
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


	@Override
	public void afterPropertiesSet() {
		GsonBuilder builder = (this.base64EncodeByteArrays ?
				GsonBuilderUtils.gsonBuilderWithBase64EncodedByteArrays() : new GsonBuilder());
		if (this.serializeNulls) {
			builder.serializeNulls();
		}
		if (this.prettyPrinting) {
			builder.setPrettyPrinting();
		}
		if (this.disableHtmlEscaping) {
			builder.disableHtmlEscaping();
		}
		if (this.dateFormatPattern != null) {
			builder.setDateFormat(this.dateFormatPattern);
		}
		this.gson = builder.create();
	}


	/**
	 * Return the created Gson instance.
	 */
	@Override
	@Nullable
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
