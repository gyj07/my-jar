package com.github.catvod.spider;

import android.content.Context;
import android.text.TextUtils;

import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NsVod extends Spider {

    private static final String HOST = "https://nsvod.cc";
    private static final String SALT = "DCC147D11943AF75";
    private static final String UA_MB = "Mozilla/5.0 (Linux; Android 13; SM-G9910) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    // ============================================================
    // init（不发网络请求）
    // ============================================================
    @Override
    public void init(Context context, String extend) {
        SpiderDebug.log("NsVod init OK");
    }

    // ============================================================
    // 工具
    // ============================================================
    private String md5(String s) {
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

    private String fixUrl(String u) {
        if (u == null || u.isEmpty()) return "";
        u = u.replace("\\/", "/").trim();
        if (u.startsWith("//")) return "https:" + u;
        if (u.startsWith("http")) return u;
        if (u.startsWith("/")) return HOST + u;
        return HOST + "/" + u;
    }

    private String stripTags(String s) {
        if (s == null) return "";
        return s.replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String decodeHtml(String s) {
        if (s == null) return "";
        return s.replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&nbsp;", " ");
    }

    private String urlEncode(String s) {
        try { return URLEncoder.encode(s == null ? "" : s, "UTF-8"); }
        catch (Exception e) { return ""; }
    }

    private String group(String regex, String text, int g) {
        if (text == null) return "";
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(text);
        return m.find() ? (m.group(g) == null ? "" : m.group(g)) : "";
    }

    private String get(String url) {
        Map<String, String> h = new HashMap<>();
        h.put("User-Agent", UA_MB);
        h.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        h.put("Accept-Language", "zh-CN,zh;q=0.9");
        h.put("Referer", HOST + "/");
        try { return OkHttp.string(url, h); }
        catch (Exception e) { return ""; }
    }

    private String get(String url, Map<String, String> headers) {
        try { return OkHttp.string(url, headers); }
        catch (Exception e) { return ""; }
    }

    // ============================================================
    // ★ homeContent（手动拼 JSON，和 DuShe 一样）
    // ============================================================
    @Override
    public String homeContent(boolean filter) {
        try {
            JSONObject result = new JSONObject();
            JSONArray classes = new JSONArray();

            String[][] cfg = {
                {"1",  "电影"}, {"2",  "连续剧"}, {"3",  "综艺"},
                {"4",  "动漫"}, {"41", "短剧"}, {"40", "纪录片"}, {"37", "Netflix"}
            };
            for (String[] c : cfg) {
                JSONObject o = new JSONObject();
                o.put("type_id", c[0]);
                o.put("type_name", c[1]);
                classes.put(o);
            }
            result.put("class", classes);

            // ★ filters 手动拼
            result.put("filters", buildFiltersJson());

            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log("homeContent error: " + e.getMessage());
            return "";
        }
    }

    // ★ filters 用 JSONObject 拼
    private JSONObject buildFiltersJson() throws Exception {
        JSONObject filters = new JSONObject();

        // 年份
        JSONArray years = new JSONArray();
        JSONObject y0 = new JSONObject(); y0.put("n", "全部"); y0.put("v", ""); years.put(y0);
        for (int y = 2026; y >= 2014; y--) {
            JSONObject o = new JSONObject();
            o.put("n", String.valueOf(y));
            o.put("v", String.valueOf(y));
            years.put(o);
        }

        JSONArray areas = vals(new String[][]{
            {"全部", ""}, {"大陆", "大陆"}, {"香港", "香港"}, {"台湾", "台湾"},
            {"美国", "美国"}, {"日本", "日本"}, {"韩国", "韩国"}, {"泰国", "泰国"},
            {"英国", "英国"}, {"法国", "法国"}, {"其他", "其他"}
        });
        JSONArray langs = vals(new String[][]{
            {"全部", ""}, {"国语", "国语"}, {"英语", "英语"}, {"粤语", "粤语"},
            {"韩语", "韩语"}, {"日语", "日语"}, {"其它", "其它"}
        });
        JSONArray orders = vals(new String[][]{
            {"最新", "time"}, {"最热", "hits"}, {"评分", "score"}
        });

        JSONArray movieClass = vals(new String[][]{
            {"全部", ""}, {"动作", "动作"}, {"喜剧", "喜剧"}, {"爱情", "爱情"},
            {"科幻", "科幻"}, {"恐怖", "恐怖"}, {"剧情", "剧情"}, {"战争", "战争"},
            {"犯罪", "犯罪"}, {"动画", "动画"}, {"奇幻", "奇幻"}, {"武侠", "武侠"},
            {"悬疑", "悬疑"}, {"惊悚", "惊悚"}, {"古装", "古装"}
        });
        JSONArray tvClass = vals(new String[][]{
            {"全部", ""}, {"古装", "古装"}, {"战争", "战争"}, {"喜剧", "喜剧"},
            {"家庭", "家庭"}, {"犯罪", "犯罪"}, {"动作", "动作"}, {"奇幻", "奇幻"},
            {"剧情", "剧情"}, {"历史", "历史"}, {"网剧", "网剧"}
        });
        JSONArray varietyClass = vals(new String[][]{
            {"全部", ""}, {"选秀", "选秀"}, {"情感", "情感"}, {"访谈", "访谈"},
            {"旅游", "旅游"}, {"音乐", "音乐"}, {"美食", "美食"}, {"生活", "生活"}
        });
        JSONArray animeClass = vals(new String[][]{
            {"全部", ""}, {"情感", "情感"}, {"科幻", "科幻"}, {"热血", "热血"},
            {"搞笑", "搞笑"}, {"冒险", "冒险"}, {"动作", "动作"}, {"亲子", "亲子"},
            {"励志", "励志"}
        });

        // 1 电影
        JSONArray f1 = new JSONArray();
        f1.put(fgroup("class", "类型", movieClass));
        f1.put(fgroup("area", "地区", areas));
        f1.put(fgroup("year", "年份", years));
        f1.put(fgroup("lang", "语言", langs));
        f1.put(fgroup("by", "排序", orders));
        filters.put("1", f1);

        // 2 连续剧
        JSONArray f2 = new JSONArray();
        f2.put(fgroup("class", "类型", tvClass));
        f2.put(fgroup("area", "地区", areas));
        f2.put(fgroup("year", "年份", years));
        f2.put(fgroup("lang", "语言", langs));
        f2.put(fgroup("by", "排序", orders));
        filters.put("2", f2);

        // 3 综艺
        JSONArray f3 = new JSONArray();
        f3.put(fgroup("class", "类型", varietyClass));
        f3.put(fgroup("area", "地区", areas));
        f3.put(fgroup("year", "年份", years));
        f3.put(fgroup("by", "排序", orders));
        filters.put("3", f3);

        // 4 动漫
        JSONArray f4 = new JSONArray();
        f4.put(fgroup("class", "类型", animeClass));
        f4.put(fgroup("area", "地区", areas));
        f4.put(fgroup("year", "年份", years));
        f4.put(fgroup("by", "排序", orders));
        filters.put("4", f4);

        return filters;
    }

    private JSONArray vals(String[][] data) throws Exception {
        JSONArray arr = new JSONArray();
        for (String[] kv : data) {
            JSONObject o = new JSONObject();
            o.put("n", kv[0]);
            o.put("v", kv[1]);
            arr.put(o);
        }
        return arr;
    }

    private JSONObject fgroup(String key, String name, JSONArray values) throws Exception {
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
            String html = get(HOST + "/");
            List<JSONObject> list = parseHtmlList(html);
            if (list.size() > 30) list = list.subList(0, 30);

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
    // categoryContent（手动拼 JSON）
    // ============================================================
    @Override
    public String categoryContent(String tid, String pg, boolean filter,
                                  HashMap<String, String> extend) {
        try {
            int page = 1;
            try { page = Integer.parseInt(pg); } catch (Exception ignored) {}

            String cls  = extend != null && extend.get("class") != null ? extend.get("class") : "";
            String area = extend != null && extend.get("area")  != null ? extend.get("area")  : "";
            String year = extend != null && extend.get("year")  != null ? extend.get("year")  : "";
            String lang = extend != null && extend.get("lang")  != null ? extend.get("lang")  : "";
            String by   = extend != null && extend.get("by")    != null ? extend.get("by")    : "";

            if ("全部".equals(cls) || "不限".equals(cls)) cls = "";
            if ("全部".equals(area)) area = "";
            if ("全部".equals(year)) year = "";

            long t = System.currentTimeMillis() / 1000;
            String key = md5("DS" + t + SALT);

            LinkedHashMap<String, String> params = new LinkedHashMap<>();
            params.put("type", tid);
            params.put("class", cls);
            params.put("area", area);
            params.put("lang", lang);
            if (!year.isEmpty()) params.put("year", year);
            params.put("version", "");
            params.put("state", "");
            params.put("letter", "");
            if (!by.isEmpty()) params.put("order", by);
            params.put("page", String.valueOf(page));
            params.put("time", String.valueOf(t));
            params.put("key", key);

            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", UA_MB);
            headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
            headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            headers.put("X-Requested-With", "XMLHttpRequest");
            headers.put("Origin", HOST);
            headers.put("Referer", HOST + "/vodshow/" + tid + "-----------.html");

            String resp = "";
            try {
                resp = OkHttp.post(HOST + "/index.php/api/vod", params, headers).getBody();
            } catch (Exception e) {
                SpiderDebug.log("POST error: " + e.getMessage());
            }
            if (resp == null) resp = "";
            SpiderDebug.log("category resp: " + resp.substring(0, Math.min(200, resp.length())));

            JSONObject data = new JSONObject(resp.isEmpty() ? "{}" : resp);
            JSONArray arr = data.optJSONArray("list");
            JSONArray listArr = new JSONArray();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject it = arr.getJSONObject(i);
                    JSONObject o = new JSONObject();
                    o.put("vod_id", String.valueOf(it.opt("vod_id")));
                    o.put("vod_name", it.optString("vod_name"));
                    o.put("vod_pic", fixUrl(it.optString("vod_pic")));
                    o.put("vod_remarks", it.optString("vod_remarks"));
                    listArr.put(o);
                }
            }
            int pagecount = data.optInt("pagecount", 1);
            int limit = data.optInt("limit", 40);
            int total = data.optInt("total", pagecount * limit);

            JSONObject result = new JSONObject();
            result.put("page", page);
            result.put("pagecount", pagecount);
            result.put("limit", limit);
            result.put("total", total);
            result.put("list", listArr);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log("categoryContent error: " + e.getMessage());
            return "";
        }
    }

    // ============================================================
    // parseHtmlList → 返回 List<JSONObject>
    // ============================================================
    private List<JSONObject> parseHtmlList(String html) {
        List<JSONObject> list = new ArrayList<>();
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
                list.add(o);
            }
        } catch (Exception e) {
            SpiderDebug.log("parseHtmlList error: " + e.getMessage());
        }
        return list;
    }

    // ============================================================
    // detailContent（手动拼 JSON）
    // ============================================================
    @Override
    public String detailContent(List<String> ids) {
        try {
            String id = ids.get(0);
            String html = get(HOST + "/voddetail/" + id + ".html");
            if (TextUtils.isEmpty(html)) {
                JSONObject r = new JSONObject();
                r.put("list", new JSONArray());
                return r.toString();
            }

            JSONObject vod = new JSONObject();
            vod.put("vod_id", id);
            vod.put("vod_name", decodeHtml(group(
                "<h3 class=\"slide-info-title[^\"]*\"[^>]*>([^<]+)</h3>", html, 1).trim()));
            vod.put("vod_pic", fixUrl(group(
                "class=\"lazy lazy1 mask-1[^\"]*\"[^>]*(?:data-src|src)=\"([^\"]+)\"", html, 1)));
            vod.put("vod_content", stripTags(group(
                "<div id=\"height_limit\"[^>]*class=\"text[^\"]*\"[^>]*>([\\s\\S]*?)</div>", html, 1)));

            String[] pl = parsePlaylist(html);
            vod.put("vod_play_from", pl[0]);
            vod.put("vod_play_url", pl[1]);

            JSONArray list = new JSONArray();
            list.put(vod);
            JSONObject result = new JSONObject();
            result.put("list", list);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log("detailContent error: " + e.getMessage());
            return "";
        }
    }

    // 返回 [from, url]
    private String[] parsePlaylist(String html) {
        StringBuilder fromSb = new StringBuilder();
        StringBuilder urlSb = new StringBuilder();

        List<String> names = new ArrayList<>();
        Matcher tabM = Pattern.compile(
            "<a[^>]*class=\"[^\"]*swiper-slide[^\"]*\"[^>]*>([\\s\\S]*?)</a>"
        ).matcher(html);
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

        Matcher boxM = Pattern.compile(
            "<div class=\"anthology-list-box[^\"]*\"[^>]*>([\\s\\S]*?)</ul>\\s*</div>\\s*</div>"
        ).matcher(html);

        int idx = 0;
        while (boxM.find()) {
            String box = boxM.group(1);
            StringBuilder epSb = new StringBuilder();
            Matcher aM = Pattern.compile(
                "<a[^>]*href=\"([^\"]+)\"[^>]*>([^<]+)</a>"
            ).matcher(box);
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
    // searchContent
    // ============================================================
    @Override
    public String searchContent(String key, boolean quick) {
        try {
            String url = HOST + "/vodsearch/-------------.html?wd=" + urlEncode(key);
            String html = get(url);
            List<JSONObject> list = parseHtmlList(html);

            JSONArray arr = new JSONArray();
            for (JSONObject o : list) arr.put(o);
            JSONObject result = new JSONObject();
            result.put("list", arr);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log("searchContent error: " + e.getMessage());
            return "";
        }
    }

    @Override
    public String searchContent(String key, boolean quick, String pg) {
        try {
            int page = 1;
            try { page = Integer.parseInt(pg); } catch (Exception ignored) {}
            String url = HOST + "/vodsearch/-------------.html?wd=" + urlEncode(key);
            if (page > 1) url += "&page=" + page;
            String html = get(url);
            List<JSONObject> list = parseHtmlList(html);

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
    // playerContent（手动拼 JSON）
    // ============================================================
    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        try {
            // ① 入参直链
            if (id != null && id.matches(".*\\.(m3u8|mp4|flv|mkv|webm|ts)(\\?.*)?$")) {
                JSONObject r = new JSONObject();
                r.put("parse", 0);
                r.put("url", id);
                return r.toString();
            }

            // ② 抓播放页
            String pageUrl = id.startsWith("http") ? id : HOST + id;
            String html = get(pageUrl);
            if (html == null || !html.contains("player_aaaa")) {
                try { Thread.sleep(400); } catch (InterruptedException ignored) {}
                html = get(pageUrl);
            }
            if (TextUtils.isEmpty(html)) return "";

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
                    SpiderDebug.log("player_aaaa parse error: " + e.getMessage());
                }
            }

            SpiderDebug.log("player.url=" + realUrl + " | from=" + from);

            // ④ m3u8/mp4 → 直连
            if (!realUrl.isEmpty() && realUrl.matches(".*\\.(m3u8|mp4|flv)(\\?.*)?$")) {
                JSONObject r = new JSONObject();
                r.put("parse", 0);
                r.put("url", realUrl);
                return r.toString();
            }

            // ⑤ 第三方源 → 读 playerconfig.js
            if (!realUrl.isEmpty()) {
                String[] cfg = readPlayerConfig(html);
                String jxapi = cfg[0];
                String artplayer = cfg[1];

                // 5.1 jxapi
                if (!jxapi.isEmpty()) {
                    String apiUrl = jxapi + urlEncode(realUrl);
                    SpiderDebug.log("→ jxapi: " + apiUrl);

                    Map<String, String> h = new HashMap<>();
                    h.put("User-Agent", UA_MB);
                    h.put("Accept", "application/json,*/*");
                    h.put("Referer", HOST + "/");
                    String resp = get(apiUrl, h);

                    if (!TextUtils.isEmpty(resp)) {
                        try {
                            JSONObject j = new JSONObject(resp);
                            if (j.optInt("code") == 200 && j.has("url")) {
                                String videoUrl = j.optString("url").replace("\\/", "/");
                                SpiderDebug.log("✅ jxapi: " + videoUrl);
                                JSONObject r = new JSONObject();
                                r.put("parse", 0);
                                r.put("url", videoUrl);
                                return r.toString();
                            }
                        } catch (Exception e) {
                            SpiderDebug.log("jxapi JSON error: " + e.getMessage());
                        }
                    }
                }

                // 5.2 artplayer 嗅探
                if (!artplayer.isEmpty()) {
                    String sniffUrl = artplayer + urlEncode(realUrl);
                    SpiderDebug.log("⚠️ artplayer 嗅探: " + sniffUrl);
                    JSONObject r = new JSONObject();
                    r.put("parse", 1);
                    r.put("url", sniffUrl);
                    return r.toString();
                }
            }

            // ⑥ 全失败
            SpiderDebug.log("❌ 全部失败");
            return "";
        } catch (Exception e) {
            SpiderDebug.log("playerContent error: " + e.getMessage());
            return "";
        }
    }

    private String[] readPlayerConfig(String html) {
        String jxapi = "";
        String artplayer = "";
        try {
            String cfgUrl = group("src=\"([^\"]*playerconfig\\.js[^\"]*)\"", html, 1);
            if (cfgUrl.isEmpty()) cfgUrl = "/static/js/playerconfig.js";
            if (cfgUrl.startsWith("//")) cfgUrl = "https:" + cfgUrl;
            else if (cfgUrl.startsWith("/")) cfgUrl = HOST + cfgUrl;

            SpiderDebug.log("→ playerconfig.js: " + cfgUrl);
            String cfgJs = get(cfgUrl);
            if (TextUtils.isEmpty(cfgJs)) return new String[]{"", ""};

            String jx = group("\"parse\"\\s*:\\s*\"([^\"]*jxapi[^\"]*)\"", cfgJs, 1);
            if (!jx.isEmpty()) {
                jx = jx.replace("\\/", "/");
                if (Pattern.compile("[?&]player(&|$)").matcher(jx).find()) {
                    jx = jx.replaceAll("([?&])player(&|$)", "$1from=player$2");
                } else {
                    jx += (jx.contains("?") ? "&" : "?") + "from=player";
                }
                jxapi = jx;
                SpiderDebug.log("  jxapi: " + jxapi);
            }

            String art = group("\"parse\"\\s*:\\s*\"([^\"]*artplayer\\.html[^\"]*)\"", cfgJs, 1);
            if (!art.isEmpty()) {
                art = art.replace("\\/", "/");
                if (!art.matches(".*[?&]url=$")) {
                    if (art.matches(".*[?&]$")) art += "url=";
                    else art += (art.contains("?") ? "&url=" : "?url=");
                }
                artplayer = art;
                SpiderDebug.log("  artplayer: " + artplayer);
            }
        } catch (Exception e) {
            SpiderDebug.log("readPlayerConfig error: " + e.getMessage());
        }
        return new String[]{jxapi, artplayer};
    }

    @Override
    public void destroy() {
        SpiderDebug.log("NsVod destroy");
    }
}