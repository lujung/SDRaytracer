package src.main.java.Profiling;

import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.commons.AdviceAdapter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import static jdk.internal.org.objectweb.asm.Opcodes.ASM5;


class SDRaytracerAgent implements ClassFileTransformer {
    // Set ASM to version 5
    private static final int ASMVersion = ASM5;
    static BufferedWriter output;

    public static void premain(String agentArgs, Instrumentation inst) throws IOException {
        // Set up logging
        SDRaytracerAgent.output = new BufferedWriter(new FileWriter(new File("profiling.log")));
        inst.addTransformer(new SDRaytracerAgent());
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] retVal = null;

        if (className.equals("src/main/java/SDRaytracer")) {
            // Add Visitor to SDRaytracer only
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor ca = new SDRaytracerClassVisitor(cw);
            cr.accept(ca, ClassReader.EXPAND_FRAMES | ClassWriter.COMPUTE_FRAMES);
            retVal = cw.toByteArray();
        }
        return retVal;
    }

    class SDRaytracerMethodVisitor extends AdviceAdapter implements Opcodes {
        // Method visitor for the calculation of method runtime and overall call count
        private String name = null;
        private Integer id = null;

        SDRaytracerMethodVisitor(int access, String name, String desc, MethodVisitor mv) {
            super(ASMVersion, mv, access, name, desc);
            this.name = name;
        }

        @Override
        protected void onMethodEnter() {
            this.id = newLocal(Type.LONG_TYPE);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
            mv.visitVarInsn(LSTORE, id);
            super.onMethodEnter();
        }

        @Override
        protected void onMethodExit(int opcode) {
            mv.visitLdcInsn(name);
            mv.visitVarInsn(LLOAD, id);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
            mv.visitMethodInsn(INVOKESTATIC, "src/main/java/Profiling/Logging", "log", "(Ljava/lang/String;JJ)V", false);
            super.onMethodExit(opcode);
        }

    }

    public class SDRaytracerClassVisitor extends ClassVisitor {
        // Class visitor which visits methods
        private ClassVisitor cv;

        SDRaytracerClassVisitor(ClassVisitor cv) {
            super(ASMVersion, cv);
            this.cv = cv;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            return new SDRaytracerMethodVisitor(access, name, desc, mv);
        }

    }

}
