/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.aot;

import java.util.function.Consumer;

import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint.Builder;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.support.ClassHintUtils;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.core.io.ByteArrayResource;

/**
 * Handle CGLIB classes by adding them to a {@link GenerationContext},
 * and register the necessary hints so that they can be instantiated.
 *
 * @author Stephane Nicoll
 * @since 6.0
 * @see ReflectUtils#setGeneratedClassHandler
 * @see ReflectUtils#setLoadedClassHandler
 * @see ClassHintUtils#registerProxyIfNecessary
 */
class CglibClassHandler {

	private static final Consumer<Builder> instantiateCglibProxy = hint ->
			hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

	private final RuntimeHints runtimeHints;

	private final GeneratedFiles generatedFiles;


	CglibClassHandler(GenerationContext generationContext) {
		this.runtimeHints = generationContext.getRuntimeHints();
		this.generatedFiles = generationContext.getGeneratedFiles();
	}


	/**
	 * Handle the specified generated CGLIB class.
	 * @param cglibClassName the name of the generated class
	 * @param content the bytecode of the generated class
	 */
	public void handleGeneratedClass(String cglibClassName, byte[] content) {
		registerHints(TypeReference.of(cglibClassName));
		String path = cglibClassName.replace(".", "/") + ".class";
		this.generatedFiles.addFile(Kind.CLASS, path, new ByteArrayResource(content));
	}

	/**
	 * Handle the specified loaded CGLIB class.
	 * @param cglibClass a cglib class that has been loaded
	 */
	public void handleLoadedClass(Class<?> cglibClass) {
		registerHints(TypeReference.of(cglibClass));
	}

	private void registerHints(TypeReference cglibTypeReference) {
		this.runtimeHints.reflection().registerType(cglibTypeReference, instantiateCglibProxy);
	}

}
