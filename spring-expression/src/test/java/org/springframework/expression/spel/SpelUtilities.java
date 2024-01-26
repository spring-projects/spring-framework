/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.expression.spel;

import java.io.PrintStream;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpression;

/**
 * Utilities for working with Spring Expressions.
 *
 * @author Andy Clement
 */
public class SpelUtilities {

	/**
	 * Output an indented representation of the expression syntax tree to the specified output stream.
	 * @param printStream the output stream to print into
	 * @param expression the expression to be displayed
	 */
	public static void printAbstractSyntaxTree(PrintStream printStream, Expression expression) {
		printStream.println("===> Expression '" + expression.getExpressionString() + "' - AST start");
		printAST(printStream, ((SpelExpression) expression).getAST(), "");
		printStream.println("===> Expression '" + expression.getExpressionString() + "' - AST end");
	}

	/*
	 * Helper method for printing the AST with indentation
	 */
	private static void printAST(PrintStream out, SpelNode t, String indent) {
		if (t != null) {
			StringBuilder sb = new StringBuilder();
			sb.append(indent).append(t.getClass().getSimpleName());
			sb.append("  value:").append(t.toStringAST());
			sb.append(t.getChildCount() < 2 ? "" : "  #children:" + t.getChildCount());
			out.println(sb);
			for (int i = 0; i < t.getChildCount(); i++) {
				printAST(out, t.getChild(i), indent + "  ");
			}
		}
	}

}
