package com.github.catvod.spider;

import android.text.TextUtils;
import android.util.Base64;

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

public class Nm extends Spider {

    private static final String HOST = "https://vip.wwgz.cn:5200";

    private static final String UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.5 Mobile/15E148 Snapchat/10.77.5.59 (like Safari/604.1)";

    private Map<String, String> headers;

    // ============================================================
    // headers（惰性构造，替代 init）
    // ============================================================
    private Map<String, String> getHeaders() {
        if (headers != null) return headers;
        headers = new HashMap<>();
        headers.put("User-Agent", UA);
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.put("accept-language", "zh-CN,zh;q=0.9");
        headers.put("cache-control", "no-cache");
        headers.put("pragma", "no-cache");
        headers.put("upgrade-insecure-requests", "1");
        return headers;
    }

    // ============================================================
    // 工具
    // ============================================================
    private String fetch(String url, String referer) {
        try {
            Map<String, String> h = new HashMap<>(getHeaders());
            h.put("Referer", TextUtils.isEmpty(referer) ? (HOST + "/") : referer);
            h.put("sec-fetch-site", TextUtils.isEmpty(referer) ? "none" : "same-origin");
            String body = OkHttp.string(url, h);
            return body == null ? "" : body;
        } catch (Exception e) {
            SpiderDebug.log("fetch error: " + e.getMessage());
            return "";
        }
    }

    private String post(String url, Map<String, String> data, String referer) {
        try {
            Map<String, String> h = new HashMap<>(getHeaders());
            h.put("Referer", TextUtils.isEmpty(referer) ? (HOST + "/") : referer);
            h.put("Content-Type", "application/x-www-form-urlencoded");
            String body = OkHttp.post(url, data, h).getBody();
            return body == null ? "" : body;
        } catch (Exception e) {
            SpiderDebug.log("post error: " + e.getMessage());
            return "";
        }
    }

    private String fixPic(String url) {
        if (url == null || url.isEmpty()) return "";
        if (url.startsWith("//")) return "http:" + url;
        if (url.startsWith("/")) return HOST + url;
        return url;
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

    // ============================================================
    // 列表解析（原版用 cheerio：.globalPicList li, .resize_list li）
    // ============================================================
    private List<JSONObject> parseList(String html) {
        List<JSONObject> list = new ArrayList<>();
        if (html == null || html.isEmpty()) return list;
        try {
            // 匹配 li 块：<li ...> ... <a href="...vod-detail-id..."> ... <img ...> ... </a> ... </li>
            Matcher liM = Pattern.compile("<li[^>]*>([\\s\\S]*?)</li>").matcher(html);
            List<String> seen = new ArrayList<>();
            while (liM.find()) {
                String block = liM.group(1);
                String href = group("<a[^>]*href=\"([^\"]*vod-detail-id[^\"]*)\"", block, 1);
                if (href.isEmpty()) continue;
                if (seen.contains(href)) continue;
                seen.add(href);

                String title = group("<a[^>]*title=\"([^\"]*)\"", block, 1);
                if (title.isEmpty()) {
                    title = group("<[^>]*class=\"[^\"]*sTit[^\"]*\"[^>]*>([^<]+)</", block, 1);
                }

                String pic = group("<img[^>]*data-echo=\"([^\"]+)\"", block, 1);
                if (pic.isEmpty()) pic = group("<img[^>]*src=\"([^\"]+)\"", block, 1);

                String remark = group("<[^>]*class=\"[^\"]*sBottom[^\"]*\"[^>]*>[\\s\\S]*?<span[^>]*>([^<]*)</span>", block, 1);
                if (remark.isEmpty()) {
                    remark = group("<[^>]*class=\"[^\"]*covericon[^\"]*\"[^>]*>([^<]*)</", block, 1);
                }
                remark = remark.replaceAll("<[^>]+>", "").trim();

                JSONObject o = new JSONObject();
                o.put("vod_id", href);
                o.put("vod_name", title.trim());
                o.put("vod_pic", fixPic(pic));
                o.put("vod_remarks", remark);
                list.add(o);
            }
        } catch (Exception e) {
            SpiderDebug.log("parseList error: " + e.getMessage());
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
            String[][] cfg = {
                {"1", "电影"}, {"2", "连续剧"}, {"3", "综艺"},
                {"4", "动漫"}, {"26", "短剧"}
            };
            for (String[] c : cfg) {
                JSONObject o = new JSONObject();
                o.put("type_id", c[0]);
                o.put("type_name", c[1]);
                classes.put(o);
            }
            result.put("class", classes);

            JSONObject filters = new JSONObject();

            JSONArray yearArr = buildYears();
            JSONArray areaArr = buildAreas();
            JSONArray byArr = buildBy();

            // 1 电影
            JSONArray f1 = new JSONArray();
            f1.put(filterGroup("id", "类型", new String[][]{
                {"全部", ""}, {"动作片", "5"}, {"喜剧片", "6"}, {"爱情片", "7"},
                {"科幻片", "8"}, {"恐怖片", "9"}, {"剧情片", "10"}, {"战争片", "11"},
                {"惊悚片", "16"}, {"奇幻片", "17"}
            }));
            f1.put(filterGroup("area", "地区", areaArr));
            f1.put(filterGroup("year", "年份", yearArr));
            f1.put(filterGroup("by", "排序", byArr));
            filters.put("1", f1);

            // 2 连续剧
            JSONArray f2 = new JSONArray();
            f2.put(filterGroup("id", "类型", new String[][]{
                {"全部", ""}, {"国产剧", "12"}, {"港台泰", "13"},
                {"日韩剧", "14"}, {"欧美剧", "15"}
            }));
            f2.put(filterGroup("area", "地区", areaArr));
            f2.put(filterGroup("year", "年份", yearArr));
            f2.put(filterGroup("by", "排序", byArr));
            filters.put("2", f2);

            // 3 综艺
            JSONArray f3 = new JSONArray();
            f3.put(filterGroup("id", "类型", new String[][]{{"全部", ""}}));
            f3.put(filterGroup("area", "地区", areaArr));
            f3.put(filterGroup("year", "年份", yearArr));
            f3.put(filterGroup("by", "排序", byArr));
            filters.put("3", f3);

            // 4 动漫
            JSONArray f4 = new JSONArray();
            f4.put(filterGroup("id", "类型", new String[][]{
                {"全部", ""}, {"动漫剧", "18"}
            }));
            f4.put(filterGroup("area", "地区", areaArr));
            f4.put(filterGroup("year", "年份", yearArr));
            f4.put(filterGroup("by", "排序", byArr));
            filters.put("4", f4);

            // 26 短剧
            JSONArray f26 = new JSONArray();
            f26.put(filterGroup("id", "类型", new String[][]{{"全部", ""}}));
            f26.put(filterGroup("area", "地区", areaArr));
            f26.put(filterGroup("year", "年份", yearArr));
            f26.put(filterGroup("by", "排序", byArr));
            filters.put("26", f26);

            result.put("filters", filters);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log("homeContent error: " + e.getMessage());
            return "";
        }
    }

    private JSONArray buildYears() throws Exception {
        JSONArray arr = new JSONArray();
        JSONObject all = new JSONObject();
        all.put("n", "全部"); all.put("v", "");
        arr.put(all);
        for (int y = 2026; y >= 1900; y--) {
            JSONObject o = new JSONObject();
            o.put("n", String.valueOf(y));
            o.put("v", String.valueOf(y));
            arr.put(o);
        }
        return arr;
    }

    private JSONArray buildAreas() throws Exception {
        String[][] data = {
            {"全部", ""}, {"大陆", "大陆"}, {"香港", "香港"}, {"台湾", "台湾"},
            {"美国", "美国"}, {"韩国", "韩国"}, {"日本", "日本"}, {"泰国", "泰国"},
            {"新加坡", "新加坡"}, {"马来西亚", "马来西亚"}, {"印度", "印度"},
            {"英国", "英国"}, {"法国", "法国"}, {"加拿大", "加拿大"},
            {"西班牙", "西班牙"}, {"俄罗斯", "俄罗斯"}, {"其它", "其它"}
        };
        JSONArray arr = new JSONArray();
        for (String[] kv : data) {
            JSONObject o = new JSONObject();
            o.put("n", kv[0]); o.put("v", kv[1]);
            arr.put(o);
        }
        return arr;
    }

    private JSONArray buildBy() throws Exception {
        String[][] data = {
            {"全部", "time"}, {"人气", "hits"}, {"评分", "score"}
        };
        JSONArray arr = new JSONArray();
        for (String[] kv : data) {
            JSONObject o = new JSONObject();
            o.put("n", kv[0]); o.put("v", kv[1]);
            arr.put(o);
        }
        return arr;
    }

    private JSONObject filterGroup(String key, String name, String[][] data) throws Exception {
        JSONArray arr = new JSONArray();
        for (String[] kv : data) {
            JSONObject o = new JSONObject();
            o.put("n", kv[0]); o.put("v", kv[1]);
            arr.put(o);
        }
        return filterGroup(key, name, arr);
    }

    private JSONObject filterGroup(String key, String name, JSONArray arr) throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("key", key);
        obj.put("name", name);
        obj.put("init", "");
        obj.put("value", arr);
        return obj;
    }

    // ============================================================
    // homeVideoContent
    // ============================================================
    @Override
    public String homeVideoContent() {
        try {
            String html = fetch(HOST + "/", null);
            List<JSONObject> list = parseList(html);
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

            // home 分类
            if ("home".equals(tid)) {
                String html = fetch(HOST + "/", null);
                List<JSONObject> list = parseList(html);
                JSONArray arr = new JSONArray();
                for (JSONObject o : list) arr.put(o);
                JSONObject r = new JSONObject();
                r.put("page", 1);
                r.put("pagecount", 1);
                r.put("list", arr);
                return r.toString();
            }

            String id = tid;
            if (extend != null && extend.get("id") != null && !extend.get("id").isEmpty()) {
                id = extend.get("id");
            }
            // 抠数字
            Matcher m = Pattern.compile("\\d+").matcher(id);
            String s = m.find() ? m.group() : "1";

            String rBy = extend != null && extend.get("by") != null ? extend.get("by") : "time";
            if (rBy.isEmpty()) rBy = "time";
            String cls = extend != null && extend.get("class") != null ? extend.get("class") : "";
            String year = extend != null && extend.get("year") != null ? extend.get("year") : "";
            String letter = extend != null && extend.get("letter") != null ? extend.get("letter") : "";
            String area = extend != null && extend.get("area") != null ? extend.get("area") : "";
            String lang = extend != null && extend.get("lang") != null ? extend.get("lang") : "";

            String url = HOST + "/index.php?m=vod-list-id-" + s
                    + "-pg-" + page
                    + "-order--by-" + rBy
                    + "-class-" + cls
                    + "-year-" + year
                    + "-letter-" + letter
                    + "-area-" + urlEncode(area)
                    + "-lang-" + urlEncode(lang)
                    + ".html";

            String html = fetch(url, null);
            List<JSONObject> list = parseList(html);

            JSONArray arr = new JSONArray();
            for (JSONObject o : list) arr.put(o);

            JSONObject r = new JSONObject();
            r.put("page", page);
            r.put("pagecount", list.size() > 0 ? page + 1 : 1);
            r.put("list", arr);
            return r.toString();
        } catch (Exception e) {
            SpiderDebug.log("categoryContent error: " + e.getMessage());
            return "";
        }
    }

    // ============================================================
    // search
    // ============================================================
    @Override
    public String searchContent(String wd, boolean quick) {
        try {
            String url = HOST + "/index.php?m=vod-search";
            Map<String, String> data = new HashMap<>();
            data.put("wd", wd);

            String resp = post(url, data, HOST + "/vod-search");

            List<JSONObject> list = new ArrayList<>();
            // 抠 #data_list li
            Matcher liM = Pattern.compile("<li[^>]*>([\\s\\S]*?)</li>").matcher(resp);
            while (liM.find()) {
                String block = liM.group(1);
                if (!block.contains("vod-detail-id")) continue;

                String href = group("<a[^>]*href=\"([^\"]*vod-detail-id[^\"]*)\"", block, 1);
                if (href.isEmpty()) continue;

                String pic = group("<img[^>]*data-src=\"([^\"]+)\"", block, 1);
                if (pic.isEmpty()) pic = group("<img[^>]*src=\"([^\"]+)\"", block, 1);

                String title = group("<[^>]*class=\"[^\"]*sTit[^\"]*\"[^>]*>([^<]+)</", block, 1);
                String style = group("<[^>]*class=\"[^\"]*sStyle[^\"]*\"[^>]*>([^<]+)</", block, 1);

                String score = "";
                String actor = "";
                Matcher dM = Pattern.compile("<[^>]*class=\"[^\"]*sDes[^\"]*\"[^>]*>([^<]+)</").matcher(block);
                while (dM.find()) {
                    String txt = dM.group(1);
                    if (txt.contains("评分")) score = txt.replaceAll("^.*?评分[：:]?\\s*", "").trim();
                    if (txt.contains("主演")) actor = txt.replaceAll("^.*?主演[：:]?\\s*", "").trim();
                }

                List<String> parts = new ArrayList<>();
                if (!style.isEmpty()) parts.add(style);
                if (!score.isEmpty()) parts.add(score + "分");
                if (!actor.isEmpty()) parts.add(actor);
                String remark = String.join(" | ", parts);

                JSONObject o = new JSONObject();
                o.put("vod_id", href);
                o.put("vod_name", title.trim());
                o.put("vod_pic", fixPic(pic));
                o.put("vod_remarks", remark);
                list.add(o);
            }

            JSONArray arr = new JSONArray();
            for (JSONObject o : list) arr.put(o);
            JSONObject r = new JSONObject();
            r.put("list", arr);
            return r.toString();
        } catch (Exception e) {
            SpiderDebug.log("searchContent error: " + e.getMessage());
            return "";
        }
    }

    @Override
    public String searchContent(String wd, boolean quick, String pg) {
        return searchContent(wd, quick);
    }

    // ============================================================
    // detailContent
    // ============================================================
    @Override
    public String detailContent(List<String> ids) {
        try {
            String vid = ids.get(0);
            String url = vid.startsWith("http") ? vid : (HOST + vid);
            String html = fetch(url, null);

            JSONObject info = new JSONObject();
            info.put("vod_id", vid);

            String name = group("<h1[^>]*class=\"title\"[^>]*>([^<]+)</h1>", html, 1);
            if (name.isEmpty()) name = group("<[^>]*class=\"page-bd\"[^>]*>[\\s\\S]*?<h1[^>]*>([^<]+)</h1>", html, 1);
            info.put("vod_name", name.trim());

            String pic = group("<[^>]*class=\"page-hd\"[^>]*>[\\s\\S]*?<img[^>]*src=\"([^\"]+)\"", html, 1);
            info.put("vod_pic", fixPic(pic));

            // 年代
            String year = group("<[^>]*class=\"desc_item\"[^>]*>[^<]*年代[：:][\\s\\S]*?<a[^>]*>([^<]+)</a>", html, 1);
            info.put("vod_year", year.trim());

            // 状态
            String remarks = group("<[^>]*class=\"desc_item\"[^>]*>[^<]*状态[：:][\\s\\S]*?<font[^>]*>([^<]+)</font>", html, 1);
            info.put("vod_remarks", remarks.trim());

            // 主演
            String actor = group("<[^>]*class=\"desc_item\"[^>]*>[^<]*主演[：:]([\\s\\S]*?)</div>", html, 1);
            List<String> actorList = new ArrayList<>();
            Matcher aM = Pattern.compile("<a[^>]*>([^<]+)</a>").matcher(actor);
            while (aM.find()) actorList.add(aM.group(1).trim());
            info.put("vod_actor", String.join(" ", actorList));

            // 简介
            String content = group("<[^>]*class=\"detail-con\"[^>]*>[\\s\\S]*?<p[^>]*>([\\s\\S]*?)</p>", html, 1);
            content = content.replaceAll("<[^>]+>", "").replaceAll("简[\\s\\S]*?介[：:]\\s*", "").trim();
            info.put("vod_content", content);

            // 播放按钮
            String playBtn = group("<[^>]*class=\"page-btn\"[^>]*>[\\s\\S]*?<a[^>]*class=\"greenBtn\"[^>]*href=\"([^\"]+)\"", html, 1);

            info.put("vod_play_from", "");
            info.put("vod_play_url", "");

            if (!playBtn.isEmpty()) {
                String playUrl = playBtn.startsWith("http") ? playBtn : (HOST + playBtn);
                String playHtml = fetch(playUrl, url);

                String macFrom = group("mac_from='([^']+)'", playHtml, 1);
                String macUrl = group("mac_url='([^']+)'", playHtml, 1);

                if (!macFrom.isEmpty() && !macUrl.isEmpty()) {
                    // 从 #leftTabBox 抠线路名
                    List<String> lineNames = new ArrayList<>();
                    String tabBox = group("<div[^>]*id=\"leftTabBox\"[^>]*>[\\s\\S]*?<ul>([\\s\\S]*?)</ul>", playHtml, 1);
                    if (!tabBox.isEmpty()) {
                        Matcher liM = Pattern.compile("<li[^>]*>[\\s\\S]*?</li>").matcher(tabBox);
                        while (liM.find()) {
                            String li = liM.group();
                            String nm = group("<a[^>]*>([^<]+)</a>", li, 1);
                            if (!nm.isEmpty()) lineNames.add(nm.trim());
                        }
                    }

                    // 兜底
                    if (lineNames.isEmpty()) {
                        for (String x : macFrom.split("\\$\\$\\$")) lineNames.add(x);
                    }

                    String[] urlLines = macUrl.split("\\$\\$\\$");
                    List<String> playFrom = new ArrayList<>();
                    List<String> playUrlList = new ArrayList<>();

                    for (int j = 0; j < urlLines.length; j++) {
                        String nm = (j < lineNames.size() && !lineNames.get(j).isEmpty())
                                ? lineNames.get(j) : ("线路" + (j + 1));
                        String[] eps = urlLines[j].split("#");
                        List<String> epList = new ArrayList<>();
                        for (String ep : eps) {
                            String[] parts = ep.split("\\$");
                            if (parts.length == 2) {
                                epList.add(parts[0] + "$" + parts[1]);
                            } else {
                                epList.add(ep);
                            }
                        }
                        playFrom.add(nm);
                        playUrlList.add(String.join("#", epList));
                    }

                    info.put("vod_play_from", String.join("$$$", playFrom));
                    info.put("vod_play_url", String.join("$$$", playUrlList));
                }
            }

            JSONArray list = new JSONArray();
            list.put(info);
            JSONObject r = new JSONObject();
            r.put("list", list);
            return r.toString();
        } catch (Exception e) {
            SpiderDebug.log("detailContent error: " + e.getMessage());
            return "";
        }
    }

    // ============================================================
    // playerContent
    // ============================================================
    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        try {
            JSONObject result = new JSONObject();
            int parse = 0;

            String realUrl = decodeNmUrl(id);

            if (realUrl.isEmpty() || !realUrl.startsWith("http")) {
                String nmUrl = "https://api.nmvod.me:520/player/?url=";
                String v = fetch(nmUrl + id, null);
                String m = group("\"url\"\\s*:\\s*\"([^\"]+)\"", v, 1);
                if (!m.isEmpty()) {
                    realUrl = m.replace("\\/", "/");
                } else {
                    realUrl = nmUrl + id;
                    parse = 1;
                }
            }

            result.put("parse", parse);
            result.put("url", realUrl);

            JSONObject h = new JSONObject();
            h.put("User-Agent", UA);
            result.put("header", h);

            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log("playerContent error: " + e.getMessage());
            return "";
        }
    }

    // ============================================================
    // decodeNmUrl — 农民自定义 Base64 解码
    // ============================================================
    private String decodeNmUrl(String s) {
        try {
            if (s == null) return "";
            s = s.replaceAll("#+$", "");
            if (s.length() < 66) return "";

            String first = s.substring(0, 66);
            String rest = s.substring(66);

            StringBuilder even = new StringBuilder();
            for (int i = 0; i < first.length(); i += 2) {
                even.append(first.charAt(i));
            }

            String merged = even.toString()
                    + rest.replace("O0O0O", "=")
                          .replace("oo00o", "/")
                          .replace("o000o", "+");

            byte[] decoded = Base64.decode(merged, Base64.DEFAULT);
            return new String(decoded, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void destroy() {
        SpiderDebug.log("NongMin destroy");
    }
}