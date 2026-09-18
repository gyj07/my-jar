package com.nsvod;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 耐视点播 Spider - Java 版
 * 策略：直链优先 → jxapi 主动抠 → artplayer 嗅探 → 空
 */
public class NsVodSpider {

    static final String HOST = "https://nsvod.cc";
    static final String SALT = "DCC147D11943AF75";

    // ============================================================
    // MD5（分类签名用）
    // ============================================================
    static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ============================================================
    // 工具
    // ============================================================
    static String fixUrl(String u) {
        if (u == null || u.isEmpty()) return "";
        u = u.replace("\\/", "/").trim();
        if (u.startsWith("//")) return "https:" + u;
        if (u.startsWith("http")) return u;
        if (u.startsWith("/")) return HOST + u;
        return HOST + "/" + u;
    }

    static String stripTags(String s) {
        if (s == null) return "";
        return s.replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    static String decodeHtml(String s) {
        if (s == null) return "";
        return s.replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&nbsp;", " ");
    }

    static String group(String regex, String text, int group) {
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(text);
        return m.find() ? m.group(group) : "";
    }

    // ============================================================
    // 首页
    // ============================================================
    public static String home() {
        JSONObject result = new JSONObject();
        try {
            JSONArray classes = new JSONArray();
            String[][] cls = {
                {"1", "电影"}, {"2", "连续剧"}, {"3", "综艺"},
                {"4", "动漫"}, {"41", "短剧"}, {"40", "纪录片"}, {"37", "Netflix"}
            };
            for (String[] c : cls) {
                JSONObject o = new JSONObject();
                o.put("type_id", c[0]);
                o.put("type_name", c[1]);
                classes.put(o);
            }
            result.put("class", classes);

            // 筛选（简化版，只放年份）
            JSONObject filters = new JSONObject();
            JSONArray years = new JSONArray();
            JSONObject all = new JSONObject();
            all.put("v", ""); all.put("n", "全部");
            years.put(all);
            for (int y = 2026; y >= 2014; y--) {
                JSONObject yo = new JSONObject();
                yo.put("v", String.valueOf(y));
                yo.put("n", String.valueOf(y));
                years.put(yo);
            }
            JSONArray fa = new JSONArray();
            JSONObject fObj = new JSONObject();
            fObj.put("key", "year");
            fObj.put("name", "年份");
            fObj.put("value", years);
            fa.put(fObj);
            filters.put("1", fa);
            filters.put("2", fa);
            filters.put("3", fa);
            filters.put("4", fa);
            result.put("filters", filters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    // ============================================================
    // 首页推荐
    // ============================================================
    public static String homeVod() {
        JSONObject result = new JSONObject();
        try {
            String html = Http.get(HOST + "/", Http.htmlHeaders(HOST + "/"));
            JSONArray list = parseHtmlList(html);
            // 取前 30
            JSONArray out = new JSONArray();
            for (int i = 0; i < Math.min(30, list.length()); i++) out.put(list.get(i));
            result.put("list", out);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("list", new JSONArray());
        }
        return result.toString();
    }

    // ============================================================
    // 分类（API + 签名）
    // ============================================================
    public static String category(String tid, String pg, String extendJson) {
        JSONObject result = new JSONObject();
        try {
            int page = 1;
            try { page = Integer.parseInt(pg); } catch (Exception ignored) {}

            String cls = "", area = "", year = "", by = "", lang = "";
            if (extendJson != null && !extendJson.isEmpty()) {
                try {
                    JSONObject ext = new JSONObject(extendJson);
                    cls  = ext.optString("class", "");
                    area = ext.optString("area", "");
                    year = ext.optString("year", "");
                    by   = ext.optString("by", "");
                    lang = ext.optString("lang", "");
                } catch (Exception ignored) {}
            }
            if ("全部".equals(cls) || "不限".equals(cls)) cls = "";
            if ("全部".equals(area)) area = "";
            if ("全部".equals(year)) year = "";

            long t = System.currentTimeMillis() / 1000;
            String key = md5("DS" + t + SALT);

            StringBuilder body = new StringBuilder();
            body.append("type=").append(tid);
            body.append("&class=").append(URLEncoder.encode(cls, "UTF-8"));
            body.append("&area=").append(URLEncoder.encode(area, "UTF-8"));
            body.append("&lang=").append(URLEncoder.encode(lang, "UTF-8"));
            if (!year.isEmpty()) body.append("&year=").append(URLEncoder.encode(year, "UTF-8"));
            body.append("&version=&state=&letter=");
            if (!by.isEmpty()) body.append("&order=").append(by);
            body.append("&page=").append(page);
            body.append("&time=").append(t);
            body.append("&key=").append(key);

            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", Http.UA);
            headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
            headers.put("X-Requested-With", "XMLHttpRequest");
            headers.put("Origin", HOST);
            headers.put("Referer", HOST + "/vodshow/" + tid + "-----------.html");

            String resp = Http.post(HOST + "/index.php/api/vod", body.toString(), headers);
            JSONObject data = new JSONObject(resp);
            JSONArray srcList = data.optJSONArray("list");
            JSONArray out = new JSONArray();
            if (srcList != null) {
                for (int i = 0; i < srcList.length(); i++) {
                    JSONObject it = srcList.getJSONObject(i);
                    JSONObject o = new JSONObject();
                    o.put("vod_id", it.optString("vod_id"));
                    o.put("vod_name", it.optString("vod_name"));
                    o.put("vod_pic", fixUrl(it.optString("vod_pic")));
                    o.put("vod_remarks", it.optString("vod_remarks"));
                    out.put(o);
                }
            }
            result.put("page", page);
            result.put("list", out);
            result.put("pagecount", data.optInt("pagecount", 1));
            result.put("limit", data.optInt("limit", 40));
            result.put("total", data.optInt("total", 0));
        } catch (Exception e) {
            e.printStackTrace();
            try {
                result.put("page", 1);
                result.put("list", new JSONArray());
                result.put("pagecount", 1);
                result.put("limit", 40);
                result.put("total", 0);
            } catch (Exception ignored) {}
        }
        return result.toString();
    }

    // ============================================================
    // HTML 列表解析
    // ============================================================
    static JSONArray parseHtmlList(String html) {
        JSONArray list = new JSONArray();
        if (html == null || html.isEmpty()) return list;
        try {
            Matcher blockM = Pattern.compile(
                    "<div class=\"public-list-box[^\"]*\">([\\s\\S]*?)</div>\\s*</div>\\s*</div>"
            ).matcher(html);

            while (blockM.find()) {
                String block = blockM.group(1);
                String id = group("href=\"/voddetail/(\\d+)\\.html\"", block, 1);
                if (id.isEmpty()) continue;

                String name = group("<a[^>]*class=\"time-title[^\"]*\"[^>]*title=\"([^\"]+)\"", block, 1);
                if (name.isEmpty()) {
                    name = group("<a[^>]*class=\"time-title[^\"]*\"[^>]*>([^<]+)</a>", block, 1);
                }
                name = decodeHtml(name.trim());

                String pic = group("data-src=\"([^\"]+)\"", block, 1);
                if (pic.isEmpty()) pic = group("src=\"([^\"]+)\"", block, 1);
                if (pic.contains("data:image")) pic = "";
                pic = fixUrl(pic);

                String remark = group("<span class=\"public-list-prb[^\"]*\"[^>]*>([^<]+)</span>", block, 1);
                if (remark.isEmpty()) {
                    remark = group("<div class=\"public-list-subtitle[^\"]*\"[^>]*>([^<]*)</div>", block, 1);
                }
                remark = stripTags(remark);
                if (remark.length() > 30) remark = remark.substring(0, 30);

                JSONObject o = new JSONObject();
                o.put("vod_id", id);
                o.put("vod_name", name);
                o.put("vod_pic", pic);
                o.put("vod_remarks", remark);
                list.put(o);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // ============================================================
    // 详情
    // ============================================================
    public static String detail(String id) {
        JSONObject result = new JSONObject();
        try {
            String url = HOST + "/voddetail/" + id + ".html";
            String html = Http.get(url, Http.htmlHeaders(HOST + "/"));
            if (html.isEmpty()) {
                result.put("list", new JSONArray());
                return result.toString();
            }

            JSONObject info = new JSONObject();
            info.put("vod_id", id);
            info.put("vod_name", decodeHtml(group("<h3 class=\"slide-info-title[^\"]*\"[^>]*>([^<]+)</h3>", html, 1).trim()));
            info.put("vod_pic", fixUrl(group("class=\"lazy lazy1 mask-1[^\"]*\"[^>]*(?:data-src|src)=\"([^\"]+)\"", html, 1)));

            // 演员
            String actors = group("演员\\s*[:：]</strong>([\\s\\S]*?)</div>", html, 1);
            info.put("vod_actor", extractLinks(actors));
            // 导演
            String directors = group("导演\\s*[:：]</strong>([\\s\\S]*?)</div>", html, 1);
            info.put("vod_director", extractLinks(directors));
            // 类型
            String types = group("类型\\s*[:：]</strong>([\\s\\S]*?)</div>", html, 1);
            info.put("vod_class", extractLinks(types));
            // 简介
            info.put("vod_content", stripTags(group(
                    "<div id=\"height_limit\"[^>]*class=\"text[^\"]*\"[^>]*>([\\s\\S]*?)</div>", html, 1)));

            // 播放列表
            String[] pl = parsePlaylist(html);
            info.put("vod_play_from", pl[0]);
            info.put("vod_play_url", pl[1]);

            JSONArray list = new JSONArray();
            list.put(info);
            result.put("list", list);
        } catch (Exception e) {
            e.printStackTrace();
            try { result.put("list", new JSONArray()); } catch (Exception ignored) {}
        }
        return result.toString();
    }

    static String extractLinks(String block) {
        if (block == null) return "";
        StringBuilder sb = new StringBuilder();
        Matcher m = Pattern.compile("<a[^>]*>([^<]+)</a>").matcher(block);
        while (m.find()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(stripTags(m.group(1)));
        }
        return sb.toString();
    }

    // ============================================================
    // 播放列表解析
    // ============================================================
    static String[] parsePlaylist(String html) {
        StringBuilder fromSb = new StringBuilder();
        StringBuilder urlSb = new StringBuilder();

        // 线路名
        List<String> names = new ArrayList<>();
        Matcher tabM = Pattern.compile("<a[^>]*class=\"[^\"]*swiper-slide[^\"]*\"[^>]*>([\\s\\S]*?)</a>").matcher(html);
        while (tabM.find()) {
            String a = tabM.group(1);
            String name = a.replaceAll("<i[^>]*>[\\s\\S]*?</i>", "")
                            .replaceAll("<span[^>]*class=\"badge\"[^>]*>[\\s\\S]*?</span>", "")
                            .replaceAll("<[^>]+>", "")
                            .replace("&nbsp;", " ")
                            .replaceAll("\\s+", " ")
                            .trim();
            if (!name.isEmpty() && !names.contains(name)) names.add(name);
        }
        if (names.isEmpty()) names.add("线路1");

        // 剧集列表
        Matcher boxM = Pattern.compile(
                "<div class=\"anthology-list-box[^\"]*\"[^>]*>([\\s\\S]*?)</ul>\\s*</div>\\s*</div>"
        ).matcher(html);

        int idx = 0;
        while (boxM.find()) {
            String box = boxM.group(1);
            StringBuilder epSb = new StringBuilder();
            Matcher aM = Pattern.compile("<a[^>]*href=\"([^\"]+)\"[^>]*>([^<]+)</a>").matcher(box);
            while (aM.find()) {
                String href = aM.group(1);
                String name = aM.group(2).trim();
                if (href.contains("/vodplay/")) {
                    if (epSb.length() > 0) epSb.append("#");
                    epSb.append(name).append("$").append(fixUrl(href));
                }
            }
            if (epSb.length() > 0) {
                if (fromSb.length() > 0) fromSb.append("$$$");
                if (urlSb.length() > 0) urlSb.append("$$$");
                String lineName = idx < names.size() ? names.get(idx) : ("线路" + (idx + 1));
                fromSb.append(lineName);
                urlSb.append(epSb);
                idx++;
            }
        }
        return new String[]{fromSb.toString(), urlSb.toString()};
    }

    // ============================================================
    // 搜索
    // ============================================================
    public static String search(String wd, String pg) {
        JSONObject result = new JSONObject();
        try {
            int page = 1;
            try { page = Integer.parseInt(pg); } catch (Exception ignored) {}
            String url = HOST + "/vodsearch/-------------.html?wd=" +
                    URLEncoder.encode(wd, "UTF-8");
            if (page > 1) url += "&page=" + page;
            String html = Http.get(url, Http.htmlHeaders(HOST + "/"));
            JSONArray list = parseHtmlList(html);
            result.put("list", list);
            result.put("page", page);
            result.put("pagecount", 1);
        } catch (Exception e) {
            e.printStackTrace();
            try { result.put("list", new JSONArray()); } catch (Exception ignored) {}
        }
        return result.toString();
    }

    // ============================================================
    // 播放（核心）
    // ============================================================
    public static String play(String flag, String id) {
        JSONObject result = new JSONObject();
        try {
            // ① 入参直链
            if (id.matches(".*\\.(m3u8|mp4|flv|mkv|webm|ts)(\\?.*)?$")) {
                result.put("parse", 0);
                result.put("url", id);
                result.put("header", new JSONObject(Http.m3u8Headers()));
                return result.toString();
            }

            // ② 抓播放页
            String pageUrl = id.startsWith("http") ? id : HOST + id;
            String html = Http.get(pageUrl, Http.htmlHeaders(HOST + "/"));
            if (html.isEmpty() || !html.contains("player_aaaa")) {
                try { Thread.sleep(400); } catch (InterruptedException ignored) {}
                html = Http.get(pageUrl, Http.htmlHeaders(HOST + "/"));
            }
            if (html.isEmpty()) {
                result.put("parse", 0);
                result.put("url", "");
                return result.toString();
            }

            // ③ 抠 player_aaaa
            String realUrl = "";
            String from = "";
            String pm = group("var\\s+player_aaaa\\s*=\\s*(\\{[\\s\\S]*?\\})\\s*</script>", html, 1);
            if (!pm.isEmpty()) {
                try {
                    JSONObject p = new JSONObject(pm);
                    realUrl = p.optString("url", "").replace("\\/", "/");
                    if (realUrl.startsWith("//")) realUrl = "https:" + realUrl;
                    from = p.optString("from", "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            System.out.println("player.url: " + realUrl);
            System.out.println("player.from: " + from);

            // ④ m3u8/mp4 → 直连
            if (realUrl.matches(".*\\.(m3u8|mp4|flv)(\\?.*)?$")) {
                result.put("parse", 0);
                result.put("url", realUrl);
                result.put("header", new JSONObject(Http.m3u8Headers()));
                return result.toString();
            }

            // ⑤ 第三方源 → 读 playerconfig，拿 jxapi / artplayer
            if (!realUrl.isEmpty()) {
                String[] cfg = readPlayerConfig(html);
                String jxapi = cfg[0];
                String artplayer = cfg[1];

                // 5.1 先试 jxapi
                if (!jxapi.isEmpty()) {
                    String apiUrl = jxapi + URLEncoder.encode(realUrl, "UTF-8");
                    System.out.println("→ jxapi: " + apiUrl);

                    Map<String, String> h = new HashMap<>();
                    h.put("User-Agent", Http.UA);
                    h.put("Accept", "application/json,*/*");
                    h.put("Referer", HOST + "/");
                    String resp = Http.get(apiUrl, h);

                    if (!resp.isEmpty()) {
                        try {
                            JSONObject j = new JSONObject(resp);
                            if (j.optInt("code") == 200 && j.has("url")) {
                                String videoUrl = j.optString("url").replace("\\/", "/");
                                System.out.println("✅ jxapi 返回: " + videoUrl);
                                result.put("parse", 0);
                                result.put("url", videoUrl);
                                result.put("header", new JSONObject(Http.m3u8Headers()));
                                return result.toString();
                            }
                        } catch (Exception e) {
                            System.out.println("jxapi JSON 失败: " + e.getMessage());
                        }
                    }
                }

                // 5.2 jxapi 不行 → artplayer 嗅探
                if (!artplayer.isEmpty()) {
                    String sniffUrl = artplayer + URLEncoder.encode(realUrl, "UTF-8");
                    System.out.println("⚠️ artplayer 嗅探: " + sniffUrl);
                    result.put("parse", 1);
                    result.put("url", sniffUrl);
                    Map<String, String> h = new HashMap<>();
                    h.put("User-Agent", Http.UA);
                    h.put("Referer", HOST + "/");
                    result.put("header", new JSONObject(h));
                    return result.toString();
                }
            }

            // ⑥ 全失败
            System.out.println("❌ 全部失败");
            result.put("parse", 0);
            result.put("url", "");
        } catch (Exception e) {
            e.printStackTrace();
            try {
                result.put("parse", 0);
                result.put("url", "");
            } catch (Exception ignored) {}
        }
        return result.toString();
    }

    // ============================================================
    // 读 playerconfig.js，返回 [jxapi, artplayer]
    // ============================================================
    static String[] readPlayerConfig(String html) {
        String jxapi = "";
        String artplayer = "";
        try {
            // 找 playerconfig.js 引用
            String cfgUrl = group("src=\"([^\"]*playerconfig\\.js[^\"]*)\"", html, 1);
            if (cfgUrl.isEmpty()) cfgUrl = "/static/js/playerconfig.js";
            if (cfgUrl.startsWith("//")) cfgUrl = "https:" + cfgUrl;
            else if (cfgUrl.startsWith("/")) cfgUrl = HOST + cfgUrl;

            System.out.println("→ 抓 playerconfig.js: " + cfgUrl);
            String cfgJs = Http.get(cfgUrl, Http.htmlHeaders(HOST + "/"));
            if (cfgJs.isEmpty()) return new String[]{"", ""};

            // jxapi
            String jx = group("\"parse\"\\s*:\\s*\"([^\"]*jxapi[^\"]*)\"", cfgJs, 1);
            if (!jx.isEmpty()) {
                jx = jx.replace("\\/", "/");
                // &player → &from=player
                if (Pattern.compile("[?&]player(&|$)").matcher(jx).find()) {
                    jx = jx.replaceAll("([?&])player(&|$)", "$1from=player$2");
                } else {
                    jx += (jx.contains("?") ? "&" : "?") + "from=player";
                }
                jxapi = jx;
                System.out.println("  jxapi 模板: " + jxapi);
            }

            // artplayer
            String art = group("\"parse\"\\s*:\\s*\"([^\"]*artplayer\\.html[^\"]*)\"", cfgJs, 1);
            if (!art.isEmpty()) {
                art = art.replace("\\/", "/");
                if (!art.matches(".*[?&]url=$")) {
                    if (art.matches(".*[?&]$")) art += "url=";
                    else art += (art.contains("?") ? "&url=" : "?url=");
                }
                artplayer = art;
                System.out.println("  artplayer 模板: " + artplayer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String[]{jxapi, artplayer};
    }

    // ============================================================
    // 测试入口
    // ============================================================
    public static void main(String[] args) {
        System.out.println("=== 测试首页 ===");
        System.out.println(home().substring(0, Math.min(200, home().length())));

        System.out.println("\n=== 测试分类 ===");
        String cat = category("1", "1", "{}");
        System.out.println(cat.substring(0, Math.min(500, cat.length())));

        System.out.println("\n=== 测试详情 ===");
        String det = detail("28260");
        System.out.println(det.substring(0, Math.min(500, det.length())));

        System.out.println("\n=== 测试播放 ===");
        String p = play("1080zyk", "/vodplay/28260-1-1.html");
        System.out.println(p);

        System.out.println("\n=== 测试搜索 ===");
        String s = search("千香", "1");
        System.out.println(s.substring(0, Math.min(500, s.length())));
    }
}