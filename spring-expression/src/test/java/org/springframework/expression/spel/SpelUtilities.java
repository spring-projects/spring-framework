/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
