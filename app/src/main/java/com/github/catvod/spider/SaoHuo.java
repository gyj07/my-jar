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
import com.github.catvod.utils.Util;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SaoHuo extends Spider {

    private static final String JS_CODE =
        "var host = 'https://shdy2.com';\n" +
        "var cookie = '';\n" +
        "var UA = 'Mozilla/5.0 (Linux; Android 9; ALN-AL00 Build/PQ3B.190801.05281406; wv) AppleWebKit/537.36';\n" +

        "function fixUrl(url) {\n" +
        "    if (!url) return '';\n" +
        "    url = url.trim();\n" +
        "    if (url.indexOf('//') === 0) return 'https:' + url;\n" +
        "    if (url.indexOf('http') === 0) return url;\n" +
        "    if (url.indexOf('/') === 0) return host + url;\n" +
        "    return host + '/' + url;\n" +
        "}\n" +

        // ---------- 列表解析（正则） ----------
        "function extractVods(html) {\n" +
        "    var list = [];\n" +
        "    if (!html) return list;\n" +
        // 匹配 <a href="xxx" title="yyy"> ... <img ... src/data-original="zzz"> ... <span class="...note...">rrr</span>
        "    var re = /<a[^>]*href=\"([^\"]+)\"[^>]*title=\"([^\"]+)\"[^>]*>[\\s\\S]*?<img[^>]*(?:data-original|src)=\"([^\"]+)\"[\\s\\S]*?(?:<[^>]*class=\"[^\"]*(?:v_note|continu|pic-text|pic-tag|module-item-note)[^\"]*\"[^>]*>([^<]*)<\\/)?/g;\n" +
        "    var m;\n" +
        "    var seen = {};\n" +
        "    while ((m = re.exec(html)) !== null) {\n" +
        "        var href = m[1], title = m[2], pic = m[3], remarks = m[4] || '';\n" +
        "        if (!href || !title) continue;\n" +
        "        var vid = fixUrl(href);\n" +
        "        if (seen[vid]) continue;\n" +
        "        seen[vid] = 1;\n" +
        "        list.push({ vod_id: vid, vod_name: title.trim(), vod_pic: fixUrl(pic), vod_remarks: remarks.trim() });\n" +
        "    }\n" +
        "    return list;\n" +
        "}\n" +

        "function extractPageCount(html, curPage) {\n" +
        "    if (!html) return curPage;\n" +
        "    var maxPage = 0;\n" +
        "    var re = /<a[^>]*href=\"[^\"]*[-_](\\d+)\\.html\"[^>]*>/g;\n" +
        "    var m;\n" +
        "    while ((m = re.exec(html)) !== null) {\n" +
        "        var p = parseInt(m[1]);\n" +
        "        if (p > maxPage) maxPage = p;\n" +
        "    }\n" +
        "    return maxPage > 0 ? maxPage : curPage + 1;\n" +
        "}\n" +

        // ---------- 详情 ----------
        "function parseDetail(html, id) {\n" +
        "    var info = { vod_id: id, vod_name: '', vod_pic: '', vod_content: '', vod_play_from: '', vod_play_url: '' };\n" +
        "    var m = html.match(/<h1[^>]*class=\"[^\"]*(?:v_title|title)[^\"]*\"[^>]*>([^<]+)<\\/h1>/);\n" +
        "    if (!m) m = html.match(/<h1[^>]*>([^<]+)<\\/h1>/);\n" +
        "    if (m) info.vod_name = m[1].trim().replace(/\\s*-.*$/, '');\n" +

        "    var picM = html.match(/<img[^>]*class=\"lazyload\"[^>]*data-original=\"([^\"]+)\"/);\n" +
        "    if (!picM) picM = html.match(/<img[^>]*class=\"lazyload\"[^>]*src=\"([^\"]+)\"/);\n" +
        "    if (picM) info.vod_pic = fixUrl(picM[1]);\n" +

        "    var descM = html.match(/<div[^>]*class=\"[^\"]*(?:intro|des)[^\"]*\"[^>]*>([\\s\\S]*?)<\\/div>/);\n" +
        "    if (descM) info.vod_content = descM[1].replace(/<[^>]+>/g, '').trim();\n" +

        // 播放列表：从 #play_link 或 .play_link 里的 li > a 抠
        "    var fromNames = [];\n" +
        "    var fromUrls = [];\n" +
        // 线路名：<div class="play_from">...<li>名称</li>...
        "    var fromRe = /<div[^>]*class=\"play_from\"[^>]*>([\\s\\S]*?)<\\/div>/g;\n" +
        "    var fromM;\n" +
        "    while ((fromM = fromRe.exec(html)) !== null) {\n" +
        "        var liRe = /<li[^>]*>([^<]+)<\\/li>/g;\n" +
        "        var liM;\n" +
        "        while ((liM = liRe.exec(fromM[1])) !== null) {\n" +
        "            var n = liM[1].trim();\n" +
        "            if (n) fromNames.push(n);\n" +
        "        }\n" +
        "    }\n" +
        // 播放列表：<ul id="play_link">...<li>...<a href="xxx">第1集</a>...
        "    var linkRe = /<ul[^>]*id=\"play_link\"[^>]*>([\\s\\S]*?)<\\/ul>/g;\n" +
        "    var linkM;\n" +
        "    while ((linkM = linkRe.exec(html)) !== null) {\n" +
        "        var eps = [];\n" +
        "        var epRe = /<a[^>]*href=\"([^\"]+)\"[^>]*>([^<]+)<\\/a>/g;\n" +
        "        var epM;\n" +
        "        while ((epM = epRe.exec(linkM[1])) !== null) {\n" +
        "            var href = epM[1].trim();\n" +
        "            var name = epM[2].trim();\n" +
        "            if (href && name) eps.push(name + '$' + fixUrl(href));\n" +
        "        }\n" +
        "        if (eps.length > 0) fromUrls.push(eps.join('#'));\n" +
        "    }\n" +

        "    if (fromUrls.length > 0) {\n" +
        "        var playFrom = [];\n" +
        "        for (var i = 0; i < fromUrls.length; i++) {\n" +
        "            playFrom.push(i < fromNames.length ? fromNames[i] : ('线路' + (i + 1)));\n" +
        "        }\n" +
        "        info.vod_play_from = playFrom.join('$$$');\n" +
        "        info.vod_play_url = fromUrls.join('$$$');\n" +
        "    }\n" +
        "    return info;\n" +
        "}\n" +

        // ---------- 播放 ----------
        "function extractM3u8(text) {\n" +
        "    if (!text) return '';\n" +
        "    var m = text.match(/\"url\"\\s*:\\s*\"(https?:[^\"]+?\\.m3u8[^\"]*)\"/i);\n" +
        "    if (m) return m[1].replace(/\\\\u0026/g, '&').replace(/\\\\\\//g, '/').replace(/&amp;/g, '&');\n" +
        "    m = text.match(/(https?:[^\"'\\s\\\\<>]+?\\.m3u8[^\"'\\s\\\\<>]*)/i);\n" +
        "    if (m) return m[1].replace(/\\\\u0026/g, '&').replace(/\\\\\\//g, '/').replace(/&amp;/g, '&');\n" +
        "    return '';\n" +
        "}\n" +

        "function extractHhUrl(html) {\n" +
        "    if (!html) return '';\n" +
        "    var m = html.match(/<iframe[^>]+src=[\"'](https?:\\/\\/[^\"']+[?&]url=[A-Za-z0-9]+)[\"']/i);\n" +
        "    if (m) return m[1].replace(/&amp;/g, '&');\n" +
        "    m = html.match(/(https?:\\/\\/[^\"'\\s<>]+[?&]url=[A-Za-z0-9]+)/i);\n" +
        "    if (m) return m[1].replace(/&amp;/g, '&');\n" +
        "    return '';\n" +
        "}\n" +

        "function parsePlay(html, id) {\n" +
        "    if (!html) return { parse: 1, url: id };\n" +
        "    var direct = extractM3u8(html);\n" +
        "    if (direct) return { parse: 0, url: direct };\n" +
        "    var hh = extractHhUrl(html);\n" +
        "    if (hh) return { parse: 1, url: hh };\n" +
        "    return { parse: 1, url: id };\n" +
        "}\n" +

        // ---------- TVBox 接口 ----------
        "function init(cfg) { return true; }\n" +

        "function home(filter) {\n" +
        "    var classes = [\n" +
        "        { type_id: '1', type_name: '电影' },\n" +
        "        { type_id: '2', type_name: '电视剧' },\n" +
        "        { type_id: '4', type_name: '动漫' }\n" +
        "    ];\n" +
        "    var filters = {\n" +
        "        '1': [{ key: 'cateId', name: '类型', value: [\n" +
        "            { n: '全部', v: '1' }, { n: '喜剧', v: '6' }, { n: '爱情', v: '7' },\n" +
        "            { n: '恐怖', v: '8' }, { n: '动作', v: '9' }, { n: '科幻', v: '10' },\n" +
        "            { n: '战争', v: '11' }, { n: '犯罪', v: '12' }, { n: '动画', v: '13' },\n" +
        "            { n: '奇幻', v: '14' }, { n: '剧情', v: '15' }, { n: '冒险', v: '16' },\n" +
        "            { n: '悬疑', v: '17' }, { n: '惊悚', v: '18' }, { n: '其他', v: '20' }\n" +
        "        ]}],\n" +
        "        '2': [{ key: 'cateId', name: '类型', value: [\n" +
        "            { n: '全部', v: '2' }, { n: '国产剧', v: '20' }, { n: 'TVB', v: '21' },\n" +
        "            { n: '韩剧', v: '22' }, { n: '美剧', v: '23' }, { n: '日剧', v: '24' },\n" +
        "            { n: '英剧', v: '25' }, { n: '台剧', v: '26' }, { n: '其他', v: '27' }\n" +
        "        ]}],\n" +
        "        '4': [{ key: 'cateId', name: '类型', value: [\n" +
        "            { n: '全部', v: '4' }, { n: '搞笑', v: '38' }, { n: '恋爱', v: '39' },\n" +
        "            { n: '热血', v: '40' }, { n: '格斗', v: '41' }, { n: '美少女', v: '42' },\n" +
        "            { n: '魔法', v: '43' }, { n: '机战', v: '44' }, { n: '校园', v: '45' },\n" +
        "            { n: '亲子', v: '46' }, { n: '童话', v: '47' }, { n: '冒险', v: '48' },\n" +
        "            { n: '真人', v: '49' }, { n: 'LOLI', v: '50' }, { n: '其他', v: '51' }\n" +
        "        ]}]\n" +
        "    };\n" +
        "    return JSON.stringify({ class: classes, filters: filters });\n" +
        "}\n" +

        "function homeVod() {\n" +
        "    var html = fetchHtml(host);\n" +
        "    var list = extractVods(html);\n" +
        "    if (list.length > 6) list = list.slice(0, 6);\n" +
        "    return JSON.stringify({ list: list });\n" +
        "}\n" +

        "function category(tid, pg, filter, extend) {\n" +
        "    var cateId = (extend && extend.cateId) ? extend.cateId : tid;\n" +
        "    var page = parseInt(pg) || 1;\n" +
        "    var url = host + '/list/' + cateId;\n" +
        "    if (page > 1) url += '-' + page;\n" +
        "    url += '.html';\n" +
        "    var html = fetchHtml(url);\n" +
        "    var list = extractVods(html);\n" +
        "    var pagecount = extractPageCount(html, page);\n" +
        "    return JSON.stringify({ list: list, page: page, pagecount: pagecount, limit: 20, total: pagecount * 20 });\n" +
        "}\n" +

        "function detail(id) {\n" +
        "    var url = (id.indexOf('http') === 0) ? id : fixUrl(id);\n" +
        "    var html = fetchHtml(url);\n" +
        "    if (!html) return JSON.stringify({ list: [] });\n" +
        "    var info = parseDetail(html, id);\n" +
        "    return JSON.stringify({ list: [info] });\n" +
        "}\n" +

        "function search(wd, quick, pg) {\n" +
        "    var url = host + '/s----------.html?wd=' + encodeURIComponent(wd);\n" +
        "    var html = fetchHtml(url);\n" +
        "    var list = extractVods(html);\n" +
        "    return JSON.stringify({ list: list });\n" +
        "}\n" +

        "function play(flag, id, flags) {\n" +
        "    if (id && /\\.(m3u8|mp4|flv|mkv|webm|ts)/i.test(id)) return JSON.stringify({ parse: 0, url: id });\n" +
        "    var url = (id.indexOf('http') === 0) ? id : fixUrl(id);\n" +
        "    var html = fetchHtml(url);\n" +
        "    var r = parsePlay(html, url);\n" +
        "    return JSON.stringify(r);\n" +
        "}\n" +

        "function fetchHtml(url) {\n" +
        "    try { var res = req(url); return (res && res.content) ? res.content : ''; } catch (e) { return ''; }\n" +
        "}\n" +

        "function __jsEvalReturn() {\n" +
        "    return { init: init, home: home, homeVod: homeVod, category: category, detail: detail, search: search, play: play };\n" +
        "}\n";

    private QuickJSContext ctx;
    private JSObject api;

    @Override
    public void init(Context context, String extend) {
        try {
            ctx = QuickJSContext.create();
            ctx.evaluate("var console = { log: function(){} };");

            ctx.getGlobalObject().setProperty("req", args -> {
                JSObject ret = ctx.createNewJSObject();
                try {
                    String url = (args == null || args.length == 0 || args[0] == null)
                            ? "" : args[0].toString();
                    Map<String, String> header = new HashMap<>();
                    header.put("User-Agent", "Mozilla/5.0 (Linux; Android 9; ALN-AL00 Build/PQ3B.190801.05281406; wv) AppleWebKit/537.36");
                    header.put("Referer", "https://shdy2.com/");
                    String content = OkHttp.string(url, header);
                    ret.setProperty("content", content == null ? "" : content);
                } catch (Exception e) {
                    SpiderDebug.log("req error: " + e.getMessage());
                    ret.setProperty("content", "");
                }
                return ret;
            });

            ctx.evaluate(JS_CODE);
            Object result = ctx.evaluate("__jsEvalReturn()");
            if (result instanceof JSObject) {
                api = (JSObject) result;
                SpiderDebug.log("SaoHuo init OK");
            } else {
                SpiderDebug.log("SaoHuo: 导出失败");
            }
        } catch (Exception e) {
            SpiderDebug.log("SaoHuo init FAIL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String callJs(String method, Object... args) {
        if (api == null) return "";
        try {
            Object fn = api.getProperty(method);
            if (!(fn instanceof JSFunction)) return "";
            Object r = ((JSFunction) fn).call(args);
            return r == null ? "" : r.toString();
        } catch (Exception e) {
            SpiderDebug.log("callJs " + method + " error: " + e.getMessage());
            return "";
        }
    }

    @Override
    public String homeContent(boolean filter) {
        String json = callJs("home", filter);
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray arr = obj.optJSONArray("class");
            List<Class> classes = new ArrayList<>();
            if (arr != null) for (int i = 0; i < arr.length(); i++) {
                JSONObject c = arr.getJSONObject(i);
                classes.add(new Class(c.optString("type_id"), c.optString("type_name")));
            }
            LinkedHashMap<String, List<Filter>> filters = new LinkedHashMap<>();
            JSONObject fObj = obj.optJSONObject("filters");
            if (fObj != null) {
                java.util.Iterator<String> keys = fObj.keys();
                while (keys.hasNext()) {
                    String tid = keys.next();
                    JSONArray fArr = fObj.optJSONArray(tid);
                    if (fArr == null) continue;
                    List<Filter> fList = new ArrayList<>();
                    for (int i = 0; i < fArr.length(); i++) {
                        JSONObject f = fArr.getJSONObject(i);
                        JSONArray vals = f.optJSONArray("value");
                        List<Filter.Value> fv = new ArrayList<>();
                        if (vals != null) for (int j = 0; j < vals.length(); j++) {
                            JSONObject v = vals.getJSONObject(j);
                            fv.add(new Filter.Value(v.optString("n"), v.optString("v")));
                        }
                        fList.add(new Filter(f.optString("key"), f.optString("name"), fv));
                    }
                    filters.put(tid, fList);
                }
            }
            return Result.string(classes, filters);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String homeVideoContent() { return convertList(callJs("homeVod")); }

    @Override
    public String categoryContent(String tid, String pg, boolean filter,
                                  HashMap<String, String> extend) {
        JSObject ext = ctx.createNewJSObject();
        if (extend != null) for (String k : extend.keySet()) ext.setProperty(k, extend.get(k));
        String json = callJs("category", tid, pg, filter, ext);
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray arr = obj.optJSONArray("list");
            List<Vod> list = parseVods(arr);
            int page = obj.optInt("page", 1);
            int pagecount = obj.optInt("pagecount", 1);
            int limit = obj.optInt("limit", 20);
            int total = obj.optInt("total", pagecount * limit);
            return Result.get().vod(list).page(page, pagecount, limit, total).string();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String detailContent(List<String> ids) {
        String json = callJs("detail", ids.get(0));
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray arr = obj.optJSONArray("list");
            if (arr == null || arr.length() == 0) return "";
            JSONObject d = arr.getJSONObject(0);
            Vod vod = new Vod();
            vod.setVodId(d.optString("vod_id"));
            vod.setVodName(d.optString("vod_name"));
            vod.setVodPic(d.optString("vod_pic"));
            vod.setVodContent(d.optString("vod_content"));
            vod.setVodPlayFrom(d.optString("vod_play_from"));
            vod.setVodPlayUrl(d.optString("vod_play_url"));
            return Result.string(vod);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String searchContent(String key, boolean quick) {
        return convertList(callJs("search", key, quick, "1"));
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        JSArray flags = ctx.createNewJSArray();
        if (vipFlags != null) for (int i = 0; i < vipFlags.size(); i++) flags.set(vipFlags.get(i), i);
        String json = callJs("play", flag, id, flags);
        try {
            JSONObject obj = new JSONObject(json);
            int parse = obj.optInt("parse", 1);
            String url = obj.optString("url");
            if (parse == 0) return Result.get().url(url).string();
            return Result.get().parse().url(url).string();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void destroy() {
        if (ctx != null) { try { ctx.destroy(); } catch (Exception ignored) {} ctx = null; api = null; }
    }

    private String convertList(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray arr = obj.optJSONArray("list");
            return Result.string(parseVods(arr));
        } catch (Exception e) {
            return "";
        }
    }

    private List<Vod> parseVods(JSONArray arr) {
        List<Vod> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject v = arr.getJSONObject(i);
                list.add(new Vod(
                    v.optString("vod_id"),
                    v.optString("vod_name"),
                    v.optString("vod_pic"),
                    v.optString("vod_remarks")
                ));
            } catch (Exception ignored) {}
        }
        return list;
    }
}