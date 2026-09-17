import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

public class CheckJar {

    private static final String[] FORBIDDEN_PACKAGES = {
        "androidx", "kotlin", "javax/xml/namespace", "org/slf4j", "org/xmlpull/v1"
    };

    private static final String[] ALLOWED_PREFIXES = {
        "android/",
        "androidx/annotation/",
        "androidx/startup/",
        "androidx/tracing/",
        "com/github/catvod/crawler/",
        "com/google/gson/",
        "com/hierynomus/",
        "com/thegrizzlylabs/sardineandroid/",
        "com/whl/quickjs/",
        "dalvik/",
        "j$/",
        "java/",
        "javax/crypto/",
        "javax/net/",
        "javax/security/",
        "javax/xml/namespace/",
        "okhttp3/",
        "okio/",
        "org/json/",
        "org/slf4j/",
        "org/w3c/dom/",
        "org/xml/sax/",
        "org/xmlpull/v1/",
        "kotlin/"
    };

    private static final String[] OPTIONAL_PREFIXES = { "com/google/re2j/" };

    private static final Map<String, String> FORBIDDEN_TEXT = new LinkedHashMap<>();
    static {
        FORBIDDEN_TEXT.put(
            "Lcom/google/gson/reflect/TypeToken;-><init>()V",
            "illegal Gson TypeToken constructor call; use TypeToken.getParameterized"
        );
    }

    private static final Pattern CLASS_PATTERN =
        Pattern.compile("(?m)^\\.class[ \\t]+(?:[^ \\t\\r\\n]+[ \\t]+)*L([^;\\r\\n]+);");
    private static final Pattern TYPE_PATTERN =
        Pattern.compile("(?<![A-Za-z0-9_$])L([A-Za-z_$][A-Za-z0-9_$]*(?:/[A-Za-z_$][A-Za-z0-9_$]*)+(?:\\$[A-Za-z0-9_$]+)?);");

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: CheckJar <jar> <apktool>");
            System.exit(2);
        }
        Path jar = Paths.get(args[0]).toAbsolutePath();
        Path apktool = Paths.get(args[1]).toAbsolutePath();

        if (!Files.exists(jar)) fail("missing jar: " + jar);
        if (!Files.exists(apktool)) fail("missing apktool: " + apktool);

        long size = Files.size(jar);

        // 1. MD5 校验
        Path md5Path = Paths.get(jar + ".md5");
        if (!Files.exists(md5Path)) fail("missing md5: " + md5Path);
        String actualMd5 = md5(jar);
        String expectedMd5 = Files.readString(md5Path).trim().toLowerCase(Locale.ROOT);
        if (!actualMd5.equals(expectedMd5))
            fail("md5 mismatch: " + actualMd5 + " != " + expectedMd5);

        // 2. apktool 反编译到临时目录
        Path work = Files.createTempDirectory("catvod-checkJar-");
        try {
            // ★ 改用 proc，避免和 lambda 参数名冲突
            Process proc = new ProcessBuilder(
                "java", "-jar", apktool.toString(), "d", "-f",
                jar.toString(), "-o", work.toString()
            ).inheritIO().start();
            if (proc.waitFor() != 0) fail("apktool decode failed");

            Path smali = work.resolve("smali");
            if (!Files.exists(smali)) fail("missing smali output");

            // 3. 检查不该出现的包
            for (String pkg : FORBIDDEN_PACKAGES) {
                if (Files.exists(smali.resolve(pkg)))
                    fail("unexpected packaged API: " + pkg);
            }

            // 4. 检查 catvod 下只允许 js / spider
            Path catvod = smali.resolve("com/github/catvod");
            if (!Files.exists(catvod)) fail("missing catvod package");
            try (Stream<Path> s = Files.list(catvod)) {
                // ★ lambda 参数改成 path
                List<String> unexpected = s.map(path -> path.getFileName().toString())
                    .filter(n -> !n.equals("js") && !n.equals("spider"))
                    .sorted().collect(Collectors.toList());
                if (!unexpected.isEmpty())
                    fail("unexpected catvod entries: " + String.join(", ", unexpected));
            }

            // 5. 扫描所有 smali
            Set<String> defs = new HashSet<>();
            Set<String> refs = new HashSet<>();
            List<Path> smaliFiles;
            try (Stream<Path> s = Files.walk(smali)) {
                // ★ lambda 参数改成 path
                smaliFiles = s.filter(path -> path.toString().endsWith(".smali"))
                    .collect(Collectors.toList());
            }
            for (Path file : smaliFiles) {
                String text = Files.readString(file);
                for (Map.Entry<String, String> e : FORBIDDEN_TEXT.entrySet()) {
                    if (text.contains(e.getKey()))
                        fail(e.getValue() + ": " + file);
                }
                Matcher cm = CLASS_PATTERN.matcher(text);
                while (cm.find()) defs.add(cm.group(1));
                Matcher tm = TYPE_PATTERN.matcher(text);
                while (tm.find()) refs.add(tm.group(1));
            }

            // 6. 引用校验
            List<String> missing = new ArrayList<>();
            List<String> optional = new ArrayList<>();
            for (String ref : refs) {
                if (defs.contains(ref)) continue;
                if (startsWithAny(ref, ALLOWED_PREFIXES)) continue;
                if (startsWithAny(ref, OPTIONAL_PREFIXES)) { optional.add(ref); continue; }
                missing.add(ref);
            }
            if (!missing.isEmpty()) {
                missing = missing.stream().distinct().sorted().limit(20).collect(Collectors.toList());
                fail("missing refs: " + String.join(", ", missing));
            }
            if (!optional.isEmpty()) {
                optional = optional.stream().distinct().sorted().collect(Collectors.toList());
                System.out.println("WARN optional refs (jsoup optional regex backend): "
                    + String.join(", ", optional));
            }

            System.out.println("======================================================");
            System.out.println("  JAR   : " + jar.getFileName());
            System.out.println("  SIZE  : " + size + " bytes (" + humanSize(size) + ")");
            System.out.println("  MD5   : " + actualMd5);
            System.out.println("  SMALI : " + smaliFiles.size() + " files, " + defs.size() + " classes defined, " + refs.size() + " types referenced");
            System.out.println("======================================================");
            System.out.println("OK " + jar.getFileName());

        } finally {
            deleteRecursively(work);
        }
    }

    private static boolean startsWithAny(String value, String[] prefixes) {
        for (String pfx : prefixes) if (value.startsWith(pfx)) return true;
        return false;
    }

    private static String md5(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int unit = 1024;
        String[] units = {"KB", "MB", "GB"};
        double v = bytes;
        for (int i = 0; i < units.length; i++) {
            v /= unit;
            if (v < unit) return String.format("%.2f %s", v, units[i]);
        }
        return String.format("%.2f TB", v / unit);
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) {}
            });
        }
    }

    private static void fail(String msg) {
        System.out.println("FAIL " + msg);
        System.exit(1);
    }
}