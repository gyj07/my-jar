package com.github.catvod.spider;

import android.text.TextUtils;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Filter;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mtyy extends Spider {

    private static final String HOST = "https://www.mtyy7.com";
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private Map<String, String> getHeaders() {
        Map<String, String> h = new HashMap<>();
        h.put("User-Agent", UA);
        h.put("Referer", HOST + "/");
        return h;
    }

    private String fetch(String url) {
        try {
            String body = OkHttp.string(url, getHeaders());
            return body == null ? "" : body;
        } catch (Exception e) {
            SpiderDebug.log("fetch error: " + e.getMessage());
            return "";
        }
    }

    private String cleanText(String s) {
        if (s == null) return "";
        s = s.replace("&nbsp;", " ").replace("\n", " ").replace("\r", " ");
        return s.replaceAll("\\s+", " ").trim();
    }

    private String group(String regex, String text, int g) {
        if (text == null) return "";
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(text);
        return m.find() ? (m.group(g) == null ? "" : m.group(g)) : "";
    }

    private String fixUrl(String url) {
        if (TextUtils.isEmpty(url)) return "";
        url = url.trim();
        if (url.startsWith("http")) return url;
        if (url.startsWith("//")) return "https:" + url;
        if (url.startsWith("/")) return HOST + url;
        return HOST + "/" + url;
    }

    private String urlEncode(String s) {
        try { return URLEncoder.encode(s == null ? "" : s, "UTF-8"); }
        catch (Exception e) { return ""; }
    }

    // ============================================================
    // 列表解析
    // ============================================================
    private List<Vod> parseVideoList(String html) {
        List<Vod> list = new ArrayList<>();
        if (TextUtils.isEmpty(html)) return list;

        Matcher m = Pattern.compile(
            "<div class=\"public-list-box[^\"]*\">.*?" +
            "<a[^>]*class=\"public-list-exp\"[^>]*href=\"([^\"]+)\"[^>]*title=\"([^\"]+)\"[^>]*>.*?" +
            "<img[^>]*class=\"lazy[^\"]*\"[^>]*data-src=\"([^\"]+)\"[^>]*>.*?" +
            "<span[^>]*class=\"public-list-prb[^\"]*\"[^>]*>([^<]*)</span>",
            Pattern.DOTALL
        ).matcher(html);

        while (m.find()) {
            String href = fixUrl(m.group(1));
            String name = cleanText(m.group(2));
            String pic = fixUrl(m.group(3));
            String remark = cleanText(m.group(4));
            if (!name.isEmpty()) list.add(new Vod(href, name, pic, remark));
        }
        return list;
    }

    private List<Vod> parseSearchList(String html) {
        List<Vod> list = new ArrayList<>();
        if (TextUtils.isEmpty(html)) return list;

        Matcher m = Pattern.compile(
            "<a[^>]*href=\"(/voddetail/[^\"]+)\"[^>]*title=\"([^\"]+)\"[^>]*>.*?" +
            "<img[^>]*(?:data-src|src)=\"([^\"]+)\"[^>]*>.*?" +
            "<span[^>]*>([^<]*)</span>",
            Pattern.DOTALL
        ).matcher(html);

        while (m.find()) {
            String href = fixUrl(m.group(1));
            String name = cleanText(m.group(2));
            String pic = fixUrl(m.group(3));
            String remark = cleanText(m.group(4));
            if (!name.isEmpty()) list.add(new Vod(href, name, pic, remark));
        }
        return list;
    }

    // ============================================================
    // homeContent
    // ============================================================
    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Class> classes = new ArrayList<>();
        classes.add(new Class("1", "电影"));
        classes.add(new Class("2", "电视剧"));
        classes.add(new Class("3", "综艺"));
        classes.add(new Class("4", "动漫"));

        LinkedHashMap<String, List<Filter>> filters = new LinkedHashMap<>();

        List<Filter> f1 = new ArrayList<>();
        f1.add(filter("class", "类型", new String[][]{
            {"全部", ""}, {"动作", "动作"}, {"喜剧", "喜剧"}, {"爱情", "爱情"},
            {"科幻", "科幻"}, {"恐怖", "恐怖"}, {"剧情", "剧情"}, {"战争", "战争"},
            {"动画", "动画"}, {"悬疑", "悬疑"}, {"犯罪", "犯罪"}, {"奇幻", "奇幻"},
            {"冒险", "冒险"}, {"纪录", "纪录"}
        }));
        f1.add(filter("area", "地区", new String[][]{
            {"全部", ""}, {"中国大陆", "中国大陆"}, {"香港", "香港"}, {"台湾", "台湾"},
            {"美国", "美国"}, {"日本", "日本"}, {"韩国", "韩国"}, {"英国", "英国"},
            {"法国", "法国"}, {"印度", "印度"}, {"泰国", "泰国"}
        }));
        f1.add(filter("year", "年份", new String[][]{
            {"全部", ""}, {"2026", "2026"}, {"2025", "2025"}, {"2024", "2024"},
            {"2023", "2023"}, {"2022", "2022"}, {"2021", "2021"}, {"2020", "2020"}
        }));
        f1.add(filter("by", "排序", new String[][]{
            {"最热", "hits_week"}, {"最新", "time"}, {"高分", "score"}
        }));
        filters.put("1", f1);

        List<Filter> f2 = new ArrayList<>();
        f2.add(filter("class", "类型", new String[][]{
            {"全部", ""}, {"国产剧", "国产剧"}, {"港剧", "港剧"}, {"台剧", "台剧"},
            {"日剧", "日剧"}, {"韩剧", "韩剧"}, {"美剧", "美剧"}, {"英剧", "英剧"},
            {"泰剧", "泰剧"}, {"海外剧", "海外剧"}
        }));
        f2.add(filter("area", "地区", new String[][]{
            {"全部", ""}, {"中国大陆", "中国大陆"}, {"香港", "香港"}, {"台湾", "台湾"},
            {"日本", "日本"}, {"韩国", "韩国"}, {"美国", "美国"}, {"英国", "英国"}
        }));
        f2.add(filter("year", "年份", new String[][]{
            {"全部", ""}, {"2026", "2026"}, {"2025", "2025"}, {"2024", "2024"},
            {"2023", "2023"}, {"2022", "2022"}
        }));
        f2.add(filter("by", "排序", new String[][]{
            {"最热", "hits_week"}, {"最新", "time"}, {"高分", "score"}
        }));
        filters.put("2", f2);

        List<Filter> f3 = new ArrayList<>();
        f3.add(filter("year", "年份", new String[][]{
            {"全部", ""}, {"2026", "2026"}, {"2025", "2025"}, {"2024", "2024"}, {"2023", "2023"}
        }));
        f3.add(filter("by", "排序", new String[][]{
            {"最热", "hits_week"}, {"最新", "time"}, {"高分", "score"}
        }));
        filters.put("3", f3);

        List<Filter> f4 = new ArrayList<>();
        f4.add(filter("area", "地区", new String[][]{
            {"全部", ""}, {"中国大陆", "中国大陆"}, {"日本", "日本"}, {"美国", "美国"}
        }));
        f4.add(filter("year", "年份", new String[][]{
            {"全部", ""}, {"2026", "2026"}, {"2025", "2025"}, {"2024", "2024"}, {"2023", "2023"}
        }));
        f4.add(filter("by", "排序", new String[][]{
            {"最热", "hits_week"}, {"最新", "time"}, {"高分", "score"}
        }));
        filters.put("4", f4);

        String html = fetch(HOST + "/");
        List<Vod> list = parseVideoList(html);

        return Result.string(classes, list);
    }

    private Filter filter(String key, String name, String[][] values) throws Exception {
        List<Filter.Value> list = new ArrayList<>();
        for (String[] kv : values) {
            list.add(new Filter.Value(kv[0], kv[1]));
        }
        return new Filter(key, name, list);
    }

    @Override
    public String homeVideoContent() throws Exception {
        return "{}";
    }

    // ============================================================
    // categoryContent
    // ============================================================
    @Override
    public String categoryContent(String tid, String pg, boolean filter,
                                  HashMap<String, String> extend) throws Exception {
        int page = 1;
        try { page = Integer.parseInt(pg); } catch (Exception ignored) {}

        String classVal = extend != null && extend.get("class") != null ? extend.get("class") : "";
        String areaVal  = extend != null && extend.get("area")  != null ? extend.get("area")  : "";
        String yearVal  = extend != null && extend.get("year")  != null ? extend.get("year")  : "";
        String byVal    = extend != null && extend.get("by")    != null ? extend.get("by")    : "hits_week";
        if (byVal.isEmpty()) byVal = "hits_week";

        if ("中国香港".equals(areaVal)) areaVal = "香港";
        if ("中国台湾".equals(areaVal)) areaVal = "台湾";

        boolean hasFilter = !classVal.isEmpty() || !areaVal.isEmpty() || !yearVal.isEmpty() || !"hits_week".equals(byVal);

        List<Vod> list = new ArrayList<>();
        String html;

        if (hasFilter) {
            String[] parts = {tid, areaVal, byVal, classVal, "", "", yearVal, "", "", "", ""};
            String showUrl = HOST + "/vodshow/" + String.join("-", parts) + ".html";
            html = fetch(showUrl);
            list = parseVideoList(html);
        }

        if (list.isEmpty()) {
            String url;
            if (page == 1) url = HOST + "/vodtype/" + tid + ".html";
            else url = HOST + "/vodtype/" + tid + "-" + page + ".html";
            html = fetch(url);
            list = parseVideoList(html);
        }

        return Result.get().vod(list).page(page, 9999, 90, 999999).string();
    }

    // ============================================================
    // ★ detailContent（方案 A：抓每个 sid 拿 from → 动态线路名）
    // ============================================================
    @Override
    public String detailContent(List<String> ids) throws Exception {
        String vid = ids.get(0);
        String url = vid.startsWith("http") ? vid : (vid.startsWith("/") ? HOST + vid : HOST + "/" + vid);
        String html = fetch(url);
        if (TextUtils.isEmpty(html)) return Result.string(new ArrayList<Vod>());

        Vod vod = new Vod();
        vod.setVodId(vid);

        // 片名
        String title = group("<div class=\"this-desc-title\"[^>]*>([^<]+)</div>", html, 1);
        vod.setVodName(cleanText(title));

        // 海报
        String pic = group("style=\"background-image:\\s*url\\('([^']+)'\\)\"", html, 1);
        if (pic.isEmpty()) pic = group("<img[^>]*class=\"[^\"]*this-pic[^\"]*\"[^>]*src=\"([^\"]+)\"", html, 1);
        if (pic.isEmpty()) pic = group("data-src=\"([^\"]+)\"[^>]*class=\"[^\"]*this-pic[^\"]*\"", html, 1);
        vod.setVodPic(pic);

        // 演员
        String actor = group("<strong class=\"r6\">演员[：:]</strong>(.*?)</div>", html, 1);
        vod.setVodActor(cleanText(actor.replaceAll("<[^>]+>", "")));

        // 导演
        String director = group("<strong class=\"r6\">导演[：:]</strong>(.*?)</div>", html, 1);
        vod.setVodDirector(cleanText(director.replaceAll("<[^>]+>", "")));

        // 类型标签
        String typeBlock = group("<div class=\"this-desc-tags\"[^>]*>(.*?)</div>", html, 1);
        if (!typeBlock.isEmpty()) {
            List<String> tags = new ArrayList<>();
            Matcher tm = Pattern.compile("<span[^>]*>([^<]+)</span>").matcher(typeBlock);
            while (tm.find()) tags.add(tm.group(1).trim());
            vod.setTypeName(String.join(" ", tags));
        }

        // 简介
        String content = group("<div id=\"height_limit\"[^>]*>.*?<strong class=\"r6\">描述[：:]</strong>(.*?)</div>", html, 1);
        if (content.isEmpty()) content = group("<strong class=\"r6\">描述[：:]</strong>(.*?)</div>", html, 1);
        if (content.isEmpty()) content = group("<div class=\"this-desc-text\"[^>]*>(.*?)</div>", html, 1);
        vod.setVodContent(cleanText(content.replaceAll("<[^>]+>", "")));

        // 年份
        String labelsBlock = group("<div class=\"this-desc-labels flex\"[^>]*>(.*?)</div>", html, 1);
        if (!labelsBlock.isEmpty()) {
            String year = group("<i[^>]*>年份</i>([^<]+)</span>", labelsBlock, 1);
            vod.setVodYear(cleanText(year));
        }

        // 地区/状态
        String infoBlock = group("<div class=\"this-desc-info\"[^>]*>(.*?)</div>", html, 1);
        if (!infoBlock.isEmpty()) {
            List<String> spans = new ArrayList<>();
            Matcher sm = Pattern.compile("<span[^>]*>([^<]*)</span>").matcher(infoBlock);
            while (sm.find()) {
                String s = cleanText(sm.group(1));
                if (!s.isEmpty() && !s.matches("^[0-9.]+$")) spans.add(s);
            }
            String status = "", area = "";
            for (String span : spans) {
                if (span.matches(".*[集期].*|.*完结.*|.*更新.*|.*连载.*")) status = span;
                else if (Arrays.asList("中国大陆","香港","台湾","美国","日本","韩国","英国","法国","泰国").contains(span)) area = span;
            }
            if (area.isEmpty() && !spans.isEmpty()) area = spans.get(0);
            if (status.isEmpty() && spans.size() >= 3) status = spans.get(2);
            vod.setVodArea(area);
            vod.setVodRemarks(status);
        }

        // ============================================================
        // ★ 播放列表（方案 A）
        // ============================================================

        // 1. 从详情页抠所有 /vodplay/{vid}-{sid}-{nid}.html，按 sid 分组
        Map<String, List<String>> sidGroups = new LinkedHashMap<>();
        Matcher allM = Pattern.compile(
            "<a[^>]*href=\"(/vodplay/(\\d+)-(\\d+)-(\\d+)\\.html)\"[^>]*>([^<]+)</a>",
            Pattern.DOTALL
        ).matcher(html);
        while (allM.find()) {
            String fullPath = allM.group(1);
            String sid = allM.group(3);
            String epName = cleanText(allM.group(5));
            if (!sidGroups.containsKey(sid)) sidGroups.put(sid, new ArrayList<>());
            sidGroups.get(sid).add(epName + "$" + HOST + fullPath);
        }

        SpiderDebug.log("Mtyy 抓到 " + sidGroups.size() + " 条线路");

        if (sidGroups.isEmpty()) {
            vod.setVodPlayFrom("麦田影院");
            vod.setVodPlayUrl("");
            return Result.string(vod);
        }

        // 2. 排序 sid（数字升序）
        List<String> sidKeys = new ArrayList<>(sidGroups.keySet());
        try {
            sidKeys.sort((a, b) -> Integer.parseInt(a) - Integer.parseInt(b));
        } catch (Exception ignored) {}

        // 3. 抠详情页 ID
        String detailId = vid.replaceAll(".*?(\\d+)\\.html.*", "$1");
        if (detailId.isEmpty()) detailId = vid.replaceAll("\\D+", "");

        // 4. 遍历每个 sid 抓播放页拿 from，同时第一次顺便抠 data-form → 中文名
        Map<String, String> formToName = new HashMap<>();
        List<String> ktabs = new ArrayList<>();
        List<String> klists = new ArrayList<>();

        for (String sid : sidKeys) {
            List<String> eps = sidGroups.get(sid);
            if (eps.isEmpty()) continue;

            String testUrl = HOST + "/vodplay/" + detailId + "-" + sid + "-1.html";
            String testHtml = fetch(testUrl);
            String from = "";

            if (!TextUtils.isEmpty(testHtml)) {
                // 抠 from
                String testJson = group("var\\s+player_data\\s*=\\s*(\\{.*?\\})\\s*<", testHtml, 1);
                if (TextUtils.isEmpty(testJson)) {
                    testJson = group("player_aaaa\\s*=\\s*(\\{.*?\\})\\s*</script>", testHtml, 1);
                }
                if (!TextUtils.isEmpty(testJson)) {
                    try {
                        JSONObject p = new JSONObject(testJson);
                        from = p.optString("from", "");
                    } catch (Exception ignored) {}
                }

                // 第一次抓时顺便抠 data-form → 中文名
                if (formToName.isEmpty()) {
                    Matcher formM = Pattern.compile(
                        "data-form=\"([^\"]+)\"[^>]*>[\\s\\S]*?&nbsp;([^<]+?)(?:<span|</a>)",
                        Pattern.DOTALL
                    ).matcher(testHtml);
                    while (formM.find()) {
                        String form = formM.group(1).trim();
                        String name = cleanText(formM.group(2));
                        if (!form.isEmpty() && !name.isEmpty()) {
                            formToName.put(form, name);
                        }
                    }
                }
            }

            // 用 from 查中文名
            String name = formToName.containsKey(from) ? formToName.get(from) : ("播放源" + sid);
            ktabs.add(name);
            klists.add(String.join("#", eps));

            SpiderDebug.log("Mtyy sid=" + sid + " from=" + from + " name=" + name + " 集数=" + eps.size());
        }

        SpiderDebug.log("Mtyy formToName = " + formToName);
        SpiderDebug.log("Mtyy 线路名 = " + ktabs);

        vod.setVodPlayFrom(ktabs.isEmpty() ? "麦田影院" : String.join("$$$", ktabs));
        vod.setVodPlayUrl(klists.isEmpty() ? "" : String.join("$$$", klists));

        return Result.string(vod);
    }

    // ============================================================
    // searchContent
    // ============================================================
    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        return searchContent(key, quick, "1");
    }

    @Override
    public String searchContent(String key, boolean quick, String pg) throws Exception {
        int page = 1;
        try { page = Integer.parseInt(pg); } catch (Exception ignored) {}

        String encoded = urlEncode(key);
        String url = HOST + "/vodsearch/-------------.html?wd=" + encoded + "&page=" + page;

        String html = fetch(url);
        List<Vod> list = parseVideoList(html);
        if (list.isEmpty()) list = parseSearchList(html);

        return Result.string(list);
    }

    // ============================================================
    // ★ playerContent（player_data + NBY 两步解密）
    // ============================================================
    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        String playUrl = id.startsWith("http") ? id : (id.startsWith("/") ? HOST + id : HOST + "/" + id);

        String html = fetch(playUrl);
        if (TextUtils.isEmpty(html)) {
            return Result.get().url("").header(getHeaders()).string();
        }

        // ★ 抠 player_data 或 player_aaaa
        String json = group("var\\s+player_data\\s*=\\s*(\\{.*?\\})\\s*<", html, 1);
        if (TextUtils.isEmpty(json)) {
            json = group("player_aaaa\\s*=\\s*(\\{.*?\\})\\s*</script>", html, 1);
        }

        String url = "";
        String from = "";

        if (!TextUtils.isEmpty(json)) {
            try {
                JSONObject player = new JSONObject(json);
                url = player.optString("url", "");
                from = player.optString("from", "");
            } catch (Exception e) {
                url = group("\"url\"\\s*:\\s*\"([^\"]+)\"", json, 1);
                from = group("\"from\"\\s*:\\s*\"([^\"]+)\"", json, 1);
            }
        }

        url = url.replace("\\/", "/");
        if (url.startsWith("//")) url = "https:" + url;
        if (url.startsWith("/")) url = HOST + url;

        SpiderDebug.log("Mtyy player.url = " + url);
        SpiderDebug.log("Mtyy player.from = " + from);

        // ① 直链 m3u8/mp4 → parse:0
        if (!TextUtils.isEmpty(url) && url.matches(".*\\.(m3u8|mp4|flv|mkv|webm|ts)(\\?.*)?$")) {
            SpiderDebug.log("✅ 直链");
            return Result.get().url(url).header(getHeaders()).string();
        }

        // ② 第三方 → art.php 两步解密
        if (!TextUtils.isEmpty(url)) {
            // 第一步：get_signed_url
            String api1 = HOST + "/static/player/art.php?get_signed_url=1&url=" + urlEncode(url);
            Map<String, String> h1 = new HashMap<>();
            h1.put("User-Agent", UA);
            h1.put("X-Requested-With", "XMLHttpRequest");
            h1.put("Referer", playUrl);

            String resp1 = "";
            try {
                resp1 = OkHttp.string(api1, h1);
            } catch (Exception e) {
                SpiderDebug.log("get_signed_url error: " + e.getMessage());
            }

            String signedUrl = "";
            try {
                JSONObject j1 = new JSONObject(resp1);
                signedUrl = j1.optString("signed_url", "");
            } catch (Exception ignored) {}

            if (!TextUtils.isEmpty(signedUrl)) {
                // 第二步：请求 signed_url
                String api2 = HOST + "/static/player/art.php" + signedUrl;
                String resp2 = "";
                try {
                    resp2 = OkHttp.string(api2, h1);
                } catch (Exception e) {
                    SpiderDebug.log("signed_url error: " + e.getMessage());
                }

                try {
                    JSONObject j2 = new JSONObject(resp2);
                    String jmurl = j2.optString("jmurl", "");
                    if (TextUtils.isEmpty(jmurl)) jmurl = j2.optString("url", "");
                    if (TextUtils.isEmpty(jmurl)) jmurl = j2.optString("signedUrl", "");
                    if (!TextUtils.isEmpty(jmurl)) {
                        jmurl = jmurl.replace("\\/", "/");
                        SpiderDebug.log("✅✅ 解密成功: " + jmurl);
                        return Result.get().url(jmurl).header(getHeaders()).string();
                    }
                } catch (Exception e) {
                    SpiderDebug.log("resp2 JSON 失败: " + e.getMessage());
                    String m3u8 = group("(https?://[^\\s\"'<>]+?\\.m3u8[^\\s\"'<>]*)", resp2, 1);
                    if (!TextUtils.isEmpty(m3u8)) {
                        return Result.get().url(m3u8).header(getHeaders()).string();
                    }
                }
            }
        }

        // ③ 兜底
        String m3u8 = group("(https?://[^\\s\"'<>]+?\\.m3u8[^\\s\"'<>]*)", html, 1);
        if (!TextUtils.isEmpty(m3u8)) {
            return Result.get().url(m3u8).header(getHeaders()).string();
        }

        SpiderDebug.log("Mtyy：没抠到播放地址");
        return Result.get().url("").header(getHeaders()).string();
    }

    @Override
    public void destroy() {
        SpiderDebug.log("Mtyy destroy");
    }
}