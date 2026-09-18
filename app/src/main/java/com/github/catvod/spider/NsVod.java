package com.github.catvod.spider;

import android.content.Context;
import android.text.TextUtils;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Filter;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
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
    // 初始化
    // ============================================================
    @Override
    public void init(Context context, String extend) {
        SpiderDebug.log("NsVod init OK");
    }

    // ============================================================
    // 工具
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

    static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    static String group(String regex, String text, int group) {
        if (text == null) return "";
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(text);
        return m.find() ? (m.group(group) == null ? "" : m.group(group)) : "";
    }

    // 通用 GET（默认头）
    static String get(String url) {
        Map<String, String> h = new HashMap<>();
        h.put("User-Agent", UA_MB);
        h.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        h.put("Accept-Language", "zh-CN,zh;q=0.9");
        h.put("Referer", HOST + "/");
        try {
            return OkHttp.string(url, h);
        } catch (Exception e) {
            SpiderDebug.log("GET error: " + e.getMessage());
            return "";
        }
    }

    // 通用 GET（自定义头）
    static String get(String url, Map<String, String> headers) {
        try {
            return OkHttp.string(url, headers);
        } catch (Exception e) {
            return "";
        }
    }

    // 通用 POST（form 格式，Map 传参，LinkedHashMap 保序）
    static String post(String url, Map<String, String> params, Map<String, String> headers) {
        try {
            return OkHttp.post(url, params, headers).getBody();
        } catch (Exception e) {
            SpiderDebug.log("POST error: " + e.getMessage());
            return "";
        }
    }

    // ============================================================
    // 首页（含筛选）
    // ============================================================
    @Override
    public String homeContent(boolean filter) {
        try {
            List<Class> classes = new ArrayList<>();
            classes.add(new Class("1",  "电影"));
            classes.add(new Class("2",  "连续剧"));
            classes.add(new Class("3",  "综艺"));
            classes.add(new Class("4",  "动漫"));
            classes.add(new Class("41", "短剧"));
            classes.add(new Class("40", "纪录片"));
            classes.add(new Class("37", "Netflix"));

            // 年份
            List<Filter.Value> years = new ArrayList<>();
            years.add(new Filter.Value("全部", ""));
            for (int y = 2026; y >= 2014; y--) {
                years.add(new Filter.Value(String.valueOf(y), String.valueOf(y)));
            }

            // 地区
            List<Filter.Value> areas = buildValues(new String[][]{
                {"全部", ""}, {"大陆", "大陆"}, {"香港", "香港"}, {"台湾", "台湾"},
                {"美国", "美国"}, {"日本", "日本"}, {"韩国", "韩国"}, {"泰国", "泰国"},
                {"英国", "英国"}, {"法国", "法国"}, {"其他", "其他"}
            });

            // 语言
            List<Filter.Value> langs = buildValues(new String[][]{
                {"全部", ""}, {"国语", "国语"}, {"英语", "英语"}, {"粤语", "粤语"},
                {"韩语", "韩语"}, {"日语", "日语"}, {"其它", "其它"}
            });

            // 排序
            List<Filter.Value> orders = buildValues(new String[][]{
                {"最新", "time"}, {"最热", "hits"}, {"评分", "score"}
            });

            // 各分类类型
            List<Filter.Value> movieClass = buildValues(new String[][]{
                {"全部", ""}, {"动作", "动作"}, {"喜剧", "喜剧"}, {"爱情", "爱情"},
                {"科幻", "科幻"}, {"恐怖", "恐怖"}, {"剧情", "剧情"}, {"战争", "战争"},
                {"犯罪", "犯罪"}, {"动画", "动画"}, {"奇幻", "奇幻"}, {"武侠", "武侠"},
                {"悬疑", "悬疑"}, {"惊悚", "惊悚"}, {"古装", "古装"}
            });
            List<Filter.Value> tvClass = buildValues(new String[][]{
                {"全部", ""}, {"古装", "古装"}, {"战争", "战争"}, {"喜剧", "喜剧"},
                {"家庭", "家庭"}, {"犯罪", "犯罪"}, {"动作", "动作"}, {"奇幻", "奇幻"},
                {"剧情", "剧情"}, {"历史", "历史"}, {"网剧", "网剧"}
            });
            List<Filter.Value> varietyClass = buildValues(new String[][]{
                {"全部", ""}, {"选秀", "选秀"}, {"情感", "情感"}, {"访谈", "访谈"},
                {"旅游", "旅游"}, {"音乐", "音乐"}, {"美食", "美食"}, {"生活", "生活"}
            });
            List<Filter.Value> animeClass = buildValues(new String[][]{
                {"全部", ""}, {"情感", "情感"}, {"科幻", "科幻"}, {"热血", "热血"},
                {"搞笑", "搞笑"}, {"冒险", "冒险"}, {"动作", "动作"}, {"亲子", "亲子"},
                {"励志", "励志"}
            });

            LinkedHashMap<String, List<Filter>> filters = new LinkedHashMap<>();

            List<Filter> f1 = new ArrayList<>();
            f1.add(new Filter("class", "类型", movieClass));
            f1.add(new Filter("area",  "地区", areas));
            f1.add(new Filter("year",  "年份", years));
            f1.add(new Filter("lang",  "语言", langs));
            f1.add(new Filter("by",    "排序", orders));
            filters.put("1", f1);

            List<Filter> f2 = new ArrayList<>();
            f2.add(new Filter("class", "类型", tvClass));
            f2.add(new Filter("area",  "地区", areas));
            f2.add(new Filter("year",  "年份", years));
            f2.add(new Filter("lang",  "语言", langs));
            f2.add(new Filter("by",    "排序", orders));
            filters.put("2", f2);

            List<Filter> f3 = new ArrayList<>();
            f3.add(new Filter("class", "类型", varietyClass));
            f3.add(new Filter("area",  "地区", areas));
            f3.add(new Filter("year",  "年份", years));
            f3.add(new Filter("by",    "排序", orders));
            filters.put("3", f3);

            List<Filter> f4 = new ArrayList<>();
            f4.add(new Filter("class", "类型", animeClass));
            f4.add(new Filter("area",  "地区", areas));
            f4.add(new Filter("year",  "年份", years));
            f4.add(new Filter("by",    "排序", orders));
            filters.put("4", f4);

            return Result.string(classes, filters);
        } catch (Exception e) {
            SpiderDebug.log("homeContent error: " + e.getMessage());
            return "";
        }
    }

    private List<Filter.Value> buildValues(String[][] data) {
        List<Filter.Value> list = new ArrayList<>();
        for (String[] kv : data) {
            list.add(new Filter.Value(kv[0], kv[1]));
        }
        return list;
    }

    // ============================================================
    // 首页推荐
    // ============================================================
    @Override
    public String homeVideoContent() {
        try {
            String html = get(HOST + "/");
            List<Vod> list = parseHtmlList(html);
            if (list.size() > 30) list = list.subList(0, 30);
            return Result.string(list);
        } catch (Exception e) {
            return "";
        }
    }

    // ============================================================
    // 分类（POST + 签名）
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

            // ★ LinkedHashMap 保序，签名依赖顺序
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

            String resp = post(HOST + "/index.php/api/vod", params, headers);
            SpiderDebug.log("category resp: " + (resp == null ? "null" : resp.substring(0, Math.min(200, resp.length()))));

            JSONObject data = new JSONObject(resp == null || resp.isEmpty() ? "{}" : resp);
            JSONArray arr = data.optJSONArray("list");
            List<Vod> list = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject it = arr.getJSONObject(i);
                    list.add(new Vod(
                        String.valueOf(it.opt("vod_id")),
                        it.optString("vod_name"),
                        fixUrl(it.optString("vod_pic")),
                        it.optString("vod_remarks")
                    ));
                }
            }
            int pagecount = data.optInt("pagecount", 1);
            int limit = data.optInt("limit", 40);
            int total = data.optInt("total", pagecount * limit);

            return Result.get().vod(list)
                    .page(page, pagecount, limit, total).string();
        } catch (Exception e) {
            SpiderDebug.log("categoryContent error: " + e.getMessage());
            return "";
        }
    }

    // ============================================================
    // HTML 列表解析
    // ============================================================
    private List<Vod> parseHtmlList(String html) {
        List<Vod> list = new ArrayList<>();
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

                list.add(new Vod(id, name, pic, remark));
            }
        } catch (Exception e) {
            SpiderDebug.log("parseHtmlList error: " + e.getMessage());
        }
        return list;
    }

    // ============================================================
    // 详情
    // ============================================================
    @Override
    public String detailContent(List<String> ids) {
        try {
            String id = ids.get(0);
            String html = get(HOST + "/voddetail/" + id + ".html");
            if (TextUtils.isEmpty(html)) return "";

            Vod vod = new Vod();
            vod.setVodId(id);
            vod.setVodName(decodeHtml(group(
                "<h3 class=\"slide-info-title[^\"]*\"[^>]*>([^<]+)</h3>", html, 1).trim()));
            vod.setVodPic(fixUrl(group(
                "class=\"lazy lazy1 mask-1[^\"]*\"[^>]*(?:data-src|src)=\"([^\"]+)\"", html, 1)));
            vod.setVodContent(stripTags(group(
                "<div id=\"height_limit\"[^>]*class=\"text[^\"]*\"[^>]*>([\\s\\S]*?)</div>", html, 1)));

            String[] pl = parsePlaylist(html);
            vod.setVodPlayFrom(pl[0]);
            vod.setVodPlayUrl(pl[1]);

            return Result.string(vod);
        } catch (Exception e) {
            SpiderDebug.log("detailContent error: " + e.getMessage());
            return "";
        }
    }

    // 返回 [from, url]
    private String[] parsePlaylist(String html) {
        StringBuilder fromSb = new StringBuilder();
        StringBuilder urlSb = new StringBuilder();

        // 线路名
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

        // 剧集
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
    // 搜索
    // ============================================================
    @Override
    public String searchContent(String key, boolean quick) {
        try {
            String url = HOST + "/vodsearch/-------------.html?wd=" + urlEncode(key);
            String html = get(url);
            return Result.string(parseHtmlList(html));
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
            return Result.string(parseHtmlList(html));
        } catch (Exception e) {
            return "";
        }
    }

    // ============================================================
    // 播放（直链 → jxapi → artplayer 嗅探）
    // ============================================================
    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        try {
            // ① 入参直链
            if (id != null && id.matches(".*\\.(m3u8|mp4|flv|mkv|webm|ts)(\\?.*)?$")) {
                return Result.get().url(id).string();
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
                return Result.get().url(realUrl).string();
            }

            // ⑤ 第三方源 → 读 playerconfig.js，抠 jxapi / artplayer
            if (!realUrl.isEmpty()) {
                String[] cfg = readPlayerConfig(html);
                String jxapi = cfg[0];
                String artplayer = cfg[1];

                // 5.1 先试 jxapi
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
                                return Result.get().url(videoUrl).string();
                            }
                        } catch (Exception e) {
                            SpiderDebug.log("jxapi JSON error: " + e.getMessage());
                        }
                    }
                }

                // 5.2 jxapi 不行 → artplayer 嗅探
                if (!artplayer.isEmpty()) {
                    String sniffUrl = artplayer + urlEncode(realUrl);
                    SpiderDebug.log("⚠️ artplayer 嗅探: " + sniffUrl);
                    return Result.get().parse().url(sniffUrl).string();
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

    // 返回 [jxapi, artplayer]
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

            // jxapi
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

            // artplayer
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

    // ============================================================
    // 销毁
    // ============================================================
    @Override
    public void destroy() {
        SpiderDebug.log("NsVod destroy");
    }
}