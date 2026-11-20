/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.agent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.asm.ClassVisitor;
import org.springframework.asm.ClassWriter;
import org.springframework.asm.Handle;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.SpringAsmInfo;

/**
 * ASM {@link ClassVisitor} that rewrites a known set of method invocations
 * to call instrumented bridge methods for {@link RecordedInvocationsPublisher recording purposes}.
 * <p>The bridge methods are located in the {@link InstrumentedBridgeMethods} class.
 *
 * @author Brian Clozel
 * @see InstrumentedMethod
 */
class InvocationsRecorderClassVisitor extends ClassVisitor implements Opcodes {

	private boolean isTransformed;

	private final ClassWriter classWriter;


	public InvocationsRecorderClassVisitor() {
		this(new ClassWriter(ClassWriter.COMPUTE_MAXS));
	}

	private InvocationsRecorderClassVisitor(ClassWriter classWriter) {
		super(SpringAsmInfo.ASM_VERSION, classWriter);
		this.classWriter = classWriter;
	}


	public boolean isTransformed() {
		return this.isTransformed;
	}

	public byte[] getTransformedClassBuffer() {
		return this.classWriter.toByteArray();
	}


	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		return new InvocationsRecorderMethodVisitor(mv);
	}


	@SuppressWarnings("deprecation")
	class InvocationsRecorderMethodVisitor extends MethodVisitor implements Opcodes {

		private static final String INSTRUMENTED_CLASS = InstrumentedBridgeMethods.class.getName().replace('.', '/');

		private static final Set<String> instrumentedMethods = new HashSet<>();

		static {
			for (InstrumentedMethod method : InstrumentedMethod.values()) {
				MethodReference methodReference = method.methodReference();
				instrumentedMethods.add(methodReference.getClassName().replace('.', '/') +
						"#" + methodReference.getMethodName());
			}
		}

		public InvocationsRecorderMethodVisitor(MethodVisitor mv) {
			super(SpringAsmInfo.ASM_VERSION, mv);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			if (isOpcodeSupported(opcode) && shouldRecordMethodCall(owner, name)) {
				String instrumentedMethodName = rewriteMethodName(owner, name);
				mv.visitMethodInsn(INVOKESTATIC, INSTRUMENTED_CLASS, instrumentedMethodName,
						rewriteDescriptor(opcode, owner, name, descriptor), false);
				isTransformed = true;
			}
			else {
				super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
			}
		}

		private boolean isOpcodeSupported(int opcode) {
			return Opcodes.INVOKEVIRTUAL == opcode || Opcodes.INVOKESTATIC == opcode;
		}

		@Override
		public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
			for (int i = 0; i < bootstrapMethodArguments.length; i++) {
				if (bootstrapMethodArguments[i] instanceof Handle argumentHandle) {
					if (shouldRecordMethodCall(argumentHandle.getOwner(), argumentHandle.getName())) {
						String instrumentedMethodName = rewriteMethodName(argumentHandle.getOwner(), argumentHandle.getName());
						String newDescriptor = rewriteDescriptor(argumentHandle.getTag(), argumentHandle.getOwner(), argumentHandle.getName(), argumentHandle.getDesc());
						bootstrapMethodArguments[i] = new Handle(H_INVOKESTATIC, INSTRUMENTED_CLASS, instrumentedMethodName, newDescriptor, false);
						isTransformed = true;
					}
				}
			}
			super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
		}

		private boolean shouldRecordMethodCall(String owner, String method) {
			String methodReference = owner + "#" + method;
			return instrumentedMethods.contains(methodReference);
		}

		private String rewriteMethodName(String owner, String methodName) {
			int classIndex = owner.lastIndexOf('/');
			return owner.substring(classIndex + 1).toLowerCase(Locale.ROOT) + methodName;
		}

		private String rewriteDescriptor(int opcode, String owner, String name, String descriptor) {
			return (opcode == Opcodes.INVOKESTATIC || opcode == Opcodes.H_INVOKESTATIC) ? descriptor : "(L" + owner + ";" + descriptor.substring(1);
		}
	}

}
