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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DuShe extends Spider {

    private static final String DEFAULT_HOST = "https://www.dushehub.com";
    private String host = DEFAULT_HOST;

    private static final String JX_HOST = "https://v.dushe.online";

    private static final String UA = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36";

    private Map<String, String> headers;
    private Map<String, String> m3u8Headers;

    // ============================================================
    // init
    // ============================================================
    @Override
    public void init(Context context, String extend) {
        if (!TextUtils.isEmpty(extend)) {
            extend = extend.trim();
            if (extend.startsWith("http")) {
                host = extend;
                if (host.endsWith("/")) host = host.substring(0, host.length() - 1);
            }
        }
        headers = new HashMap<>();
        headers.put("User-Agent", UA);
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9");
        headers.put("Referer", host + "/");

        m3u8Headers = new HashMap<>();
        m3u8Headers.put("User-Agent", UA);
        m3u8Headers.put("Accept", "*/*");
    }

    private Map<String, String> getHeaders() {
        if (headers == null) init(null, null);
        return headers;
    }

    private Map<String, String> getM3u8Headers() {
        if (m3u8Headers == null) init(null, null);
        return m3u8Headers;
    }

    // ============================================================
    // 工具
    // ============================================================
    private String fetch(String url) {
        return fetch(url, null);
    }

    private String fetch(String url, String referer) {
        try {
            Map<String, String> h = new HashMap<>(getHeaders());
            if (referer != null) h.put("Referer", referer);
            String body = OkHttp.string(url, h);
            return body == null ? "" : body;
        } catch (Exception e) {
            SpiderDebug.log("fetch error: " + e.getMessage());
            return "";
        }
    }

    private String postForm(String url, Map<String, String> data, String referer) {
        try {
            Map<String, String> h = new HashMap<>(getHeaders());
            h.put("Content-Type", "application/x-www-form-urlencoded");
            h.put("X-Requested-With", "XMLHttpRequest");
            if (referer != null) h.put("Referer", referer);
            String body = OkHttp.post(url, data, h).getBody();
            return body == null ? "" : body;
        } catch (Exception e) {
            SpiderDebug.log("postForm error: " + e.getMessage());
            return "";
        }
    }

    private String fixUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        if (url.startsWith("//")) return "https:" + url;
        if (url.startsWith("http")) return url;
        if (url.startsWith("/")) return host + url;
        return host + "/" + url;
    }

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

    private boolean isM3u8(String url) {
        return url != null && url.matches(".*\\.(m3u8|mp4|flv|mkv|webm|ts)(\\?.*)?$");
    }

    private String buildResult(int parse, String url, Map<String, String> h) {
        try {
            JSONObject r = new JSONObject();
            r.put("parse", parse);
            r.put("url", url);
            if (h != null) {
                JSONObject hObj = new JSONObject();
                for (Map.Entry<String, String> e : h.entrySet()) hObj.put(e.getKey(), e.getValue());
                r.put("header", hObj);
            }
            return r.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ============================================================
    // 列表解析
    // ============================================================
    private List<JSONObject> extractList(String html) {
        List<JSONObject> list = new ArrayList<>();
        if (html == null || html.isEmpty()) return list;
        try {
            Matcher cardM = Pattern.compile(
                "<a[^>]*href=\"(/album/\\d+\\.html)\"[^>]*title=\"([^\"]*)\"[^>]*>[\\s\\S]*?" +
                "<img[^>]*(?:data-original|src)=\"([^\"]+)\"[^>]*>",
                Pattern.DOTALL
            ).matcher(html);
            List<String> seen = new ArrayList<>();
            while (cardM.find()) {
                String href = cardM.group(1);
                if (seen.contains(href)) continue;
                seen.add(href);

                JSONObject o = new JSONObject();
                o.put("vod_id", fixUrl(href));
                o.put("vod_name", cardM.group(2).trim());
                o.put("vod_pic", fixUrl(cardM.group(3)));
                o.put("vod_remarks", "");
                list.add(o);
            }
        } catch (Exception e) {
            SpiderDebug.log("extractList error: " + e.getMessage());
        }
        return list;
    }

    // ============================================================
    // homeContent
    // ============================================================
    @Override
    public String homeContent(boolean filter) {
        try {
            JSONObject result = new JSONObject();
            JSONArray classes = new JSONArray();
            // 毒舌分类 slug
            String[][] cfg = {
                {"dianying", "电影"}, {"dianshiju", "电视剧"},
                {"zongyi", "综艺"}, {"dongman", "动漫"}
            };
            for (String[] c : cfg) {
                JSONObject o = new JSONObject();
                o.put("type_id", c[0]);
                o.put("type_name", c[1]);
                classes.put(o);
            }
            result.put("class", classes);
            result.put("filters", buildFilters());
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log("homeContent error: " + e.getMessage());
            return "";
        }
    }

    private JSONObject buildFilters() throws Exception {
        JSONObject filters = new JSONObject();

        String[][] areaValues = {
            {"", "全部"}, {"中国大陆", "中国大陆"}, {"中国香港", "中国香港"},
            {"中国台湾", "中国台湾"}, {"美国", "美国"}, {"日本", "日本"},
            {"韩国", "韩国"}, {"泰国", "泰国"}, {"英国", "英国"},
            {"法国", "法国"}, {"德国", "德国"}, {"其它", "其它"}
        };

        JSONArray yearValues = new JSONArray();
        JSONObject y0 = new JSONObject(); y0.put("v", ""); y0.put("n", "全部"); yearValues.put(y0);
        for (int y = 2026; y >= 2000; y--) {
            JSONObject o = new JSONObject();
            o.put("v", String.valueOf(y));
            o.put("n", String.valueOf(y));
            yearValues.put(o);
        }

        String[][] sortValues = {
            {"", "默认"}, {"time", "按最新"}, {"hits", "按最热"}, {"score", "按评分"}
        };

        filters.put("dianying", buildFilterArray(
            filterGroup("class", "类型", new String[][]{
                {"", "全部"}, {"动作", "动作"}, {"喜剧", "喜剧"}, {"爱情", "爱情"},
                {"科幻", "科幻"}, {"恐怖", "恐怖"}, {"剧情", "剧情"}, {"战争", "战争"},
                {"犯罪", "犯罪"}, {"动画", "动画"}, {"奇幻", "奇幻"}, {"武侠", "武侠"},
                {"悬疑", "悬疑"}, {"惊悚", "惊悚"}, {"古装", "古装"}
            }),
            filterGroup("area", "地区", areaValues),
            filterGroup("year", "年份", yearValues),
            filterGroup("by", "排序", sortValues)
        ));

        filters.put("dianshiju", buildFilterArray(
            filterGroup("class", "类型", new String[][]{
                {"", "全部"}, {"古装", "古装"}, {"战争", "战争"}, {"喜剧", "喜剧"},
                {"家庭", "家庭"}, {"犯罪", "犯罪"}, {"动作", "动作"}, {"奇幻", "奇幻"},
                {"剧情", "剧情"}, {"历史", "历史"}, {"网剧", "网剧"}
            }),
            filterGroup("area", "地区", areaValues),
            filterGroup("year", "年份", yearValues),
            filterGroup("by", "排序", sortValues)
        ));

        filters.put("zongyi", buildFilterArray(
            filterGroup("class", "类型", new String[][]{
                {"", "全部"}, {"选秀", "选秀"}, {"情感", "情感"}, {"访谈", "访谈"},
                {"旅游", "旅游"}, {"音乐", "音乐"}, {"美食", "美食"}, {"生活", "生活"}
            }),
            filterGroup("area", "地区", areaValues),
            filterGroup("year", "年份", yearValues),
            filterGroup("by", "排序", sortValues)
        ));

        filters.put("dongman", buildFilterArray(
            filterGroup("class", "类型", new String[][]{
                {"", "全部"}, {"情感", "情感"}, {"科幻", "科幻"}, {"热血", "热血"},
                {"搞笑", "搞笑"}, {"冒险", "冒险"}, {"动作", "动作"}, {"亲子", "亲子"},
                {"励志", "励志"}
            }),
            filterGroup("area", "地区", areaValues),
            filterGroup("year", "年份", yearValues),
            filterGroup("by", "排序", sortValues)
        ));

        return filters;
    }

    private JSONArray buildFilterArray(JSONObject... items) {
        JSONArray arr = new JSONArray();
        for (JSONObject o : items) arr.put(o);
        return arr;
    }

    private JSONObject filterGroup(String key, String name, String[][] values) throws Exception {
        JSONArray arr = new JSONArray();
        for (String[] v : values) {
            JSONObject o = new JSONObject();
            o.put("n", v[1]);
            o.put("v", v[0]);
            arr.put(o);
        }
        JSONObject obj = new JSONObject();
        obj.put("key", key);
        obj.put("name", name);
        obj.put("value", arr);
        return obj;
    }

    private JSONObject filterGroup(String key, String name, JSONArray values) throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("key", key);
        obj.put("name", name);
        obj.put("value", values);
        return obj;
    }

    // ============================================================
    // homeVideoContent
    // ============================================================
    @Override
    public String homeVideoContent() {
        try {
            String html = fetch(host + "/");
            List<JSONObject> list = extractList(html);
            if (list.size() > 12) list = list.subList(0, 12);
            JSONArray arr = new JSONArray();
            for (JSONObject o : list) arr.put(o);
            JSONObject r = new JSONObject();
            r.put("list", arr);
            return r.toString();
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

            String cls  = extend != null && extend.get("class") != null ? extend.get("class") : "";
            String area = extend != null && extend.get("area") != null ? extend.get("area") : "";
            String year = extend != null && extend.get("year") != null ? extend.get("year") : "";
            String by   = extend != null && extend.get("by") != null ? extend.get("by") : "";

            // 毒舌 URL：/show/{tid}-{area}-{by}-{class}--------{year}.html
            // 先做基础版：/show/{tid}-----------.html + 分页
            // 有筛选时用 /show/{tid}-{area}-{by}-{class}--------{year}.html 之类
            String url;
            if (cls.isEmpty() && area.isEmpty() && year.isEmpty() && by.isEmpty()) {
                // 无筛选
                url = host + "/show/" + tid + "-----------";
                if (page > 1) url += page;
                url += ".html";
            } else {
                // 有筛选：/show/{tid}-{area}-{by}-{class}--------{year}-{page}.html
                StringBuilder sb = new StringBuilder(host);
                sb.append("/show/").append(tid);
                sb.append("-").append(area);
                sb.append("-").append(by);
                sb.append("-").append(cls);
                sb.append("--------").append(year);
                sb.append("--------");  // 补齐
                if (page > 1) sb.append(page);
                sb.append(".html");
                url = sb.toString();
            }

            SpiderDebug.log("category url: " + url);

            String html = fetch(url);
            List<JSONObject> list = extractList(html);
            int pagecount = extractPageCount(html, page, list.size());

            SpiderDebug.log("category list size: " + list.size());

            JSONArray arr = new JSONArray();
            for (JSONObject o : list) arr.put(o);

            JSONObject result = new JSONObject();
            result.put("page", page);
            result.put("list", arr);
            result.put("pagecount", pagecount);
            result.put("limit", 24);
            result.put("total", pagecount * 24);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log("categoryContent error: " + e.getMessage());
            return "";
        }
    }

    private int extractPageCount(String html, int page, int listSize) {
        int pagecount = page;
        try {
            // 找 "共 N 页"
            String total = group("共\\s*(\\d+)\\s*页", html, 1);
            if (!total.isEmpty()) {
                try { return Integer.parseInt(total); } catch (Exception ignored) {}
            }
            // 找尾页
            Matcher m = Pattern.compile("<a[^>]*href=\"[^\"]*-----(\\d+)---[^\"]*\"[^>]*>尾页</a>").matcher(html);
            if (m.find()) {
                try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
            }
            // 兜底：列表 >= 10 认为有下一页
            if (listSize >= 10) pagecount = page + 1;
        } catch (Exception ignored) {}
        return pagecount;
    }

    // ============================================================
    // detailContent
    // ============================================================
    @Override
    public String detailContent(List<String> ids) {
        try {
            String id = ids.get(0);
            String url = id.startsWith("http") ? id : (host + id);
            String html = fetch(url);
            if (html.isEmpty()) {
                JSONObject r = new JSONObject();
                r.put("list", new JSONArray());
                return r.toString();
            }

            JSONObject info = new JSONObject();
            info.put("vod_id", id);

            // ★ 片名
            String name = group("<h1>([^<]+)</h1>", html, 1);
            info.put("vod_name", name.trim());

            // ★ 封面
            String pic = group("<img[^>]*class=\"[^\"]*lazyload[^\"]*\"[^>]*data-original=\"([^\"]+)\"", html, 1);
            info.put("vod_pic", fixUrl(pic));

            // ★ 简介
            String content = group(
                "<div[^>]*class=\"module-info-introduction-content\"[^>]*>[\\s\\S]*?<p>([\\s\\S]*?)</p>",
                html, 1
            );
            content = content.replaceAll("<[^>]+>", "").replace("　", " ").trim();
            info.put("vod_content", content);

            // ★ 年份
            String year = group(
                "<div[^>]*class=\"module-info-tag-link\"[^>]*>[\\s\\S]*?<a[^>]*title=\"(\\d{4})\"",
                html, 1
            );
            info.put("vod_year", year);

            // ★ 地区
            String area = "";
            Matcher areaM = Pattern.compile(
                "<a[^>]*title=\"([^\"]*(?:中国大陆|中国香港|中国台湾|美国|日本|韩国|泰国|英国|法国|德国|印度|加拿大|澳大利亚)[^\"]*)\""
            ).matcher(html);
            if (areaM.find()) area = areaM.group(1).trim();
            info.put("vod_area", area);

            // ★ 类型
            List<String> typeList = new ArrayList<>();
            Matcher tagM = Pattern.compile(
                "<div[^>]*class=\"module-info-tag-link\"[^>]*>[\\s\\S]*?</div>",
                Pattern.DOTALL
            ).matcher(html);
            while (tagM.find()) {
                String block = tagM.group(0);
                Matcher aM = Pattern.compile("<a[^>]*>([^<]+)</a>").matcher(block);
                while (aM.find()) {
                    String t = aM.group(1).trim();
                    if (t.matches("\\d+")) continue;
                    if (t.contains("中国") || t.contains("美国") || t.contains("日本")
                        || t.contains("韩国") || t.contains("香港") || t.contains("台湾")
                        || t.contains("英国") || t.contains("法国") || t.contains("德国")) continue;
                    if (!typeList.contains(t)) typeList.add(t);
                }
            }
            info.put("vod_class", String.join(",", typeList));

            // ★ 导演
            String dirBlock = group(
                "<span[^>]*class=\"module-info-item-title\"[^>]*>导演：</span>[\\s\\S]*?<div[^>]*class=\"module-info-item-content\"[^>]*>([\\s\\S]*?)</div>",
                html, 1
            );
            List<String> dirs = new ArrayList<>();
            Matcher dM = Pattern.compile("<a[^>]*>([^<]+)</a>").matcher(dirBlock);
            while (dM.find()) dirs.add(dM.group(1).trim());
            info.put("vod_director", String.join("/", dirs));

            // ★ 主演
            String actorBlock = group(
                "<span[^>]*class=\"module-info-item-title\"[^>]*>主演：</span>[\\s\\S]*?<div[^>]*class=\"module-info-item-content\"[^>]*>([\\s\\S]*?)</div>",
                html, 1
            );
            List<String> actors = new ArrayList<>();
            Matcher aM2 = Pattern.compile("<a[^>]*>([^<]+)</a>").matcher(actorBlock);
            while (aM2.find()) actors.add(aM2.group(1).trim());
            info.put("vod_actor", String.join("/", actors));

            // ★ 播放列表（方案 B：用 data-dropdown-value 抠中文名）
            String[] pl = extractPlaylist(html);
            info.put("vod_play_from", pl[0]);
            info.put("vod_play_url", pl[1]);

            SpiderDebug.log("detail: name=" + name);
            SpiderDebug.log("detail: from=" + pl[0]);

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
    // ★ extractPlaylist（方案 B）
    // ============================================================
    private String[] extractPlaylist(String html) {
        List<String> playFrom = new ArrayList<>();
        List<String> playUrl = new ArrayList<>();

        // 1. 抠线路名（data-dropdown-value）
        List<String> names = new ArrayList<>();
        Matcher tabM = Pattern.compile(
            "<div[^>]*class=\"module-tab-item tab-item[^\"]*\"[^>]*data-dropdown-value=\"([^\"]+)\"",
            Pattern.DOTALL
        ).matcher(html);
        while (tabM.find()) {
            String name = tabM.group(1).trim();
            if (!name.isEmpty() && !names.contains(name)) names.add(name);
        }
        SpiderDebug.log("detail: lineNames = " + names);

        // 2. 抠剧集块
        Matcher blockM = Pattern.compile(
            "<div[^>]*class=\"module-play-list-content[^\"]*\"[^>]*>([\\s\\S]*?)</div>",
            Pattern.DOTALL
        ).matcher(html);

        int idx = 0;
        while (blockM.find()) {
            String block = blockM.group(1);
            List<String> eps = new ArrayList<>();
            Matcher aM = Pattern.compile(
                "<a[^>]*class=\"module-play-list-link[^\"]*\"[^>]*href=\"([^\"]+)\"[^>]*>[\\s\\S]*?<span>([^<]+)</span>",
                Pattern.DOTALL
            ).matcher(block);
            while (aM.find()) {
                String href = aM.group(1);
                String text = aM.group(2).trim();
                eps.add(text + "$" + fixUrl(href));
            }
            if (!eps.isEmpty()) {
                String fromName = idx < names.size() ? names.get(idx) : ("线路" + (idx + 1));
                playFrom.add(fromName);
                playUrl.add(String.join("#", eps));
                idx++;
            }
        }

        return new String[]{String.join("$$$", playFrom), String.join("$$$", playUrl)};
    }

    // ============================================================
    // searchContent
    // ============================================================
    @Override
    public String searchContent(String wd, boolean quick) {
        return searchContent(wd, quick, "1");
    }

    @Override
    public String searchContent(String wd, boolean quick, String pg) {
        try {
            int page = 1;
            try { page = Integer.parseInt(pg); } catch (Exception ignored) {}
            String url = host + "/search/" + urlEncode(wd) + "-------------.html";
            if (page > 1) url += "?page=" + page;
            String html = fetch(url);
            List<JSONObject> list = extractList(html);

            JSONArray arr = new JSONArray();
            for (JSONObject o : list) arr.put(o);

            JSONObject result = new JSONObject();
            result.put("list", arr);
            result.put("page", page);
            result.put("pagecount", 1);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log("searchContent error: " + e.getMessage());
            return "";
        }
    }

    // ============================================================
    // ★ playerContent（全部返回 parse:0 + m3u8）
    // ============================================================
    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        try {
            // ① 入参直链
            if (isM3u8(id)) {
                return buildResult(0, id, getM3u8Headers());
            }

            // ② 抓播放页
            String pageUrl = id.startsWith("http") ? id : (host + id);
            String html = fetch(pageUrl);
            if (html.isEmpty()) {
                return buildResult(1, pageUrl, null);
            }

            // ③ 抠 player_aaaa
            String pJson = group("var\\s+player_aaaa\\s*=\\s*(\\{[^;]+\\})", html, 1);
            if (pJson.isEmpty()) {
                return buildResult(1, pageUrl, null);
            }

            JSONObject pdata;
            try {
                pdata = new JSONObject(pJson);
            } catch (Exception e) {
                try {
                    String fixed = pJson.replaceAll("([{,])\\s*([a-zA-Z0-9_]+)\\s*:", "$1\"$2\":")
                                       .replaceAll(":\\s*'([^']*)'", ":\"$1\"")
                                       .replaceAll(",\\s*}", "}");
                    pdata = new JSONObject(fixed);
                } catch (Exception e2) {
                    SpiderDebug.log("player_aaaa parse error: " + e2.getMessage());
                    return buildResult(1, pageUrl, null);
                }
            }

            String realUrl = pdata.optString("url", "");
            String from = pdata.optString("from", "");

            SpiderDebug.log("player.url = " + realUrl);
            SpiderDebug.log("player.from = " + from);

            // ④ 直链 m3u8/mp4 → parse:0
            if (isM3u8(realUrl)) {
                SpiderDebug.log("✅ 直链: " + realUrl);
                return buildResult(0, realUrl, getM3u8Headers());
            }

            // ⑤ 第三方 → v.dushe.online 解析
            if (!realUrl.isEmpty()) {
                // 5a. GET v.dushe.online
                String jxUrl = JX_HOST + "/?url=" + urlEncode(realUrl)
                             + "&t=" + urlEncode(from) + "&d=v2";
                SpiderDebug.log("→ v.dushe.online: " + jxUrl);

                String jxHtml = fetch(jxUrl, pageUrl);
                if (jxHtml.isEmpty()) {
                    SpiderDebug.log("❌ v.dushe.online 空响应");
                    return buildResult(1, pageUrl, null);
                }

                // 5b. 抠 config.url
                String configUrl = group("\"url\"\\s*:\\s*\"([^\"]+)\"", jxHtml, 1);
                if (configUrl.isEmpty()) {
                    SpiderDebug.log("❌ 没抠到 config.url");
                    return buildResult(1, pageUrl, null);
                }
                SpiderDebug.log("config.url = " + configUrl);

                // 5c. POST api.php
                LinkedHashMap<String, String> postData = new LinkedHashMap<>();
                postData.put("url", configUrl);
                postData.put("time", "");
                postData.put("key", "");
                postData.put("token", "");

                String apiUrl = JX_HOST + "/api.php";
                String resp = postForm(apiUrl, postData, jxUrl);
                if (resp.isEmpty()) {
                    SpiderDebug.log("❌ api.php 空响应");
                    return buildResult(1, pageUrl, null);
                }
                SpiderDebug.log("api resp: " + (resp.length() > 300 ? resp.substring(0, 300) : resp));

                // 5d. 解析 JSON
                try {
                    JSONObject j = new JSONObject(resp);
                    if (j.optInt("code", 0) == 200 && j.has("url")) {
                        String m3u8 = j.optString("url").replace("\\/", "/");
                        SpiderDebug.log("✅✅ 解析成功: " + m3u8);
                        return buildResult(0, m3u8, getM3u8Headers());
                    }
                    SpiderDebug.log("❌ api code != 200");
                } catch (Exception e) {
                    SpiderDebug.log("❌ JSON error: " + e.getMessage());
                }
            }

            // ⑥ 兜底
            SpiderDebug.log("❌ 全部失败，兜底嗅探");
            return buildResult(1, pageUrl, null);
        } catch (Exception e) {
            SpiderDebug.log("playerContent error: " + e.getMessage());
            return "";
        }
    }

    @Override
    public void destroy() {
        SpiderDebug.log("DuShe destroy");
    }
}