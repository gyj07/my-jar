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
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShenMa extends Spider {

    // ============================================================
    // ★ JS 代码（完整版，含筛选，QuickJS 兼容）
    // ============================================================
    private static final String JS_CODE =
        "var API_HOST = 'https://www.smyyok.cc';\n" +

        // ---------- 工具 ----------
        "function fetchHtml(url) {\n" +
        "    try { var res = req(url); return (res && res.content) ? res.content : ''; } catch (e) { return ''; }\n" +
        "}\n" +
        "function fixUrl(url) {\n" +
        "    if (!url) return '';\n" +
        "    url = url.trim();\n" +
        "    if (url.indexOf('//') === 0) return 'https:' + url;\n" +
        "    if (url.indexOf('http') === 0) return url;\n" +
        "    if (url.indexOf('/') === 0) return API_HOST + url;\n" +
        "    return API_HOST + '/' + url;\n" +
        "}\n" +
        "function cleanUrl(url) {\n" +
        "    if (!url) return '';\n" +
        "    url = url.replace(/\\\\\\//g, '/');\n" +
        "    url = url.replace(/^[\"']|[\"']$/g, '').trim();\n" +
        "    if (url.indexOf('//') === 0) url = 'https:' + url;\n" +
        "    return url;\n" +
        "}\n" +

        // ---------- 列表解析 ----------
        "function extractList(html) {\n" +
        "    var list = [];\n" +
        "    if (!html) return list;\n" +
        "    var re = /<div class=\"public-list-box public-pic-b\">([\\s\\S]*?)<\\/div>\\s*<\\/div>/g;\n" +
        "    var m;\n" +
        "    while ((m = re.exec(html)) !== null) {\n" +
        "        var item = m[1];\n" +
        "        var hrefM = item.match(/<a[^>]*class=\"public-list-exp\"[^>]*href=\"([^\"]+)\"/);\n" +
        "        var titleM = item.match(/<a[^>]*class=\"time-title[^\"]*\"[^>]*title=\"([^\"]+)\"/);\n" +
        "        var picM = item.match(/<img[^>]*data-src=\"([^\"]+)\"/);\n" +
        "        var remarkM = item.match(/<span[^>]*class=\"public-list-prb[^\"]*\"[^>]*>([^<]+)<\\/span>/);\n" +
        "        var href = hrefM ? hrefM[1] : '';\n" +
        "        var title = titleM ? titleM[1].trim() : '';\n" +
        "        var pic = picM ? picM[1] : '';\n" +
        "        var remark = remarkM ? remarkM[1].trim() : '';\n" +
        "        if (!href || !title) continue;\n" +
        "        list.push({ vod_id: href, vod_name: title, vod_pic: pic ? fixUrl(pic) : '', vod_remarks: remark });\n" +
        "    }\n" +
        "    return list;\n" +
        "}\n" +

        // ---------- 分页 ----------
        "function extractPageCount(html) {\n" +
        "    if (!html) return 1;\n" +
        "    var m1 = html.match(/共(\\d+)条数据,当前(\\d+)\\/(\\d+)页/);\n" +
        "    if (m1) return parseInt(m1[3]) || 1;\n" +
        "    var m2 = html.match(/<a[^>]*href=\"[^\"]*-----(\\d+)---[^\"]*\"[^>]*>尾页<\\/a>/);\n" +
        "    if (m2) return parseInt(m2[1]) || 1;\n" +
        "    return 1;\n" +
        "}\n" +

        // ---------- 构建分类 URL ----------
        "function buildVodShowUrl(tid, area, sort, pg, year, cls) {\n" +
        "    var parts = [];\n" +
        "    for (var i = 0; i < 12; i++) parts.push('');\n" +
        "    parts[1] = area || '';\n" +
        "    parts[2] = sort || '';\n" +
        "    parts[3] = cls || '';\n" +
        "    parts[8] = (parseInt(pg) > 1) ? pg : '';\n" +
        "    var url = '/vodshow/' + tid + parts.join('-');\n" +
        "    if (year) url += year;\n" +
        "    url += '.html';\n" +
        "    return url;\n" +
        "}\n" +

        // ---------- 详情 ----------
        "function parseDetail(html, id) {\n" +
        "    var info = { vod_id: id, vod_name: '', vod_pic: '', vod_content: '', vod_play_from: '', vod_play_url: '' };\n" +
        "    var nameM = html.match(/<h1[^>]*>([^<]+)<\\/h1>/);\n" +
        "    if (nameM) info.vod_name = nameM[1].trim();\n" +
        "    var picM = html.match(/<img[^>]*class=\"lazy lazy1 mask-1\"[^>]*data-src=\"([^\"]+)\"/);\n" +
        "    if (picM) info.vod_pic = fixUrl(picM[1]);\n" +
        "    var descM = html.match(/<div[^>]*id=\"height_limit\"[^>]*>([\\s\\S]*?)<\\/div>/);\n" +
        "    if (descM) info.vod_content = descM[1].replace(/<[^>]+>/g, '').trim();\n" +

        "    var playFrom = [];\n" +
        "    var playUrl = [];\n" +
        "    var names = [];\n" +
        // 线路名
        "    var tabRe = /<a[^>]*class=\"swiper-slide[^\"]*\"[^>]*>\\s*<i[^>]*><\\/i>&nbsp;([^<]+?)(?:<span[^>]*>\\d+<\\/span>)?<\\/a>/g;\n" +
        "    var tabM;\n" +
        "    while ((tabM = tabRe.exec(html)) !== null) {\n" +
        "        var n = tabM[1].trim();\n" +
        "        if (n && names.indexOf(n) < 0) names.push(n);\n" +
        "    }\n" +
        // 剧集
        "    var ulRe = /<ul[^>]*class=\"anthology-list-play size\"[^>]*>([\\s\\S]*?)<\\/ul>/g;\n" +
        "    var ulM;\n" +
        "    var idx = 0;\n" +
        "    while ((ulM = ulRe.exec(html)) !== null) {\n" +
        "        var eps = [];\n" +
        "        var epRe = /<a[^>]*class=\"hide[^\"]*\"[^>]*href=\"([^\"]+)\"[^>]*>([^<]+)<\\/a>/g;\n" +
        "        var epM;\n" +
        "        while ((epM = epRe.exec(ulM[1])) !== null) {\n" +
        "            var epName = epM[2].trim();\n" +
        "            var epUrl = fixUrl(epM[1]);\n" +
        "            if (epName && epUrl) eps.push(epName + '$' + epUrl);\n" +
        "        }\n" +
        "        if (eps.length > 0) {\n" +
        "            var lineName = (idx < names.length) ? names[idx] : ('线路' + (idx + 1));\n" +
        "            playFrom.push('【神马影院】' + lineName);\n" +
        "            playUrl.push(eps.join('#'));\n" +
        "            idx++;\n" +
        "        }\n" +
        "    }\n" +
        "    info.vod_play_from = playFrom.join('$$$');\n" +
        "    info.vod_play_url = playUrl.join('$$$');\n" +
        "    return info;\n" +
        "}\n" +

        // ---------- 播放 ----------
        "function parsePlay(html, id) {\n" +
        "    if (!html) return { parse: 1, url: id };\n" +
        "    var iframeM = html.match(/<iframe[^>]*src=\"([^\"]*player\\/\\?url=[^\"]*)\"/);\n" +
        "    if (iframeM) { var wrapper = iframeM[1].replace(/&amp;/g, '&'); return { parse: 1, url: wrapper }; }\n" +
        "    var pM = html.match(/var\\s+player_aaaa\\s*=\\s*(\\{[^;]+\\})/);\n" +
        "    if (pM) {\n" +
        "        var uM = pM[1].match(/\"url\"\\s*:\\s*\"([^\"]+)\"/);\n" +
        "        if (uM) {\n" +
        "            var url = cleanUrl(uM[1]);\n" +
        "            if (url.indexOf('.m3u8') >= 0 && url.indexOf('url=http') < 0) return { parse: 0, url: url };\n" +
        "            return { parse: 1, url: url };\n" +
        "        }\n" +
        "    }\n" +
        "    return { parse: 1, url: id };\n" +
        "}\n" +

        // ---------- 筛选配置 ----------
        "function getFilter() {\n" +
        "    var areaValues = [ {v:'',n:'全部'},\n" +
        "        {v:'大陆',n:'大陆'},{v:'香港',n:'香港'},{v:'台湾',n:'台湾'},\n" +
        "        {v:'美国',n:'美国'},{v:'日本',n:'日本'},{v:'韩国',n:'韩国'},\n" +
        "        {v:'英国',n:'英国'},{v:'法国',n:'法国'},{v:'德国',n:'德国'},\n" +
        "        {v:'意大利',n:'意大利'},{v:'西班牙',n:'西班牙'},{v:'俄罗斯',n:'俄罗斯'},\n" +
        "        {v:'加拿大',n:'加拿大'},{v:'印度',n:'印度'},{v:'泰国',n:'泰国'},\n" +
        "        {v:'其它',n:'其它'},{v:'新加坡',n:'新加坡'},{v:'菲律宾',n:'菲律宾'},\n" +
        "        {v:'澳大利亚',n:'澳大利亚'},{v:'土耳其',n:'土耳其'},{v:'瑞典',n:'瑞典'},\n" +
        "        {v:'巴西',n:'巴西'},{v:'荷兰',n:'荷兰'},{v:'印度尼西亚',n:'印度尼西亚'},\n" +
        "        {v:'挪威',n:'挪威'},{v:'智利',n:'智利'},{v:'爱尔兰',n:'爱尔兰'},\n" +
        "        {v:'伊朗',n:'伊朗'},{v:'蒙古',n:'蒙古'} ];\n" +
        "    var yearValues = [ {v:'',n:'全部'} ];\n" +
        "    for (var y = 2026; y >= 2000; y--) yearValues.push({v:''+y, n:''+y});\n" +
        "    var sortValues = [ {v:'time',n:'按最新'},{v:'hits',n:'按最热'},{v:'score',n:'按评分'} ];\n" +

        "    var classValues1 = [ {v:'',n:'全部'},\n" +
        "        {v:'动作片',n:'动作片'},{v:'喜剧片',n:'喜剧片'},{v:'科幻片',n:'科幻片'},\n" +
        "        {v:'恐怖片',n:'恐怖片'},{v:'爱情片',n:'爱情片'},{v:'剧情片',n:'剧情片'},\n" +
        "        {v:'战争片',n:'战争片'},{v:'记录片',n:'记录片'},{v:'动画片',n:'动画片'},\n" +
        "        {v:'惊悚',n:'惊悚'},{v:'犯罪',n:'犯罪'},{v:'悬疑',n:'悬疑'},{v:'冒险',n:'冒险'},\n" +
        "        {v:'奇幻',n:'奇幻'},{v:'家庭',n:'家庭'},{v:'历史',n:'历史'},{v:'传记',n:'传记'},\n" +
        "        {v:'古装',n:'古装'},{v:'音乐',n:'音乐'},{v:'同性',n:'同性'},{v:'运动',n:'运动'},\n" +
        "        {v:'武侠',n:'武侠'},{v:'短片',n:'短片'},{v:'歌舞',n:'歌舞'},{v:'西部',n:'西部'},\n" +
        "        {v:'儿童',n:'儿童'},{v:'灾难',n:'灾难'},{v:'戏曲',n:'戏曲'},{v:'真人秀',n:'真人秀'},\n" +
        "        {v:'青春',n:'青春'} ];\n" +

        "    var classValues2 = [ {v:'',n:'全部'},\n" +
        "        {v:'国产剧',n:'国产剧'},{v:'欧美剧',n:'欧美剧'},{v:'香港剧',n:'香港剧'},\n" +
        "        {v:'韩国剧',n:'韩国剧'},{v:'台湾剧',n:'台湾剧'},{v:'日本剧',n:'日本剧'},\n" +
        "        {v:'海外剧',n:'海外剧'},{v:'泰国剧',n:'泰国剧'},{v:'剧情',n:'剧情'},\n" +
        "        {v:'爱情',n:'爱情'},{v:'喜剧',n:'喜剧'},{v:'悬疑',n:'悬疑'},{v:'犯罪',n:'犯罪'},\n" +
        "        {v:'古装',n:'古装'},{v:'动作',n:'动作'},{v:'奇幻',n:'奇幻'},{v:'惊悚',n:'惊悚'},\n" +
        "        {v:'家庭',n:'家庭'},{v:'历史',n:'历史'},{v:'科幻',n:'科幻'},{v:'战争',n:'战争'},\n" +
        "        {v:'同性',n:'同性'},{v:'武侠',n:'武侠'},{v:'冒险',n:'冒险'},{v:'恐怖',n:'恐怖'},\n" +
        "        {v:'纪录',n:'纪录'},{v:'传记',n:'传记'},{v:'短片',n:'短片'},{v:'运动',n:'运动'},\n" +
        "        {v:'音乐',n:'音乐'},{v:'儿童',n:'儿童'},{v:'歌舞',n:'歌舞'},{v:'西部',n:'西部'},\n" +
        "        {v:'灾难',n:'灾难'} ];\n" +

        "    var classValues3 = [ {v:'',n:'全部'},\n" +
        "        {v:'大陆综艺',n:'大陆综艺'},{v:'港台综艺',n:'港台综艺'},{v:'日韩综艺',n:'日韩综艺'},\n" +
        "        {v:'欧美综艺',n:'欧美综艺'},{v:'真人秀',n:'真人秀'},{v:'纪录片',n:'纪录片'},\n" +
        "        {v:'脱口秀',n:'脱口秀'},{v:'音乐',n:'音乐'},{v:'歌舞',n:'歌舞'},{v:'相声',n:'相声'},\n" +
        "        {v:'喜剧',n:'喜剧'},{v:'爱情',n:'爱情'},{v:'历史',n:'历史'},{v:'运动',n:'运动'},\n" +
        "        {v:'冒险',n:'冒险'},{v:'剧情',n:'剧情'},{v:'访谈',n:'访谈'},{v:'旅游',n:'旅游'},\n" +
        "        {v:'悬疑',n:'悬疑'},{v:'家庭',n:'家庭'},{v:'短片',n:'短片'},{v:'同性',n:'同性'},\n" +
        "        {v:'动作',n:'动作'},{v:'儿童',n:'儿童'},{v:'惊悚',n:'惊悚'},{v:'美食',n:'美食'} ];\n" +

        "    var classValues4 = [ {v:'',n:'全部'},\n" +
        "        {v:'国产动漫',n:'国产动漫'},{v:'日韩动漫',n:'日韩动漫'},{v:'欧美动漫',n:'欧美动漫'},\n" +
        "        {v:'港台动漫',n:'港台动漫'},{v:'海外动漫',n:'海外动漫'},{v:'动画',n:'动画'},\n" +
        "        {v:'喜剧',n:'喜剧'},{v:'剧情',n:'剧情'},{v:'奇幻',n:'奇幻'},{v:'冒险',n:'冒险'},\n" +
        "        {v:'动作',n:'动作'},{v:'科幻',n:'科幻'},{v:'爱情',n:'爱情'},{v:'儿童',n:'儿童'},\n" +
        "        {v:'家庭',n:'家庭'},{v:'短片',n:'短片'},{v:'悬疑',n:'悬疑'},{v:'运动',n:'运动'},\n" +
        "        {v:'古装',n:'古装'},{v:'武侠',n:'武侠'},{v:'音乐',n:'音乐'},{v:'犯罪',n:'犯罪'},\n" +
        "        {v:'惊悚',n:'惊悚'},{v:'战争',n:'战争'},{v:'恐怖',n:'恐怖'},{v:'历史',n:'历史'},\n" +
        "        {v:'搞笑',n:'搞笑'},{v:'歌舞',n:'歌舞'},{v:'热血',n:'热血'} ];\n" +

        "    var classValues5 = [ {v:'',n:'全部'},\n" +
        "        {v:'女频恋爱',n:'女频恋爱'},{v:'反转爽剧',n:'反转爽剧'},{v:'古装仙侠',n:'古装仙侠'},\n" +
        "        {v:'年代穿越',n:'年代穿越'},{v:'脑洞悬疑',n:'脑洞悬疑'},{v:'现代都市',n:'现代都市'} ];\n" +

        "    return {\n" +
        "        '1': [ {key:'class',name:'类型',value:classValues1},\n" +
        "               {key:'area',name:'地区',value:areaValues},\n" +
        "               {key:'year',name:'年份',value:yearValues},\n" +
        "               {key:'sort',name:'排序',value:sortValues} ],\n" +
        "        '2': [ {key:'class',name:'类型',value:classValues2},\n" +
        "               {key:'area',name:'地区',value:areaValues},\n" +
        "               {key:'year',name:'年份',value:yearValues},\n" +
        "               {key:'sort',name:'排序',value:sortValues} ],\n" +
        "        '3': [ {key:'class',name:'类型',value:classValues3},\n" +
        "               {key:'area',name:'地区',value:areaValues},\n" +
        "               {key:'year',name:'年份',value:yearValues},\n" +
        "               {key:'sort',name:'排序',value:sortValues} ],\n" +
        "        '4': [ {key:'class',name:'类型',value:classValues4},\n" +
        "               {key:'area',name:'地区',value:areaValues},\n" +
        "               {key:'year',name:'年份',value:yearValues},\n" +
        "               {key:'sort',name:'排序',value:sortValues} ],\n" +
        "        '5': [ {key:'class',name:'类型',value:classValues5},\n" +
        "               {key:'area',name:'地区',value:areaValues},\n" +
        "               {key:'year',name:'年份',value:yearValues},\n" +
        "               {key:'sort',name:'排序',value:sortValues} ]\n" +
        "    };\n" +
        "}\n" +

        // ---------- TVBox 接口 ----------
        "function init(cfg) { return true; }\n" +

        "function home(filter) {\n" +
        "    var classes = [\n" +
        "        { type_id: '1', type_name: '电影' },\n" +
        "        { type_id: '2', type_name: '电视剧' },\n" +
        "        { type_id: '3', type_name: '综艺' },\n" +
        "        { type_id: '4', type_name: '动漫' },\n" +
        "        { type_id: '5', type_name: '短剧' }\n" +
        "    ];\n" +
        "    var result = { class: classes };\n" +
        "    if (filter) result.filters = getFilter();\n" +
        "    return JSON.stringify(result);\n" +
        "}\n" +

        "function homeVod() {\n" +
        "    var html = fetchHtml(API_HOST + '/');\n" +
        "    var list = extractList(html);\n" +
        "    if (list.length > 12) list = list.slice(0, 12);\n" +
        "    return JSON.stringify({ list: list });\n" +
        "}\n" +

        "function category(tid, pg, filter, extend) {\n" +
        "    var cls = (extend && extend['class']) ? extend['class'] : '';\n" +
        "    var area = (extend && extend['area']) ? extend['area'] : '';\n" +
        "    var year = (extend && extend['year']) ? extend['year'] : '';\n" +
        "    var sort = (extend && extend['sort']) ? extend['sort'] : '';\n" +
        "    if (cls === '全部') cls = '';\n" +
        "    if (area === '全部') area = '';\n" +
        "    if (year === '全部') year = '';\n" +
        "    if (sort === '全部') sort = '';\n" +
        "    var url = API_HOST + buildVodShowUrl(tid, area, sort, pg, year, cls);\n" +
        "    var html = fetchHtml(url);\n" +
        "    var list = extractList(html);\n" +
        "    var pagecount = extractPageCount(html);\n" +
        "    return JSON.stringify({ page: parseInt(pg) || 1, pagecount: pagecount, limit: 24, total: pagecount * 24, list: list });\n" +
        "}\n" +

        "function detail(id) {\n" +
        "    var url = id.indexOf('http') === 0 ? id : API_HOST + id;\n" +
        "    var html = fetchHtml(url);\n" +
        "    if (!html) return JSON.stringify({ list: [] });\n" +
        "    var info = parseDetail(html, id);\n" +
        "    return JSON.stringify({ list: [info] });\n" +
        "}\n" +

        "function search(wd, quick, pg) {\n" +
        "    var url = API_HOST + '/vodsearch/' + encodeURIComponent(wd) + '-------------.html';\n" +
        "    var html = fetchHtml(url);\n" +
        "    var list = extractList(html);\n" +
        "    return JSON.stringify({ list: list, page: parseInt(pg) || 1, pagecount: 1 });\n" +
        "}\n" +

        "function play(flag, id, flags) {\n" +
        "    if (id && /\\.(m3u8|mp4|flv|mkv|webm|ts)/i.test(id)) return JSON.stringify({ parse: 0, url: id });\n" +
        "    var html = fetchHtml(id);\n" +
        "    var result = parsePlay(html, id);\n" +
        "    return JSON.stringify(result);\n" +
        "}\n" +

        // ---------- 导出 ----------
        "function __jsEvalReturn() {\n" +
        "    return { init: init, home: home, homeVod: homeVod, category: category, detail: detail, search: search, play: play };\n" +
        "}\n";

    private QuickJSContext ctx;
    private JSObject api;

    // ============================================================
    // init
    // ============================================================
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
                    header.put("User-Agent", Util.CHROME);
                    header.put("Referer", "https://www.smyyok.cc/");
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
                SpiderDebug.log("ShenMa init OK");
            } else {
                SpiderDebug.log("ShenMa: __jsEvalReturn() 返回不是对象");
            }

        } catch (Exception e) {
            SpiderDebug.log("ShenMa init FAIL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================
    // JS 调用
    // ============================================================
    private String callJs(String method, Object... args) {
        if (api == null) return "";
        try {
            Object fn = api.getProperty(method);
            if (!(fn instanceof JSObject)) return "";
            Object r = ((JSObject) fn).call(args);
            return r == null ? "" : r.toString();
        } catch (Exception e) {
            SpiderDebug.log("callJs " + method + " error: " + e.getMessage());
            return "";
        }
    }

    // ============================================================
    // Spider 接口
    // ============================================================
    @Override
    public String homeContent(boolean filter) {
        String json = callJs("home", filter);
        SpiderDebug.log("home raw: " + preview(json));
        try {
            JSONObject obj = new JSONObject(json);

            JSONArray arr = obj.optJSONArray("class");
            List<Class> classes = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject c = arr.getJSONObject(i);
                    classes.add(new Class(c.optString("type_id"), c.optString("type_name")));
                }
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
                        String key = f.optString("key");
                        String name = f.optString("name");
                        JSONArray vals = f.optJSONArray("value");
                        List<Filter.Value> fv = new ArrayList<>();
                        if (vals != null) for (int j = 0; j < vals.length(); j++) {
                            JSONObject v = vals.getJSONObject(j);
                            fv.add(new Filter.Value(v.optString("n"), v.optString("v")));
                        }
                        fList.add(new Filter(key, name, fv));
                    }
                    filters.put(tid, fList);
                }
            }

            return Result.string(classes, filters);
        } catch (Exception e) {
            SpiderDebug.log("homeContent error: " + e.getMessage());
            return "";
        }
    }

    @Override
    public String homeVideoContent() {
        return convertList(callJs("homeVod"));
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter,
                                  HashMap<String, String> extend) {
        JSObject ext = ctx.createNewJSObject();
        if (extend != null) for (String k : extend.keySet()) ext.setProperty(k, extend.get(k));
        String json = callJs("category", tid, pg, filter, ext);
        SpiderDebug.log("category raw: " + preview(json));
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray arr = obj.optJSONArray("list");
            List<Vod> list = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject v = arr.getJSONObject(i);
                    list.add(new Vod(
                        v.optString("vod_id"),
                        v.optString("vod_name"),
                        v.optString("vod_pic"),
                        v.optString("vod_remarks")
                    ));
                }
            }
            int page = obj.optInt("page", 1);
            int pagecount = obj.optInt("pagecount", 1);
            int limit = obj.optInt("limit", 24);
            int total = obj.optInt("total", pagecount * limit);
            return Result.get().vod(list).page(page, pagecount, limit, total).string();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String detailContent(List<String> ids) {
        String json = callJs("detail", ids.get(0));
        SpiderDebug.log("detail raw: " + preview(json));
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
        SpiderDebug.log("play raw: " + preview(json));
        try {
            JSONObject obj = new JSONObject(json);
            int parse = obj.optInt("parse", 1);
            String url = obj.optString("url");
            SpiderDebug.log("play parse=" + parse + " url=" + url);
            if (parse == 0) return Result.get().url(url).string();
            return Result.get().parse().url(url).string();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void destroy() {
        if (ctx != null) {
            try { ctx.destroy(); } catch (Exception ignored) {}
            ctx = null;
            api = null;
        }
    }

    // ============================================================
    // 工具
    // ============================================================
    private String convertList(String json) {
        try {
            if (TextUtils.isEmpty(json)) return "";
            JSONObject obj = new JSONObject(json);
            JSONArray arr = obj.optJSONArray("list");
            List<Vod> list = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject v = arr.getJSONObject(i);
                    list.add(new Vod(
                        v.optString("vod_id"),
                        v.optString("vod_name"),
                        v.optString("vod_pic"),
                        v.optString("vod_remarks")
                    ));
                }
            }
            return Result.string(list);
        } catch (Exception e) {
            return "";
        }
    }

    private String preview(String s) {
        if (s == null) return "null";
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}