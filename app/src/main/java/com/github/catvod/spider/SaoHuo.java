package com.github.catvod.spider;

import android.content.Context;
import android.text.TextUtils;

import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SaoHuo extends Spider {

    private String host = "https://shdy2.com";
    private String cookie = "";

    private static final String UA = "Mozilla/5.0 (Linux; Android 9; ALN-AL00 Build/PQ3B.190801.05281406; wv) AppleWebKit/537.36";

    private Map<String, String> headers;

    // ============================================================
    // init（★ 不发网络请求）
    // ============================================================
    @Override
    public void init(Context context, String extend) {
        if (!TextUtils.isEmpty(extend)) {
            host = extend.trim();
            if (host.endsWith("/")) host = host.substring(0, host.length() - 1);
        }
        headers = new HashMap<>();
        headers.put("User-Agent", UA);
        headers.put("accept-language", "zh-CN,zh;q=0.9");
        // ★ 删掉了 request(host)
    }

    private Map<String, String> getHeaders() {
        if (headers == null) init(null, null);
        return headers;
    }

    // ============================================================
    // request / postJson
    // ============================================================
    private String request(String url) {
        return request(url, null, null);
    }

    private String request(String url, Map<String, String> extraHeaders, String referer) {
        try {
            Map<String, String> h = new HashMap<>(getHeaders());
            if (extraHeaders != null) h.putAll(extraHeaders);
            if (referer != null) h.put("Referer", referer);
            if (!cookie.isEmpty()) h.put("Cookie", cookie);

            String body = OkHttp.string(url, h);
            return body == null ? "" : body;
        } catch (Exception e) {
            SpiderDebug.log("request error: " + e.getMessage());
            return "";
        }
    }

    private String postJson(String url, JSONObject obj, String referer) {
        try {
            Map<String, String> h = new HashMap<>(getHeaders());
            h.put("Content-Type", "application/json");
            if (referer != null) h.put("Referer", referer);

            String body = OkHttp.post(url, obj.toString(), h).getBody();
            return body == null ? "" : body;
        } catch (Exception e) {
            SpiderDebug.log("postJson error: " + e.getMessage());
            return "";
        }
    }

    // ============================================================
    // homeContent
    // ============================================================
    @Override
    public String homeContent(boolean filter) {
        try {
            JSONObject result = new JSONObject();
            JSONArray classes = new JSONArray();

            String[][] cls = {
                {"1", "电影"}, {"2", "电视剧"}, {"4", "动漫"}
            };
            for (String[] c : cls) {
                JSONObject o = new JSONObject();
                o.put("type_id", c[0]);
                o.put("type_name", c[1]);
                classes.put(o);
            }
            result.put("class", classes);

            JSONObject filters = new JSONObject();
            filters.put("1", buildFilterArr(buildFilterGroup("cateId", "类型", new String[][]{
                {"全部", "1"}, {"喜剧", "6"}, {"爱情", "7"}, {"恐怖", "8"},
                {"动作", "9"}, {"科幻", "10"}, {"战争", "11"}, {"犯罪", "12"},
                {"动画", "13"}, {"奇幻", "14"}, {"剧情", "15"}, {"冒险", "16"},
                {"悬疑", "17"}, {"惊悚", "18"}, {"其他", "20"}
            })));
            filters.put("2", buildFilterArr(buildFilterGroup("cateId", "类型", new String[][]{
                {"全部", "2"}, {"国产剧", "20"}, {"TVB", "21"}, {"韩剧", "22"},
                {"美剧", "23"}, {"日剧", "24"}, {"英剧", "25"}, {"台剧", "26"}, {"其他", "27"}
            })));
            filters.put("4", buildFilterArr(buildFilterGroup("cateId", "类型", new String[][]{
                {"全部", "4"}, {"搞笑", "38"}, {"恋爱", "39"}, {"热血", "40"},
                {"格斗", "41"}, {"美少女", "42"}, {"魔法", "43"}, {"机战", "44"},
                {"校园", "45"}, {"亲子", "46"}, {"童话", "47"}, {"冒险", "48"},
                {"真人", "49"}, {"LOLI", "50"}, {"其他", "51"}
            })));
            result.put("filters", filters);

            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log("homeContent error: " + e.getMessage());
            return "";
        }
    }

    private JSONObject buildFilterGroup(String key, String name, String[][] values) throws Exception {
        JSONObject g = new JSONObject();
        g.put("key", key);
        g.put("name", name);
        JSONArray arr = new JSONArray();
        for (String[] v : values) {
            JSONObject o = new JSONObject();
            o.put("n", v[0]);
            o.put("v", v[1]);
            arr.put(o);
        }
        g.put("value", arr);
        return g;
    }

    private JSONArray buildFilterArr(JSONObject... items) {
        JSONArray arr = new JSONArray();
        for (JSONObject o : items) arr.put(o);
        return arr;
    }

    // ============================================================
    // homeVideoContent
    // ============================================================
    @Override
    public String homeVideoContent() {
        try {
            String html = request(host);
            if (html.isEmpty()) {
                JSONObject r = new JSONObject();
                r.put("list", new JSONArray());
                return r.toString();
            }
            List<JSONObject> list = extractList(html, 6);

            JSONArray arr = new JSONArray();
            for (JSONObject o : list) arr.put(o);
            JSONObject result = new JSONObject();
            result.put("list", arr);
            return result.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ============================================================
    // categoryContent
    // ============================================================
    @Override
    public String categoryContent(String tid, String pg, boolean filter,
                                  HashMap<String, String> extend) {
        try {
            int page = 1;
            try { page = Integer.parseInt(pg); } catch (Exception ignored) {}

            String cateId = (extend != null && extend.get("cateId") != null)
                    ? extend.get("cateId") : tid;

            String url = host + "/list/" + cateId;
            if (page > 1) url += "-" + page;
            url += ".html";

            String html = request(url);
            if (html.isEmpty()) {
                JSONObject r = new JSONObject();
                r.put("list", new JSONArray());
                r.put("page", page);
                r.put("pagecount", 1);
                return r.toString();
            }

            List<JSONObject> list = extractList(html, 0);
            int pagecount = extractPageCount(html, page, list.size());

            JSONArray arr = new JSONArray();
            for (JSONObject o : list) arr.put(o);

            JSONObject result = new JSONObject();
            result.put("list", arr);
            result.put("page", page);
            result.put("pagecount", pagecount);
            result.put("limit", list.size());
            result.put("total", pagecount * list.size());
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log("categoryContent error: " + e.getMessage());
            return "";
        }
    }

    // ============================================================
    // detailContent
    // ============================================================
    @Override
    public String detailContent(List<String> ids) {
        try {
            String id = ids.get(0);
            String url = id.startsWith("http") ? id : (host + id);
            String html = request(url);
            if (html.isEmpty()) {
                JSONObject r = new JSONObject();
                r.put("list", new JSONArray());
                return r.toString();
            }

            JSONObject info = new JSONObject();
            info.put("vod_id", id);

            // 片名
            String name = group("<h1[^>]*class=\"v_title\"[^>]*>[\\s\\S]*?<a[^>]*>([^<]+)</a>", html, 1);
            if (name.isEmpty()) {
                name = group("<h1[^>]*class=\"title\"[^>]*>([^<]+)</h1>", html, 1);
            }
            info.put("vod_name", name.trim());

            // 封面
            String pic = group("class=\"m_background\"[^>]*style=\"background-image:url\\(([^)]+)\\)", html, 1);
            pic = pic.replace("\"", "").replace("'", "").trim();
            if (pic.isEmpty()) {
                pic = group("<img[^>]*class=\"lazyload\"[^>]*data-original=\"([^\"]+)\"", html, 1);
                if (pic.isEmpty()) {
                    pic = group("<img[^>]*class=\"lazyload\"[^>]*src=\"([^\"]+)\"", html, 1);
                }
            }
            if (!pic.isEmpty() && !pic.startsWith("http")) pic = host + pic;
            info.put("vod_pic", pic);

            // 参数行
            String infoLine = group(
                "<h1[^>]*class=\"v_title\"[^>]*>[\\s\\S]*?</h1>\\s*<p>([\\s\\S]*?)<a",
                html, 1
            );
            if (infoLine.isEmpty()) {
                infoLine = group(
                    "<h1[^>]*class=\"v_title\"[^>]*>[\\s\\S]*?</h1>\\s*<p>([\\s\\S]*?)</p>",
                    html, 1
                );
            }
            infoLine = infoLine.replaceAll("<[^>]+>", "").trim();

            String vodArea = "", vodYear = "", typeName = "", vodDirector = "", vodActor = "";
            String[] segs = infoLine.split("/");
            if (segs.length > 0) vodArea = segs[0].trim();
            if (segs.length > 1) vodYear = segs[1].trim();
            if (segs.length > 2) typeName = segs[2].trim();
            for (int i = 3; i < segs.length; i++) {
                String seg = segs[i].trim();
                if (seg.startsWith("导演:")) {
                    vodDirector = seg.substring(3).trim();
                } else if (seg.startsWith("主演:")) {
                    vodActor = seg.substring(3).trim();
                    vodActor = vodActor.replaceAll("剧情介绍.*$", "").trim();
                }
            }
            info.put("vod_director", vodDirector);
            info.put("vod_actor", vodActor);
            info.put("vod_area", vodArea);
            info.put("vod_year", vodYear);
            info.put("vod_class", typeName);

            // 简介
            String content = group("<p[^>]*class=\"p_txt[^\"]*\"[^>]*>([\\s\\S]*?)</p>", html, 1);
            if (content.isEmpty()) {
                content = group("<[^>]*class=\"intro\"[^>]*>([\\s\\S]*?)</", html, 1);
            }
            if (content.isEmpty()) {
                content = group("<[^>]*class=\"des\"[^>]*>([\\s\\S]*?)</", html, 1);
            }
            content = content.replaceAll("<[^>]+>", "").trim();
            content = content.replaceAll("^(剧情)?简介[:：]\\s*", "").trim();
            info.put("vod_content", content);

            // 播放列表
            String[] pl = extractPlaylist(html);
            info.put("vod_play_from", pl[0]);
            info.put("vod_play_url", pl[1]);

            SpiderDebug.log("detail: name=" + name + " from=" + pl[0]);

            JSONArray list = new JSONArray();
            list.put(info);
            JSONObject result = new JSONObject();
            result.put("list", list);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log("detailContent error: " + e.getMessage());
            return "";
        }
    }

    // ============================================================
    // 播放列表
    // ============================================================
    private String[] extractPlaylist(String html) {
        List<String> sourceNames = new ArrayList<>();
        List<String> sourceUrls = new ArrayList<>();

        // 线路名
        Matcher fromM = Pattern.compile(
            "<[^>]*class=\"[^\"]*from_list[^\"]*\"[^>]*>([\\s\\S]*?)</ul>", Pattern.DOTALL
        ).matcher(html);
        List<String> tmpNames = new ArrayList<>();
        while (fromM.find()) {
            String block = fromM.group(1);
            Matcher liM = Pattern.compile("<li[^>]*>([\\s\\S]*?)</li>").matcher(block);
            while (liM.find()) {
                String li = liM.group(1);
                String name = li.replaceAll("<[^>]+>", "").trim();
                if (!name.isEmpty()) tmpNames.add(name);
            }
        }

        // #play_link 剧集
        Matcher linkM = Pattern.compile(
            "<ul[^>]*id=\"play_link\"[^>]*>([\\s\\S]*?)</ul>", Pattern.DOTALL
        ).matcher(html);
        List<String> linkBlocks = new ArrayList<>();
        while (linkM.find()) {
            linkBlocks.add(linkM.group(1));
        }

        if (!linkBlocks.isEmpty()) {
            String inner = linkBlocks.get(0);
            Matcher liM = Pattern.compile("<li[^>]*>([\\s\\S]*?)</li>", Pattern.DOTALL).matcher(inner);
            int idx = 0;
            while (liM.find()) {
                String block = liM.group(1);
                List<String[]> eps = extractEpisodes(block);
                if (!eps.isEmpty()) {
                    String name = idx < tmpNames.size() ? tmpNames.get(idx) : ("线路" + (idx + 1));
                    sourceNames.add(name);
                    sourceUrls.add(joinEpisodes(eps));
                }
                idx++;
            }
        }

        if (sourceNames.isEmpty()) {
            Matcher liM = Pattern.compile("<li[^>]*>([\\s\\S]*?)</li>", Pattern.DOTALL).matcher(html);
            int idx = 0;
            while (liM.find()) {
                String block = liM.group(1);
                if (!block.contains("play_link") && !block.contains("<a")) continue;
                List<String[]> eps = extractEpisodes(block);
                if (!eps.isEmpty()) {
                    sourceNames.add("线路" + (idx + 1));
                    sourceUrls.add(joinEpisodes(eps));
                    idx++;
                }
            }
        }

        return new String[]{String.join("$$$", sourceNames), String.join("$$$", sourceUrls)};
    }

    private List<String[]> extractEpisodes(String block) {
        List<String[]> eps = new ArrayList<>();
        Matcher aM = Pattern.compile("<a[^>]*href=\"([^\"]+)\"[^>]*>([^<]+)</a>").matcher(block);
        while (aM.find()) {
            String href = aM.group(1);
            String text = aM.group(2).trim();
            String numStr = text.replaceAll("[^0-9]", "");
            int num = 9999;
            if (!numStr.isEmpty()) {
                try { num = Integer.parseInt(numStr); } catch (Exception ignored) {}
            }
            eps.add(new String[]{text, href, String.valueOf(num)});
        }
        eps.sort((a, b) -> {
            try { return Integer.parseInt(a[2]) - Integer.parseInt(b[2]); }
            catch (Exception e) { return 0; }
        });
        return eps;
    }

    private String joinEpisodes(List<String[]> eps) {
        List<String> parts = new ArrayList<>();
        for (String[] ep : eps) {
            parts.add(ep[0] + "$" + ep[1]);
        }
        return String.join("#", parts);
    }

    // ============================================================
    // searchContent
    // ============================================================
    @Override
    public String searchContent(String wd, boolean quick) {
        try {
            String url = host + "/s----------.html?wd=" + urlEncode(wd);
            String html = request(url);
            if (html.isEmpty()) {
                JSONObject r = new JSONObject();
                r.put("list", new JSONArray());
                return r.toString();
            }
            List<JSONObject> list = extractList(html, 0);

            JSONArray arr = new JSONArray();
            for (JSONObject o : list) arr.put(o);
            JSONObject result = new JSONObject();
            result.put("list", arr);
            return result.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ============================================================
    // playerContent
    // ============================================================
    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        try {
            String playPageUrl = id.startsWith("http") ? id : (host + id);

            // 1. 播放页
            String html = request(playPageUrl);
            if (!html.isEmpty()) {
                String direct = extractM3u8(html);
                if (!direct.isEmpty()) {
                    return buildResult(0, direct, null, 0);
                }
            }

            String hhUrl = extractHhUrl(html);
            if (hhUrl.isEmpty()) {
                return buildResult(0, playPageUrl, null, 0);
            }

            // 2. hhplayer
            Map<String, String> hhHeaders = new HashMap<>();
            hhHeaders.put("Referer", playPageUrl);
            String hhHtml = request(hhUrl, hhHeaders, null);

            if (!hhHtml.isEmpty()) {
                String direct = extractM3u8(hhHtml);
                if (!direct.isEmpty()) {
                    return buildResult(0, direct, null, 0);
                }
            }

            JSONObject boot = extractBootstrap(hhHtml);
            if (boot == null || !boot.has("url") || !boot.has("key")) {
                return buildResult(0, hhUrl, null, 0);
            }

            // 3. POST /api/parse
            String hhDomain = "";
            Matcher dm = Pattern.compile("^https?://([^/]+)").matcher(hhUrl);
            if (dm.find()) hhDomain = dm.group(1);
            String apiUrl = "https://" + hhDomain + "/api/parse";

            JSONObject reqBody = new JSONObject();
            reqBody.put("url", boot.optString("url"));
            reqBody.put("t", boot.opt("t"));
            reqBody.put("key", boot.optString("key"));
            reqBody.put("client_fallback", false);

            String respText = postJson(apiUrl, reqBody, hhUrl);
            if (respText.isEmpty()) {
                return buildResult(0, hhUrl, null, 0);
            }

            JSONObject resp = null;
            try {
                resp = new JSONObject(respText);
            } catch (Exception e) {
                String fb = extractM3u8(respText);
                if (!fb.isEmpty()) {
                    return buildResult(0, fb, null, 0);
                }
                return buildResult(0, hhUrl, null, 0);
            }

            if (resp == null || resp.optInt("code", 0) != 200 || !resp.has("url")) {
                return buildResult(0, hhUrl, null, 0);
            }

            String m3u8 = resp.optString("url").replace("\\u0026", "&").replace("\\/", "/");

            Map<String, String> playHeaders = new HashMap<>();
            playHeaders.put("Referer", hhUrl);
            playHeaders.put("User-Agent", UA);

            return buildResult(0, m3u8, playHeaders, 0);
        } catch (Exception e) {
            SpiderDebug.log("playerContent error: " + e.getMessage());
            return "";
        }
    }

    private String buildResult(int parse, String url, Map<String, String> h, int jx) {
        try {
            JSONObject r = new JSONObject();
            r.put("parse", parse);
            r.put("url", url);
            r.put("jx", jx);
            if (h != null) {
                JSONObject ho = new JSONObject();
                for (Map.Entry<String, String> e : h.entrySet()) ho.put(e.getKey(), e.getValue());
                r.put("headers", ho);
            }
            return r.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ============================================================
    // 辅助：extractHhUrl / extractBootstrap / extractM3u8
    // ============================================================
    private String extractHhUrl(String html) {
        if (html == null || html.isEmpty()) return "";
        Matcher m = Pattern.compile(
            "<iframe[^>]+src=[\"'](https?://[^\"']+[?&]url=[A-Za-z0-9]+)[\"']",
            Pattern.CASE_INSENSITIVE
        ).matcher(html);
        if (m.find()) return m.group(1).replace("&amp;", "&");

        m = Pattern.compile("(https?://[^\"'\\s<>]+[?&]url=[A-Za-z0-9]+)").matcher(html);
        if (m.find()) return m.group(1).replace("&amp;", "&");
        return "";
    }

    private JSONObject extractBootstrap(String html) {
        if (html == null || html.isEmpty()) return null;
        Matcher m = Pattern.compile("__HHJX_BOOTSTRAP__\\s*=\\s*(\\{[^}]+\\})").matcher(html);
        if (!m.find()) return null;
        try {
            return new JSONObject(m.group(1));
        } catch (Exception e) {
            return null;
        }
    }

    private String extractM3u8(String text) {
        if (text == null || text.isEmpty()) return "";
        String[] patterns = {
            "\"url\"\\s*:\\s*\"(https?:[^\"]+?\\.m3u8[^\"]*)\"",
            "\"m3u8_url\"\\s*:\\s*\"(https?:[^\"]+?\\.m3u8[^\"]*)\"",
            "(https?:[^\"'\\s\\\\<>]+?\\.m3u8[^\"'\\s\\\\<>]*)"
        };
        for (String p : patterns) {
            Matcher m = Pattern.compile(p, Pattern.CASE_INSENSITIVE).matcher(text);
            if (m.find()) {
                return m.group(1).replace("\\u0026", "&").replace("\\/", "/").replace("&amp;", "&");
            }
        }
        return "";
    }

    // ============================================================
    // 列表解析
    // ============================================================
    private List<JSONObject> extractList(String html, int limit) {
        List<JSONObject> list = new ArrayList<>();
        if (html == null || html.isEmpty()) return list;
        try {
            Pattern cardPattern = Pattern.compile(
                "<a[^>]*href=\"([^\"]+)\"[^>]*title=\"([^\"]*)\"[^>]*>[\\s\\S]*?" +
                "<img[^>]*(?:data-original|src)=\"([^\"]+)\"[^>]*>[\\s\\S]*?" +
                "</a>"
            );
            Matcher m = cardPattern.matcher(html);
            List<String> seen = new ArrayList<>();
            int count = 0;
            while (m.find()) {
                if (limit > 0 && count >= limit) break;
                String href = m.group(1);
                String title = m.group(2) == null ? "" : m.group(2).trim();
                String pic = m.group(3) == null ? "" : m.group(3);

                if (title.isEmpty() || href.isEmpty()) continue;
                if (seen.contains(href)) continue;
                seen.add(href);

                String vodId = href.startsWith("http") ? href : (host + href);
                String vodPic = pic.startsWith("http") ? pic : (host + pic);

                JSONObject o = new JSONObject();
                o.put("vod_id", vodId);
                o.put("vod_name", title);
                o.put("vod_pic", vodPic);
                o.put("vod_remarks", "");
                list.add(o);
                count++;
            }
        } catch (Exception e) {
            SpiderDebug.log("extractList error: " + e.getMessage());
        }
        return list;
    }

    private int extractPageCount(String html, int page, int listSize) {
        int pagecount = page;
        try {
            Matcher m = Pattern.compile("<a[^>]*href=\"[^\"]*[-_](\\d+)\\.html\"[^>]*>").matcher(html);
            int maxPage = 0;
            while (m.find()) {
                try {
                    int p = Integer.parseInt(m.group(1));
                    if (p > maxPage) maxPage = p;
                } catch (Exception ignored) {}
            }
            if (maxPage > 0) pagecount = maxPage;
            else if (listSize >= 10) pagecount = page + 1;
        } catch (Exception ignored) {}
        return pagecount;
    }

    // ============================================================
    // 工具
    // ============================================================
    private String group(String regex, String text, int g) {
        if (text == null) return "";
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(text);
        return m.find() ? (m.group(g) == null ? "" : m.group(g)) : "";
    }

    private String urlEncode(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void destroy() {
        SpiderDebug.log("SaoHuo destroy");
    }
}