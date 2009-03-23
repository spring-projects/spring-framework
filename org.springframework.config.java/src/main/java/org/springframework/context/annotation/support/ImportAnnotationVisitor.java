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
package org.springframework.context.annotation.support;

import static java.lang.String.*;
import static org.springframework.context.annotation.support.AsmUtils.*;
import static org.springframework.util.ClassUtils.*;

import java.util.ArrayList;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassReader;
import org.springframework.asm.Type;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.context.annotation.Import;
import org.springframework.util.Assert;


/** 
 * ASM {@link AnnotationVisitor} implementation that reads an {@link Import} annotation
 * for all its specified classes and then one by one processes each class with a new
 * {@link ConfigurationClassVisitor}.
 * 
 * @author Chris Beams
 * @see Import
 * @see ImportStack
 * @see ImportStackHolder
 * @see ConfigurationClassVisitor
 */
class ImportAnnotationVisitor extends AnnotationAdapter {
	private final ArrayList<String> classesToImport = new ArrayList<String>();
	private final ConfigurationModel model;
	private final ProblemReporter problemReporter;
	private final ClassLoader classLoader;

	public ImportAnnotationVisitor(ConfigurationModel model, ProblemReporter problemReporter, ClassLoader classLoader) {
		super(AsmUtils.EMPTY_VISITOR);
		this.model = model;
		this.problemReporter = problemReporter;
		this.classLoader = classLoader;
	}

	@Override
	public AnnotationVisitor visitArray(String attribName) {
		Assert.isTrue("value".equals(attribName),
				format("expected 'value' attribute, got unknown '%s' attribute", attribName));

		return new AnnotationAdapter(AsmUtils.EMPTY_VISITOR) {
			@Override
			public void visit(String na, Object type) {
				Assert.isInstanceOf(Type.class, type);
				classesToImport.add(((Type) type).getClassName());
			}
		};
	}

	@Override
	public void visitEnd() {
		for (String classToImport : classesToImport)
			processClassToImport(classToImport);

		ImportStackHolder.getImportStack().pop();
	}

	private void processClassToImport(String classToImport) {
		ConfigurationClass configClass = new ConfigurationClass();

		ClassReader reader = newClassReader(convertClassNameToResourcePath(classToImport), classLoader);

		reader.accept(new ConfigurationClassVisitor(configClass, model, problemReporter, classLoader), false);

		model.add(configClass);
	}

}
