/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.test.generate;

import java.util.function.UnaryOperator;

import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.ClassName;

/**
 * {@link GenerationContext} test implementation that uses
 * {@link InMemoryGeneratedFiles} and can configure a {@link TestCompiler}
 * instance.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.0
 */
public class TestGenerationContext extends DefaultGenerationContext implements UnaryOperator<TestCompiler> {

	/**
	 * The default test target {@link ClassName}.
	 */
	public static final ClassName TEST_TARGET = ClassName.get("com.example", "TestTarget");

	/**
	 * Create an instance using the specified {@link ClassNameGenerator}.
	 * @param classNameGenerator the class name generator to use
	 */
	public TestGenerationContext(ClassNameGenerator classNameGenerator) {
		super(classNameGenerator, new InMemoryGeneratedFiles());
	}

	/**
	 * Create an instance using the specified {@code target}.
	 * @param target the default target class name to use
	 */
	public TestGenerationContext(ClassName target) {
		this(new ClassNameGenerator(target));
	}

	/**
	 * Create an instance using the specified {@code target}.
	 * @param target the default target class to use
	 */
	public TestGenerationContext(Class<?> target) {
		this(ClassName.get(target));
	}


	/**
	 * Create an instance using {@link #TEST_TARGET} as the {@code target}.
	 */
	public TestGenerationContext() {
		this(TEST_TARGET);
	}


	@Override
	public InMemoryGeneratedFiles getGeneratedFiles() {
		return (InMemoryGeneratedFiles) super.getGeneratedFiles();
	}

	/**
	 * Configure the specified {@link TestCompiler} with the state of this context.
	 * @param testCompiler the compiler to configure
	 * @return a new {@link TestCompiler} instance configured with the generated files
	 * @see TestCompiler#with(UnaryOperator)
	 */
	@Override
	public TestCompiler apply(TestCompiler testCompiler) {
		return CompilerFiles.from(getGeneratedFiles()).apply(testCompiler);
	}

}
