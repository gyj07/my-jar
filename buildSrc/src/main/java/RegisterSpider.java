import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

public class RegisterSpider {

    private static final Pattern CLASS_PATTERN =
        Pattern.compile("public\\s+class\\s+(\\w+)\\s+extends\\s+Spider\\b");

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: RegisterSpider <spiderDir> <configJson>");
            System.exit(2);
        }
        Path spiderDir = Paths.get(args[0]).toAbsolutePath();
        Path configJson = Paths.get(args[1]).toAbsolutePath();

        if (!Files.isDirectory(spiderDir)) {
            System.out.println("SKIP spider dir not found: " + spiderDir);
            return;
        }
        if (!Files.exists(configJson)) {
            System.out.println("SKIP config not found: " + configJson);
            return;
        }

        // 1. 扫描继承 Spider 的类
        Set<String> spiderClasses = new TreeSet<>();
        try (Stream<Path> s = Files.walk(spiderDir)) {
            for (Path p : s.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList())) {
                String text = Files.readString(p, StandardCharsets.UTF_8);
                Matcher m = CLASS_PATTERN.matcher(text);
                while (m.find()) spiderClasses.add(m.group(1));
            }
        }
        if (spiderClasses.isEmpty()) {
            System.out.println("SKIP no Spider subclasses found");
            return;
        }

        // 2. 读取 config.json，提取已有的 csp_XXX
        String json = Files.readString(configJson, StandardCharsets.UTF_8);
        Set<String> existingApis = new TreeSet<>();
        Matcher apiMatcher = Pattern.compile("\"api\"\\s*:\\s*\"csp_(\\w+)\"").matcher(json);
        while (apiMatcher.find()) existingApis.add(apiMatcher.group(1));

        // 3. 找出缺失的
        List<String> missing = spiderClasses.stream()
            .filter(c -> !existingApis.contains(c))
            .collect(Collectors.toList());

        if (missing.isEmpty()) {
            System.out.println("OK config.json already contains all " + spiderClasses.size() + " spiders");
            return;
        }

        System.out.println("Found " + missing.size() + " new spider(s): " + String.join(", ", missing));

        // 4. 往 sites 数组里插入
        StringBuilder sb = new StringBuilder();
        for (String cls : missing) {
            String key = cls.toLowerCase(Locale.ROOT);
            sb.append("    {\n");
            sb.append("      \"key\": \"").append(key).append("\",\n");
            sb.append("      \"name\": \"").append(cls).append("\",\n");
            sb.append("      \"type\": 3,\n");
            sb.append("      \"api\": \"csp_").append(cls).append("\",\n");
            sb.append("      \"searchable\": 1,\n");
            sb.append("      \"quickSearch\": 1,\n");
            sb.append("      \"filterable\": 1\n");
            sb.append("    },\n");
        }

        // 在 "sites": [ 后面插入
        int idx = json.indexOf("\"sites\"");
        if (idx < 0) {
            System.out.println("SKIP no \"sites\" field in config.json");
            return;
        }
        int bracket = json.indexOf('[', idx);
        if (bracket < 0) {
            System.out.println("SKIP malformed \"sites\" field");
            return;
        }
        String result = json.substring(0, bracket + 1)
            + "\n" + sb.toString().stripTrailing()
            + json.substring(bracket + 1);

        Files.writeString(configJson, result, StandardCharsets.UTF_8);
        System.out.println("OK registered " + missing.size() + " spider(s) into config.json");
    }
}
