/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.http.converter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

/**
 * Implementation of {@link HttpMessageConverter} that can read and write {@link BufferedImage BufferedImages}.
 *
 * <p>By default, this converter can read all media types that are supported by the {@linkplain
 * ImageIO#getReaderMIMETypes() registered image readers}, and writes using the media type of the first available
 * {@linkplain javax.imageio.ImageIO#getWriterMIMETypes() registered image writer}. This behavior can be overriden by
 * setting the {@link #setSupportedMediaTypes(java.util.List) supportedMediaTypes} and {@link
 * #setContentType(org.springframework.http.MediaType) contentType} properties respectively.
 *
 * <p>If the {@link #setCacheDir(java.io.File) cacheDir} property is set to an existing directory, this converter will
 * cache image data.
 *
 * <p>The {@link #process(ImageReadParam)} and {@link #process(ImageWriteParam)} template methods allow subclasses to
 * override Image I/O parameters.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class BufferedImageHttpMessageConverter extends AbstractHttpMessageConverter<BufferedImage> {

	private MediaType contentType;

	private File cacheDir;

	public BufferedImageHttpMessageConverter() {
		String[] readerMediaTypes = ImageIO.getReaderMIMETypes();
		List<MediaType> supportedMediaTypes = new ArrayList<MediaType>(readerMediaTypes.length);
		for (String mediaType : readerMediaTypes) {
			supportedMediaTypes.add(MediaType.parseMediaType(mediaType));
		}
		setSupportedMediaTypes(supportedMediaTypes);
		String[] writerMediaTypes = ImageIO.getWriterMIMETypes();
		if (writerMediaTypes.length > 0) {
			contentType = MediaType.parseMediaType(writerMediaTypes[0]);
		}
	}

	/**
	 * Sets the {@link MediaType MediaTypes} supported for reading.
	 *
	 * @throws IllegalArgumentException if the given media type is not supported by the Java Image I/O API
	 */
	@Override
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		Assert.notEmpty(supportedMediaTypes, "'supportedMediaTypes' must not be empty");
		for (MediaType supportedMediaType : supportedMediaTypes) {
			Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByMIMEType(supportedMediaType.toString());
			if (!imageReaders.hasNext()) {
				throw new IllegalArgumentException(
						"MediaType [" + supportedMediaType + "] is not supported by the Java Image I/O API");
			}
		}
		super.setSupportedMediaTypes(supportedMediaTypes);
	}

	/**
	 * Sets the {@code Content-Type} to be used for writing.
	 *
	 * @throws IllegalArgumentException if the given content type is not supported by the Java Image I/O API
	 */
	public void setContentType(MediaType contentType) {
		Assert.notNull(contentType, "'contentType' must not be null");
		Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByMIMEType(contentType.toString());
		if (!imageWriters.hasNext()) {
			throw new IllegalArgumentException(
					"ContentType [" + contentType + "] is not supported by the Java Image I/O API");
		}

		this.contentType = contentType;
	}

	/** Sets the cache directory. If this property is set to an existing directory, this converter will cache image data. */
	public void setCacheDir(File cacheDir) {
		Assert.notNull(cacheDir, "'cacheDir' must not be null");
		Assert.isTrue(cacheDir.isDirectory(), "'cacheDir' is not a directory");
		this.cacheDir = cacheDir;
	}

	public boolean supports(Class<? extends BufferedImage> clazz) {
		return BufferedImage.class.equals(clazz);
	}

	@Override
	public BufferedImage readInternal(Class<BufferedImage> clazz, HttpInputMessage inputMessage) throws IOException {
		ImageInputStream imageInputStream = null;
		ImageReader imageReader = null;
		try {
			imageInputStream = createImageInputStream(inputMessage.getBody());
			MediaType contentType = inputMessage.getHeaders().getContentType();
			Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByMIMEType(contentType.toString());
			if (imageReaders.hasNext()) {
				imageReader = imageReaders.next();
				ImageReadParam irp = imageReader.getDefaultReadParam();
				process(irp);
				imageReader.setInput(imageInputStream, true);
				return imageReader.read(0, irp);
			}
			else {
				throw new HttpMessageNotReadableException(
						"Could not find javax.imageio.ImageReader for Content-Type [" + contentType + "]");
			}
		}
		finally {
			if (imageReader != null) {
				imageReader.dispose();
			}
			if (imageInputStream != null) {
				try {
					imageInputStream.close();
				}
				catch (IOException ex) {
					// ignore
				}
			}
		}
	}

	private ImageInputStream createImageInputStream(InputStream is) throws IOException {
		if (cacheDir != null) {
			return new FileCacheImageInputStream(is, cacheDir);
		}
		else {
			return new MemoryCacheImageInputStream(is);
		}
	}

	@Override
	protected MediaType getContentType(BufferedImage image) {
		return contentType;
	}

	@Override
	protected void writeInternal(BufferedImage image, HttpOutputMessage outputMessage) throws IOException {
		ImageOutputStream imageOutputStream = null;
		ImageWriter imageWriter = null;
		try {
			imageOutputStream = createImageOutputStream(outputMessage.getBody());
			Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByMIMEType(contentType.toString());
			if (imageWriters.hasNext()) {
				imageWriter = imageWriters.next();
				ImageWriteParam iwp = imageWriter.getDefaultWriteParam();
				process(iwp);
				imageWriter.setOutput(imageOutputStream);
				imageWriter.write(null, new IIOImage(image, null, null), iwp);
			}
			else {
				throw new HttpMessageNotWritableException(
						"Could not find javax.imageio.ImageWriter for Content-Type [" + contentType + "]");
			}
		}
		finally {
			if (imageWriter != null) {
				imageWriter.dispose();
			}
			if (imageOutputStream != null) {
				try {
					imageOutputStream.close();
				}
				catch (IOException ex) {
					// ignore
				}
			}
		}
	}

	private ImageOutputStream createImageOutputStream(OutputStream os) throws IOException {
		if (cacheDir != null) {
			return new FileCacheImageOutputStream(os, cacheDir);
		}
		else {
			return new MemoryCacheImageOutputStream(os);
		}
	}

	/**
	 * Template method that allows for manipulating the {@link ImageReadParam} before it is used to read an image.
	 *
	 * <p>Default implementation is empty.
	 */
	protected void process(ImageReadParam irp) {
	}

	/**
	 * Template method that allows for manipulating the {@link ImageWriteParam} before it is used to write an image.
	 *
	 * <p>Default implementation is empty.
	 */
	protected void process(ImageWriteParam iwp) {
	}
}
