/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.expression.spel.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.asm.ClassWriter;
import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelNode;

/**
 * Represent a list in an expression, e.g. '{1,2,3}'
 *
 * @author Andy Clement
 * @since 3.0.4
 */
public class InlineList extends SpelNodeImpl {

	// If the list is purely literals, it is a constant value and can be computed and cached
	private TypedValue constant = null;  // TODO must be immutable list


	public InlineList(int pos, SpelNodeImpl... args) {
		super(pos, args);
		checkIfConstant();
	}


	/**
	 * If all the components of the list are constants, or lists that themselves contain constants, then a constant list
	 * can be built to represent this node. This will speed up later getValue calls and reduce the amount of garbage
	 * created.
	 */
	private void checkIfConstant() {
		boolean isConstant = true;
		for (int c = 0, max = getChildCount(); c < max; c++) {
			SpelNode child = getChild(c);
			if (!(child instanceof Literal)) {
				if (child instanceof InlineList) {
					InlineList inlineList = (InlineList) child;
					if (!inlineList.isConstant()) {
						isConstant = false;
					}
				}
				else {
					isConstant = false;
				}
			}
		}
		if (isConstant) {
			List<Object> constantList = new ArrayList<Object>();
			int childcount = getChildCount();
			for (int c = 0; c < childcount; c++) {
				SpelNode child = getChild(c);
				if ((child instanceof Literal)) {
					constantList.add(((Literal) child).getLiteralValue().getValue());
				}
				else if (child instanceof InlineList) {
					constantList.add(((InlineList) child).getConstantValue());
				}
			}
			this.constant = new TypedValue(Collections.unmodifiableList(constantList));
		}
	}

	@Override
	public TypedValue getValueInternal(ExpressionState expressionState) throws EvaluationException {
		if (this.constant != null) {
			return this.constant;
		}
		else {
			List<Object> returnValue = new ArrayList<Object>();
			int childCount = getChildCount();
			for (int c = 0; c < childCount; c++) {
				returnValue.add(getChild(c).getValue(expressionState));
			}
			return new TypedValue(returnValue);
		}
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder("{");
		// String ast matches input string, not the 'toString()' of the resultant collection, which would use []
		int count = getChildCount();
		for (int c = 0; c < count; c++) {
			if (c > 0) {
				sb.append(",");
			}
			sb.append(getChild(c).toStringAST());
		}
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Return whether this list is a constant value.
	 */
	public boolean isConstant() {
		return (this.constant != null);
	}

	@SuppressWarnings("unchecked")
	public List<Object> getConstantValue() {
		return (List<Object>) this.constant.getValue();
	}
	
	@Override
	public boolean isCompilable() {
		return isConstant();
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
		final String constantFieldName = "inlineList$"+codeflow.nextFieldId();
		final String clazzname = codeflow.getClassname();

		codeflow.registerNewField(new CodeFlow.FieldAdder() {
			public void generateField(ClassWriter cw, CodeFlow codeflow) {
				cw.visitField(ACC_PRIVATE|ACC_STATIC|ACC_FINAL, constantFieldName, "Ljava/util/List;", null, null);
			}
		});
		
		codeflow.registerNewClinit(new CodeFlow.ClinitAdder() {
			public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
				generateClinitCode(clazzname,constantFieldName, mv,codeflow,false);
			}
		});
		
		mv.visitFieldInsn(GETSTATIC, clazzname, constantFieldName, "Ljava/util/List;");
		codeflow.pushDescriptor("Ljava/util/List");
	}
	
	void generateClinitCode(String clazzname, String constantFieldName, MethodVisitor mv, CodeFlow codeflow, boolean nested) {
		mv.visitTypeInsn(NEW, "java/util/ArrayList");
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
		if (!nested) {
			mv.visitFieldInsn(PUTSTATIC, clazzname, constantFieldName, "Ljava/util/List;");
		}
		int childcount = getChildCount();		
		for (int c=0; c < childcount; c++) {
			if (!nested) {
				mv.visitFieldInsn(GETSTATIC, clazzname, constantFieldName, "Ljava/util/List;");
			}
			else {
				mv.visitInsn(DUP);
			}
			// The children might be further lists if they are not constants. In this
			// situation do not call back into generateCode() because it will register another clinit adder.
			// Instead, directly build the list here:
			if (children[c] instanceof InlineList) {
				((InlineList)children[c]).generateClinitCode(clazzname, constantFieldName, mv, codeflow, true);
			}
			else {
				children[c].generateCode(mv, codeflow);
				if (CodeFlow.isPrimitive(codeflow.lastDescriptor())) {
					CodeFlow.insertBoxIfNecessary(mv, codeflow.lastDescriptor().charAt(0));
				}
			}
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
			mv.visitInsn(POP);
		}
	}

}
