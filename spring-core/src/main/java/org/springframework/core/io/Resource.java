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

package org.springframework.core.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.springframework.lang.Nullable;

/**
 * bean资源定位
 */
public interface Resource extends InputStreamSource {

	/**
	 * 是否存在
	 */
	boolean exists();

	/**
	 * 资源权限 资源是否可读
	 */
	default boolean isReadable() {
		return exists();
	}

	/**
	 * 资源是否被打开过
	 * 资源所代表的句柄是否被一个 stream 打开了
	 */
	default boolean isOpen() {
		return false;
	}

	/**
	 * 是否为 文件File
	 */

	default boolean isFile() {
		return false;
	}

	/**
	 * 返回Url
	 * 返回资源的 URL 的句柄
	 */
	URL getURL() throws IOException;

	/**
	 * 返回Uri
	 * 返回资源的 URi 的句柄
	 */
	URI getURI() throws IOException;

	/**
	 * 返回file
	 * 返回资源的 file 的句柄
	 */
	File getFile() throws IOException;

	/**
	 * 返回 ReadableByteChannel
	 */
	default ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	/**
	 * 资源内容的长度
	 */
	long contentLength() throws IOException;

	/**
	 * 最后修改时间
	 */
	long lastModified() throws IOException;

	/**
	 * 创建新的资源 根据资源的相对路径
	 */
	Resource createRelative(String relativePath) throws IOException;

	/**
	 * 资源文件名称
	 */
	@Nullable
	String getFilename();

	/**
	 * 获取资源的描述信息
	 */
	String getDescription();

}
