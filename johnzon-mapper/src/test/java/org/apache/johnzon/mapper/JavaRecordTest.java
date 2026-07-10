/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.johnzon.mapper;

import static org.apache.xbean.asm9.Opcodes.ACC_FINAL;
import static org.apache.xbean.asm9.Opcodes.ACC_PRIVATE;
import static org.apache.xbean.asm9.Opcodes.ACC_PUBLIC;
import static org.apache.xbean.asm9.Opcodes.ACC_RECORD;
import static org.apache.xbean.asm9.Opcodes.ACC_STATIC;
import static org.apache.xbean.asm9.Opcodes.ALOAD;
import static org.apache.xbean.asm9.Opcodes.ARETURN;
import static org.apache.xbean.asm9.Opcodes.GETFIELD;
import static org.apache.xbean.asm9.Opcodes.ILOAD;
import static org.apache.xbean.asm9.Opcodes.INVOKESPECIAL;
import static org.apache.xbean.asm9.Opcodes.IRETURN;
import static org.apache.xbean.asm9.Opcodes.PUTFIELD;
import static org.apache.xbean.asm9.Opcodes.RETURN;
import static org.apache.xbean.asm9.Opcodes.V16;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import org.apache.xbean.asm9.ClassWriter;
import org.apache.xbean.asm9.MethodVisitor;
import org.junit.Test;

/**
 * Serialization of real java records (not {@link JohnzonRecord} classes) must only
 * expose the record components as properties, not any public no-arg method.
 *
 * The module compiles at a pre-record language level so the record under test is
 * built with asm; the test is skipped when the JVM cannot load record class files.
 */
public class JavaRecordTest {
    @Test
    public void onlyComponentsAreProperties() throws Exception {
        assumeTrue("records require java 16+", Runtime.version().feature() >= 16);

        // a record carrying the extra no-arg methods a Lombok-style builder generates:
        // neither toBuilder() (instance) nor builder() (static) is a component so
        // neither must serialize
        final Class<?> type = definePersonRecord();
        final Object ref = type.getConstructor(String.class, int.class).newInstance("Ada", 36);

        try (final Mapper mapper = new MapperBuilder().setAttributeOrder(String.CASE_INSENSITIVE_ORDER).build()) {
            final String expectedJson = "{\"age\":36,\"name\":\"Ada\"}";
            assertEquals(expectedJson, mapper.writeObjectAsString(ref));

            final Object read = mapper.readObject(expectedJson, type);
            assertEquals("Ada", type.getMethod("name").invoke(read));
            assertEquals(36, type.getMethod("age").invoke(read));
        }
    }

    /**
     * public record Person(String name, int age) {
     *     public String toBuilder() { return "bogus"; }
     *     public static String builder() { return "bogus"; }
     * }
     */
    private Class<?> definePersonRecord() {
        final String name = "org.apache.johnzon.mapper.generated.Person";
        final String internalName = name.replace('.', '/');

        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V16, ACC_PUBLIC | ACC_FINAL | ACC_RECORD, internalName, null, "java/lang/Record", null);

        cw.visitRecordComponent("name", "Ljava/lang/String;", null).visitEnd();
        cw.visitRecordComponent("age", "I", null).visitEnd();

        cw.visitField(ACC_PRIVATE | ACC_FINAL, "name", "Ljava/lang/String;", null, null).visitEnd();
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "age", "I", null, null).visitEnd();

        // canonical constructor, MethodParameters included as javac emits it
        final MethodVisitor ctor = cw.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/String;I)V", null, null);
        ctor.visitParameter("name", 0);
        ctor.visitParameter("age", 0);
        ctor.visitCode();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Record", "<init>", "()V", false);
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitVarInsn(ALOAD, 1);
        ctor.visitFieldInsn(PUTFIELD, internalName, "name", "Ljava/lang/String;");
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitVarInsn(ILOAD, 2);
        ctor.visitFieldInsn(PUTFIELD, internalName, "age", "I");
        ctor.visitInsn(RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();

        final MethodVisitor nameAccessor = cw.visitMethod(ACC_PUBLIC, "name", "()Ljava/lang/String;", null, null);
        nameAccessor.visitCode();
        nameAccessor.visitVarInsn(ALOAD, 0);
        nameAccessor.visitFieldInsn(GETFIELD, internalName, "name", "Ljava/lang/String;");
        nameAccessor.visitInsn(ARETURN);
        nameAccessor.visitMaxs(0, 0);
        nameAccessor.visitEnd();

        final MethodVisitor ageAccessor = cw.visitMethod(ACC_PUBLIC, "age", "()I", null, null);
        ageAccessor.visitCode();
        ageAccessor.visitVarInsn(ALOAD, 0);
        ageAccessor.visitFieldInsn(GETFIELD, internalName, "age", "I");
        ageAccessor.visitInsn(IRETURN);
        ageAccessor.visitMaxs(0, 0);
        ageAccessor.visitEnd();

        final MethodVisitor toBuilder = cw.visitMethod(ACC_PUBLIC, "toBuilder", "()Ljava/lang/String;", null, null);
        toBuilder.visitCode();
        toBuilder.visitLdcInsn("bogus");
        toBuilder.visitInsn(ARETURN);
        toBuilder.visitMaxs(0, 0);
        toBuilder.visitEnd();

        final MethodVisitor builder = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "builder", "()Ljava/lang/String;", null, null);
        builder.visitCode();
        builder.visitLdcInsn("bogus");
        builder.visitInsn(ARETURN);
        builder.visitMaxs(0, 0);
        builder.visitEnd();

        cw.visitEnd();
        final byte[] bytes = cw.toByteArray();

        return new ClassLoader(JavaRecordTest.class.getClassLoader()) {
            Class<?> define() {
                return defineClass(name, bytes, 0, bytes.length);
            }
        }.define();
    }
}
