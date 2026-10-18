package com.ank;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class StringEncryptorMain {

    private static ClassLoader auxClassLoader;

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Throwable t) {
            System.err.println("[string-encrypt] FATAL: " + t);
            t.printStackTrace();
        }
        System.exit(0);
    }

    private static void run(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("usage: StringEncryptorMain <in.jar> <out.jar> <key> <classpath> <include1> [!exclude]...");
            System.exit(1);
        }

        Path in = Paths.get(args[0]).toAbsolutePath();
        Path out = Paths.get(args[1]).toAbsolutePath();
        String keyStr = args[2];
        String classpathStr = args[3];

        // строим aux classloader из MC jar'ов — ASM через него разрешит иерархию классов
        List<URL> urls = new ArrayList<>();
        if (classpathStr != null && !classpathStr.isEmpty()) {
            for (String p : classpathStr.split(File.pathSeparator)) {
                if (p == null || p.isBlank()) continue;
                File f = new File(p);
                if (f.exists()) {
                    try {
                        urls.add(f.toURI().toURL());
                    } catch (Exception ignored) {}
                }
            }
        }
        auxClassLoader = new URLClassLoader(
                urls.toArray(new URL[0]),
                StringEncryptorMain.class.getClassLoader());

        System.out.println("[string-encrypt] aux classpath: " + urls.size() + " entries");
        for (int i = 0; i < Math.min(urls.size(), 25); i++) {
            System.out.println("[string-encrypt]   cp[" + i + "]: " + urls.get(i));
        }

        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
        for (int i = 4; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("!")) excludes.add(a.substring(1).replace('.', '/'));
            else includes.add(a.replace('.', '/'));
        }

        byte[] keyBytes = new byte[16];
        byte[] raw = keyStr.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < 16; i++) keyBytes[i] = raw[i % raw.length];

        if (Files.exists(out)) Files.delete(out);
        Path outParent = out.getParent();
        if (outParent != null) Files.createDirectories(outParent);

        int processed = 0;
        int skipped = 0;

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(in)));
             ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(out)))) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() || !name.endsWith(".class")) {
                    copyEntry(zis, zos, entry);
                    continue;
                }

                byte[] classBytes;
                try {
                    classBytes = zis.readAllBytes();
                } catch (Throwable t) {
                    System.err.println("[string-encrypt] read failed " + name + " : " + t);
                    continue;
                }

                String internalName = name.substring(0, name.length() - ".class".length());

                if (shouldProcess(internalName, includes, excludes)) {
                    byte[] original = classBytes;
                    try {
                        byte[] transformed = encryptClass(classBytes, keyBytes);
                        if (transformed != null && transformed.length > 0) {
                            classBytes = transformed;
                            processed++;
                        } else {
                            classBytes = original;
                            skipped++;
                        }
                    } catch (Throwable t) {
                        classBytes = original;
                        skipped++;
                        System.err.println("[string-encrypt] skipped " + name + " : " + t);
                    }
                }

                ZipEntry newEntry = new ZipEntry(name);
                zos.putNextEntry(newEntry);
                zos.write(classBytes);
                zos.closeEntry();
            }
        }
        System.out.println("[string-encrypt] processed " + processed + " classes, skipped " + skipped + ", wrote " + out);
    }

    private static boolean shouldProcess(String internalName, List<String> inc, List<String> exc) {
        for (String e : exc) if (internalName.startsWith(e)) return false;
        for (String i : inc) if (internalName.startsWith(i)) return true;
        return false;
    }

    private static byte[] encryptClass(byte[] classBytes, byte[] key) {
        ClassReader reader = new ClassReader(classBytes);

        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected ClassLoader getClassLoader() {
                return auxClassLoader;
            }
        };

        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof String s && !s.isEmpty()) {
                            try {
                                String encrypted = encryptString(s, key);
                                super.visitLdcInsn(encrypted);
                                super.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        "com/example/bfb/AnkStrings",
                                        "decrypt",
                                        "(Ljava/lang/String;)Ljava/lang/String;",
                                        false);
                            } catch (Exception e) {
                                super.visitLdcInsn(value);
                            }
                        } else {
                            super.visitLdcInsn(value);
                        }
                    }
                };
            }
        };

        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static String encryptString(String plain, byte[] key) throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ct, 0, combined, iv.length, ct.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    private static void copyEntry(ZipInputStream zis, ZipOutputStream zos, ZipEntry entry) throws IOException {
        ZipEntry copy = new ZipEntry(entry.getName());
        zos.putNextEntry(copy);
        byte[] buf = new byte[8192];
        int n;
        while ((n = zis.read(buf)) > 0) zos.write(buf, 0, n);
        zos.closeEntry();
    }
}