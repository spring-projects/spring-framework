/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core.type.classreading;

import java.io.IOException;
import java.util.Objects;

import org.springframework.asm.Type;
import org.springframework.core.type.ArrayTypeMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.ClassMetadataNotFoundException;
import org.springframework.core.type.TypeMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Simple implementation of {@link TypeMetadata}.
 *
 * @author Danny Thomas
 */
class SimpleTypeMetadata implements TypeMetadata {

	private final Type type;

	@Nullable
	private final MetadataReaderFactory metadataReaderFactory;

	@Nullable
	private volatile ClassMetadata classMetadata;

	SimpleTypeMetadata(Type type, MetadataReaderFactory metadataReaderFactory) {
		this(type, metadataReaderFactory, null);
	}

	SimpleTypeMetadata(ClassMetadata classMetadata) {
		this(Type.getObjectType(classMetadata.getClassName()), null, classMetadata);
	}

	private SimpleTypeMetadata(Type type, @Nullable MetadataReaderFactory metadataReaderFactory, @Nullable ClassMetadata classMetadata) {
		Assert.isTrue(metadataReaderFactory != null || classMetadata != null, "Either a metadata reader factory or class metadata must be provided");
		this.type = type;
		this.metadataReaderFactory = metadataReaderFactory;
		this.classMetadata = classMetadata;
	}

	@Override
	public boolean equals(Object o) {
		return this.type.equals(o);
	}

	@Override
	public int hashCode() {
		return this.type.hashCode();
	}

	@Override
	public String getTypeName() {
		return this.type.getClassName();
	}

	@Override
	public boolean isPrimitive() {
		return this.type.getSort() != Type.OBJECT;
	}

	@Override
	public boolean isVoid() {
		return this.type.getSort() == Type.VOID;
	}

	@Override
	public ClassMetadata getClassMetadata() {
		if (this.classMetadata != null) {
			return this.classMetadata;
		}
		if (isPrimitive() || !(this instanceof ArrayTypeMetadata)) {
			return null;
		}
		Objects.requireNonNull(this.metadataReaderFactory, "Metadata reader factory must be provided");
		String typeName = getTypeName();
		try {
			MetadataReader reader = this.metadataReaderFactory.getMetadataReader(typeName);
			this.classMetadata = reader.getAnnotationMetadata();
			return this.classMetadata;
		}
		catch (IOException ex) {
			throw new ClassMetadataNotFoundException("Could not load metadata for class " + typeName, ex);
		}
	}

}
