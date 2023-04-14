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

import java.util.function.Consumer;

import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.Type;
import org.springframework.asm.signature.SignatureVisitor;

/**
 * A {@link SignatureVisitor} producing {@link SimpleMethodSignature}.
 *
 * @author Danny Thomas
 */
public class SimpleMethodSignatureVisitor extends SignatureVisitor {

	static final String OBJECT_TYPE_DESCRIPTOR = Type.getObjectType(Object.class.getName()).getDescriptor();

	private final MetadataReaderFactory metadataReaderFactory;

	private final Consumer<SimpleMethodSignature> consumer;

	private final SimpleMethodSignature methodSignature;

	protected SimpleMethodSignatureVisitor(MetadataReaderFactory metadataReaderFactory,
			Consumer<SimpleMethodSignature> consumer) {
		super(SpringAsmInfo.ASM_VERSION);
		this.metadataReaderFactory = metadataReaderFactory;
		this.consumer = consumer;
		this.methodSignature = new SimpleMethodSignature();
	}

	@Override
	public SignatureVisitor visitParameterType() {
		return new SimpleTypeMetadataSignatureVisitor(OBJECT_TYPE_DESCRIPTOR, this.metadataReaderFactory,
				this.methodSignature::addTypeParameter);
	}

	@Override
	public SignatureVisitor visitReturnType() {
		return new SimpleTypeMetadataSignatureVisitor(OBJECT_TYPE_DESCRIPTOR, this.metadataReaderFactory, returnType -> {
			this.methodSignature.setReturnType(returnType);
			this.consumer.accept(this.methodSignature);
		});
	}

}
