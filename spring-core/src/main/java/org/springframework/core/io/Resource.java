/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.core.io;

import org.springframework.lang.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Interface for a resource descriptor that abstracts from the actual
 * type of underlying resource, such as a file or class path resource.
 *
 * <p>An InputStream can be opened for every resource if it exists in
 * physical form, but a URL or File handle can just be returned for
 * certain resources. The actual behavior is implementation-specific.
 *
 * <p>资源(Resource)
 * <p>从底层资源（如文件或类路径资源）的实际类型抽象出来的资源描述符的接口。
 * <p> InputStream可以为每个资源打开，如果它以物理形式存在，但是URL或File句柄只能为某些资源返回。实际的行为是特定于实现的。
 *
 * @author Juergen Hoeller
 * @see #getInputStream()
 * @see #getURL()
 * @see #getURI()
 * @see #getFile()
 * @see WritableResource
 * @see ContextResource
 * @see UrlResource
 * @see FileUrlResource
 * @see FileSystemResource
 * @see ClassPathResource
 * @see ByteArrayResource
 * @see InputStreamResource
 * @since 28.12.2003
 */
public interface Resource extends InputStreamSource {

	/**
	 * Determine whether this resource actually exists in physical form.
	 * <p>This method performs a definitive existence check, whereas the
	 * existence of a {@code Resource} handle only guarantees a valid
	 * descriptor handle.
	 *
	 * <p>存在性(exists)
	 * <p>确定该资源是否以物理形式实际存在。
	 * <p>此方法执行确定的存在性检查，而{@code Resource}句柄的存在性仅保证描述符句柄有效。
	 */
	boolean exists();

	/**
	 * Indicate whether non-empty contents of this resource can be read via
	 * {@link #getInputStream()}.
	 * <p>Will be {@code true} for typical resource descriptors that exist
	 * since it strictly implies {@link #exists()} semantics as of 5.1.
	 * Note that actual content reading may still fail when attempted.
	 * However, a value of {@code false} is a definitive indication
	 * that the resource content cannot be read.
	 *
	 * <p>可读性(isReadable)
	 * <p>指示该资源的非空内容是否可以通过{@link #getInputStream()}读取。对于存在的典型资源描述符，
	 * <p>将为{@code true}，因为它严格暗示5.1中的{@link #exists()}语义。请注意，实际的内容读取在尝试时仍然可能失败。
	 * 但是，{@code false}的值明确表示无法读取资源内容。
	 *
	 * @see #getInputStream()
	 * @see #exists()
	 */
	default boolean isReadable() {
		return exists();
	}

	/**
	 * Indicate whether this resource represents a handle with an open stream.
	 * If {@code true}, the InputStream cannot be read multiple times,
	 * and must be read and closed to avoid resource leaks.
	 * <p>Will be {@code false} for typical resource descriptors.
	 *
	 * <p>是否打开(isOpen)
	 * <p>指示此资源是否表示具有打开流的句柄。如果{@code true}，InputStream不能被多次读取，必须被读取并关闭以避免资源泄漏。
	 * <p>对于典型的资源描述符，将为{@code false}。
	 */
	default boolean isOpen() {
		return false;
	}

	/**
	 * Determine whether this resource represents a file in a file system.
	 * <p>A value of {@code true} strongly suggests (but does not guarantee)
	 * that a {@link #getFile()} call will succeed.
	 * <p>This is conservatively {@code false} by default.
	 *
	 * <p>是否文件(isFile)
	 * <p>确定该资源是否代表文件系统中的文件。
	 * <p> {@code true}强烈建议（但不保证）{@link #getFile()}调用将成功。
	 * <p>默认为保守的{@code false}。
	 *
	 * @see #getFile()
	 * @since 5.0
	 */
	default boolean isFile() {
		return false;
	}

	/**
	 * Return a URL handle for this resource.
	 *
	 * <p>获取资源的URL句柄(getURL)
	 * <p>返回此资源的URL句柄
	 *
	 * @throws IOException if the resource cannot be resolved as URL,
	 *                     i.e. if the resource is not available as a descriptor
	 */
	URL getURL() throws IOException;

	/**
	 * Return a URI handle for this resource.
	 *
	 * <p>获取资源的URI句柄(getURI)
	 * <p>返回资源的URI句柄
	 *
	 * @throws IOException if the resource cannot be resolved as URI,
	 *                     i.e. if the resource is not available as a descriptor
	 * @since 2.5
	 */
	URI getURI() throws IOException;

	/**
	 * Return a File handle for this resource.
	 *
	 * <p>获取资源的文件句柄(getFile)
	 * <p>返回资源的文件句柄
	 *
	 * @throws java.io.FileNotFoundException if the resource cannot be resolved as
	 *                                       absolute file path, i.e. if the resource is not available in a file system
	 * @throws IOException                   in case of general resolution/reading failures
	 * @see #getInputStream()
	 */
	File getFile() throws IOException;

	/**
	 * Return a {@link ReadableByteChannel}.
	 * <p>It is expected that each call creates a <i>fresh</i> channel.
	 * <p>The default implementation returns {@link Channels#newChannel(InputStream)}
	 * with the result of {@link #getInputStream()}.
	 *
	 * <p>获取可读字节通道(readableChannel)
	 * <p>返回{@link ReadableByteChannel}。
	 * <p>期望每个调用创建一个<i>fresh<i>通道。
	 * <p>默认实现返回{@link Channels#newChannel(InputStream)}，结果为{@link #getInputStream()}
	 *
	 * @return the byte channel for the underlying resource (must not be {@code null})
	 * @throws java.io.FileNotFoundException if the underlying resource doesn't exist
	 * @throws IOException                   if the content channel could not be opened
	 * @see #getInputStream()
	 * @since 5.0
	 */
	default ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	/**
	 * Determine the content length for this resource.
	 *
	 * <p>内容长度(contentLength)
	 * <p>确定此资源的内容长度
	 *
	 * @throws IOException if the resource cannot be resolved
	 *                     (in the file system or as some other known physical resource type)
	 */
	long contentLength() throws IOException;

	/**
	 * Determine the last-modified timestamp for this resource.
	 *
	 * <p>最后修改(lastModified)
	 * <p>确定此资源最后修改的时间戳。
	 *
	 * @throws IOException if the resource cannot be resolved
	 *                     (in the file system or as some other known physical resource type)
	 */
	long lastModified() throws IOException;

	/**
	 * Create a resource relative to this resource.
	 *
	 * <p>创建相对于此资源的资源(createRelative)
	 * <p>返回相对于此资源的资源句柄
	 *
	 * @param relativePath the relative path (relative to this resource)
	 * @return the resource handle for the relative resource
	 * @throws IOException if the relative resource cannot be determined
	 */
	Resource createRelative(String relativePath) throws IOException;

	/**
	 * Determine a filename for this resource, i.e. typically the last
	 * part of the path: for example, "myfile.txt".
	 * <p>Returns {@code null} if this type of resource does not
	 * have a filename.
	 *
	 * <p>获取资源的文件名(getFilename)
	 * <p>返回此资源的文件名，通常是路径的最后一部分：例如，“myfile.txt”。
	 * <p>如果此类资源没有文件名，则返回{@code null}。
	 */
	@Nullable
	String getFilename();

	/**
	 * Return a description for this resource,
	 * to be used for error output when working with the resource.
	 * <p>Implementations are also encouraged to return this value
	 * from their {@code toString} method.
	 *
	 * <p>得到描述(getDescription)
	 * <p>返回此资源的描述，当使用资源时用于错误输出。
	 * <p>实现还鼓励从它们的{@code toString}方法返回此值。
	 *
	 * @see Object#toString()
	 */
	String getDescription();

}
