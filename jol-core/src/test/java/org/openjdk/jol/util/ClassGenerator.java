package org.openjdk.jol.util;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

public class ClassGenerator {

    private static final int MAX_CLASSES_IN_HIERARCHY = 5;
    private static final int MAX_FIELDS_PER_CLASS = 50;
    private static final int CLASSFILE_VERSION = 50;

    private static final AtomicInteger idx = new AtomicInteger();

    public static Class<?> generate(Random r) throws Exception {
        ByteClassLoader classLoader = new ByteClassLoader();

        int numClasses = r.nextInt(MAX_CLASSES_IN_HIERARCHY);
        Class<?> sup = Object.class;
        for (int c = 0; c < numClasses; c++) {
            sup = generate(r, sup, classLoader);
        }
        return sup;
    }

    private static Class<?> generate(Random r, Class<?> superClass, ByteClassLoader classLoader) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        String name = "Class" + idx.incrementAndGet();

        cw.visit(CLASSFILE_VERSION,
                ACC_PUBLIC + ACC_SUPER,
                name,
                null,
                Type.getInternalName(superClass),
                new String[0]);

        cw.visitSource(name + ".java", null);

        Class<?>[] types = new Class[] { boolean.class, byte.class, short.class, char.class, int.class, float.class, long.class, double.class, Object.class };

        int count = r.nextInt(MAX_FIELDS_PER_CLASS);
        for (int c = 0; c < count; c++) {
            Class<?> type = types[r.nextInt(types.length)];
            cw.visitField(ACC_PUBLIC, "field" + c, Type.getType(type).getDescriptor(), null, null);
        }

        cw.visitEnd();

        classLoader.put(name, cw.toByteArray());
        return classLoader.findClass(name);
    }

    public static class ByteClassLoader extends URLClassLoader {
        private final Map<String, byte[]> customClasses;

        public ByteClassLoader() {
            super(new URL[] {});
            customClasses = new HashMap<String, byte[]>();
        }

        @Override
        protected Class<?> findClass(final String name) throws ClassNotFoundException {
            if (customClasses.containsKey(name)) {
                byte[] bs = customClasses.get(name);
                return defineClass(name, bs, 0, bs.length);
            }
            return super.findClass(name);
        }

        public void put(String name, byte[] bytes) {
            customClasses.put(name, bytes);
        }
    }

}