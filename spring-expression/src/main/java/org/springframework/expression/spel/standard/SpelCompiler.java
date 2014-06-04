/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.expression.spel.standard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.springframework.asm.ClassWriter;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.CompiledExpression;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.util.ClassUtils;

/**
 * A SpelCompiler will take a regular parsed expression and create (and load) a class
 * containing byte code that does the same thing as that expression. The compiled form of
 * an expression will evaluate far faster than the interpreted form.
 * <p>
 * The SpelCompiler is not currently handling all expression types but covers many of the
 * common cases. The framework is extensible to cover more cases in the future. For
 * absolute maximum speed there is *no checking* in the compiled code. The compiled
 * version of the expression uses information learned during interpreted runs of the
 * expression when it generates the byte code. For example if it knows that a particular
 * property dereference always seems to return a Map then it will generate byte code that
 * expects the result of the property dereference to be a Map. This ensures maximal
 * performance but should the dereference result in something other than a map, the
 * compiled expression will fail - like a ClassCastException would occur if passing data
 * of an unexpected type in a regular Java program.
 * <p>
 * Due to the lack of checking there are likely some expressions that should never be
 * compiled, for example if an expression is continuously dealing with different types of
 * data. Due to these cases the compiler is something that must be selectively turned on
 * for an associated SpelExpressionParser (through the {@link SpelParserConfiguration}
 * object), it is not on by default.
 * <p>
 * Individual expressions can be compiled by calling
 * <tt>SpelCompiler.compile(expression)</tt>.
 *
 * @author Andy Clement
 * @since 4.1
 */
public class SpelCompiler implements Opcodes {

	// Default number of times to interpret an expression before compiling it
	private static int DEFAULT_INTERPRETED_COUNT_THRESHOLD = 100;

	// Once an expression is evaluated the threshold number of times, it will be a candidate for compilation
	public static int interpretedCountThreshold = DEFAULT_INTERPRETED_COUNT_THRESHOLD;

	// Useful for debugging
	public static final boolean verbose = false;

	// A compiler is created for each classloader, it manages a child class loader of that
	// classloader and the child is used to load the compiled expressions.
	private static Map<ClassLoader,SpelCompiler> compilers = Collections.synchronizedMap(new WeakHashMap<ClassLoader,SpelCompiler>());

	// The child classloader used to load the compiled expression classes
	private ChildClassLoader ccl;

	// counter suffix for generated classes within this SpelCompiler instance
	private int suffixId;

	/**
	 * Factory method for compiler instances. The returned SpelCompiler will
	 * attach a class loader as the child of the default class loader and this
	 * child will be used to load compiled expressions.
	 *
	 * @return a SpelCompiler instance
	 */
	public static SpelCompiler getCompiler() {
		ClassLoader classloader = ClassUtils.getDefaultClassLoader();
		synchronized (compilers) {
			SpelCompiler compiler = compilers.get(classloader);
			if (compiler == null) {
				compiler = new SpelCompiler(classloader);
				compilers.put(classloader,compiler);
			}
			return compiler;
		}
	}

	private SpelCompiler(ClassLoader classloader) {
		this.ccl = new ChildClassLoader(classloader);
		this.suffixId = 1;
	}

	/**
	 * Attempt compilation of the supplied expression. A check is
	 * made to see if it is compilable before compilation proceeds. The
	 * check involves visiting all the nodes in the expression Ast and
	 * ensuring enough state is known about them that bytecode can
	 * be generated for them.
	 * @param expression the expression to compile
	 * @return an instance of the class implementing the compiled expression, or null
	 * if compilation is not possible
	 */
	public CompiledExpression compile(SpelNodeImpl expression) {
		if (expression.isCompilable()) {
			if (verbose) {
				System.out.println("SpEL: compiling " + expression.toStringAST());
			}
			Class<? extends CompiledExpression> clazz = createExpressionClass(expression);
			try {
				CompiledExpression instance = clazz.newInstance();
				return instance;
			}
			catch (InstantiationException ie) {
				ie.printStackTrace();
			}
			catch (IllegalAccessException iae) {
				iae.printStackTrace();
			}
		}
		else {
			if (verbose) {
				System.out.println("SpEL: unable to compile " + expression.toStringAST());
			}
		}
		return null;
	}

	private synchronized int getNextSuffix() {
		return suffixId++;
	}

	/**
	 * Generate the class that encapsulates the compiled expression and define it. The
	 * generated class will be a subtype of CompiledExpression.
	 * @param expressionToCompile the expression to be compiled
	 */
	@SuppressWarnings("unchecked")
	private Class<? extends CompiledExpression> createExpressionClass(SpelNodeImpl expressionToCompile) {

		// Create class outline 'spel/ExNNN extends org.springframework.expression.spel.CompiledExpression'
		String clazzName = "spel/Ex" + getNextSuffix();
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
		cw.visit(V1_5, ACC_PUBLIC, clazzName, null,
				"org/springframework/expression/spel/CompiledExpression", null);

		// Create default constructor
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "org/springframework/expression/spel/CompiledExpression", "<init>", "()V",false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();

		// Create getValue() method
		mv = cw.visitMethod(ACC_PUBLIC, "getValue", "(Ljava/lang/Object;Lorg/springframework/expression/EvaluationContext;)Ljava/lang/Object;", null,
				new String[]{"org/springframework/expression/EvaluationException"});
		mv.visitCode();

		CodeFlow codeflow = new CodeFlow();

		// Ask the expression Ast to generate the body of the method
		expressionToCompile.generateCode(mv,codeflow);

		CodeFlow.insertBoxIfNecessary(mv,codeflow.lastDescriptor());
		if (codeflow.lastDescriptor() == "V") {
			mv.visitInsn(ACONST_NULL);
		}
		mv.visitInsn(ARETURN);

		mv.visitMaxs(0,0); // not supplied due to COMPUTE_MAXS
		mv.visitEnd();
		cw.visitEnd();
		byte[] data = cw.toByteArray();
		// TODO need to make this conditionally occur based on a debug flag
		// dump(expressionToCompile.toStringAST(), clazzName, data);
		Class<? extends CompiledExpression> clazz = (Class<? extends CompiledExpression>) ccl.defineClass(clazzName.replaceAll("/","."),data);
		return clazz;
	}

	/**
	 * For debugging purposes, dump the specified byte code into a file on the disk. Not
	 * yet hooked in, needs conditionally calling based on a sys prop.
	 *
	 * @param expressionText the text of the expression compiled
	 * @param name the name of the class being used for the compiled expression
	 * @param bytecode the bytecode for the generated class
	 */
	@SuppressWarnings("unused")
	private static void dump(String expressionText, String name, byte[] bytecode) {
		name = name.replace('.', '/');
		String dir = "";
		if (name.indexOf('/') != -1) {
			dir = name.substring(0, name.lastIndexOf('/'));
		}
		String dumplocation = null;
		try {
			File tempfile = null;
			tempfile = File.createTempFile("tmp", null);
			tempfile.delete();
			File f = new File(tempfile, dir);
			f.mkdirs();
			dumplocation = tempfile + File.separator + name + ".class";
			System.out.println("Expression '" + expressionText + "' compiled code dumped to "
					+ dumplocation);
			f = new File(dumplocation);
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(bytecode);
			fos.flush();
			fos.close();
		}
		catch (IOException ioe) {
			throw new IllegalStateException("Unexpected problem dumping class "
					+ name + " into " + dumplocation, ioe);
		}
	}


	/**
	 * A ChildClassLoader will load the generated compiled expression classes
	 */
	public static class ChildClassLoader extends URLClassLoader {

		private static URL[] NO_URLS = new URL[0];

		public ChildClassLoader(ClassLoader classloader) {
			super(NO_URLS, classloader);
		}

		public Class<?> defineClass(String name, byte[] bytes) {
			return super.defineClass(name, bytes, 0, bytes.length);
		}

	}

	/**
	 * Request that an attempt is made to compile the specified expression. It may fail if
	 * components of the expression are not suitable for compilation or the data types
	 * involved are not suitable for compilation. Used for testing.
	 * @return true if the expression was successfully compiled
	 */
	public static boolean compile(Expression expression) {
		if (expression instanceof SpelExpression) {
			SpelExpression spelExpression = (SpelExpression)expression;
			return spelExpression.compileExpression();
		}
		return false;
	}

	/**
	 * Request to revert to the interpreter for expression evaluation. Any compiled form
	 * is discarded but can be recreated by later recompiling again.
	 * @param expression the expression
	 */
	public static void revertToInterpreted(Expression expression) {
		if (expression instanceof SpelExpression) {
			SpelExpression spelExpression = (SpelExpression)expression;
			spelExpression.revertToInterpreted();
		}
	}
}
