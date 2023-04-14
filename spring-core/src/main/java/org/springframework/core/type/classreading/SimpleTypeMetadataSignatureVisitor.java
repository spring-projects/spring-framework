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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.Type;
import org.springframework.asm.signature.SignatureVisitor;
import org.springframework.core.type.TypeMetadata;
import org.springframework.lang.Nullable;

import static org.springframework.core.type.classreading.SimpleMethodSignatureVisitor.OBJECT_TYPE_DESCRIPTOR;

/**
 * A {@link SignatureVisitor} producing {@link TypeMetadata}.
 *
 * @author Danny Thomas
 */
public class SimpleTypeMetadataSignatureVisitor extends SignatureVisitor {

	private final String descriptor;

	private final MetadataReaderFactory metadataReaderFactory;

	private final Consumer<TypeMetadata> consumer;

	private final List<TypeMetadata> parameterTypes;

	@Nullable
	private String classType;

	private boolean isArray;

	SimpleTypeMetadataSignatureVisitor(String descriptor, MetadataReaderFactory metadataReaderFactory,
				Consumer<TypeMetadata> consumer) {
		super(SpringAsmInfo.ASM_VERSION);
		this.descriptor = descriptor;
		this.metadataReaderFactory = metadataReaderFactory;
		this.consumer = consumer;
		this.parameterTypes = new ArrayList<>();
	}

	@Override
	public void visitClassType(String name) {
		this.classType = 'L' + name + ';';
	}

	@Override
	public void visitBaseType(char descriptor) {
		produceTypeMetadata(String.valueOf(descriptor));
	}

	@Override
	public void visitTypeVariable(String name) {
		// Raw type variable, we purposefully don't try and resolve the bounds. Fallback
		// to the raw type
		produceTypeMetadata(this.descriptor);
	}

	@Override
	public SignatureVisitor visitArrayType() {
		this.isArray = true;
		return this;
	}

	@Override
	public SignatureVisitor visitTypeArgument(char wildcard) {
		return new SimpleTypeMetadataSignatureVisitor(OBJECT_TYPE_DESCRIPTOR, this.metadataReaderFactory,
				this.parameterTypes::add);
	}

	private void produceTypeMetadata(String descriptor) {
		Objects.requireNonNull(descriptor);
		Type type = Type.getType(descriptor);
		TypeMetadata typeMetadata = this.parameterTypes.isEmpty() ? new SimpleTypeMetadata(type, this.metadataReaderFactory)
				: new SimpleParameterizedTypeMetadata(type, this.parameterTypes, this.metadataReaderFactory);
		if (this.isArray) {
			typeMetadata = new SimpleArrayTypeMetadata(typeMetadata, this.metadataReaderFactory);
		}
		this.consumer.accept(typeMetadata);
	}

	@Override
	public void visitEnd() {
		produceTypeMetadata(this.classType);
	}

}
