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

public class DuShe extends Spider {

    private static final String JS_CODE =
        "var API_HOST = 'https://www.dushehub.com';\n" +

        "function fixUrl(url) {\n" +
        "    if (!url) return '';\n" +
        "    if (url.indexOf('//') === 0) return 'https:' + url;\n" +
        "    if (url.indexOf('http') === 0) return url;\n" +
        "    if (url.indexOf('/') === 0) return API_HOST + url;\n" +
        "    return API_HOST + '/' + url;\n" +
        "}\n" +

        "function cleanHtml(t) {\n" +
        "    if (!t) return '';\n" +
        "    return t.replace(/<[^>]+>/g, '').replace(/\\s+/g, ' ').trim();\n" +
        "}\n" +

        "function fetchHtml(url) {\n" +
        "    try { var res = req(url); return (res && res.content) ? res.content : ''; } catch (e) { return ''; }\n" +
        "}\n" +

        // 分类/筛选
        "function getFilters() {\n" +
        "    var TYPE = {\n" +
        "        'dianying': [ {v:'',n:'全部'},{v:'dongzuo',n:'动作'},{v:'xiju',n:'喜剧'},{v:'aiqing',n:'爱情'},{v:'kehuan',n:'科幻'},{v:'jilu',n:'纪录传记'},{v:'zhanzheng',n:'战争灾难'},{v:'juqing',n:'家庭剧情'},{v:'lishi',n:'古装历史'},{v:'fanzui',n:'犯罪悬疑'},{v:'kongbu',n:'惊悚恐怖'},{v:'qihuan',n:'奇幻冒险'},{v:'donghua',n:'动画影院'} ],\n" +
        "        'dianshiju': [ {v:'',n:'全部'},{v:'guochan',n:'国产'},{v:'gangju',n:'港剧'},{v:'zilei10',n:'台剧'},{v:'hanju',n:'韩剧'},{v:'riju',n:'日剧'},{v:'taiju',n:'泰剧'},{v:'meiju',n:'欧美'},{v:'haiwaiju',n:'海外'} ],\n" +
        "        'zongyi': [ {v:'',n:'全部'},{v:'dalu',n:'大陆'},{v:'gangtai',n:'港台'},{v:'rihan',n:'日韩'},{v:'hiwai',n:'海外'} ],\n" +
        "        'dongman': [ {v:'',n:'全部'},{v:'guoman',n:'国漫'},{v:'riman',n:'日漫'},{v:'oumei',n:'欧美'} ]\n" +
        "    };\n" +
        "    var STORY = {\n" +
        "        'dianying': [ {v:'',n:'全部'},{v:'喜剧',n:'喜剧'},{v:'爱情',n:'爱情'},{v:'恐怖',n:'恐怖'},{v:'动作',n:'动作'},{v:'科幻',n:'科幻'},{v:'剧情',n:'剧情'},{v:'战争',n:'战争'},{v:'警匪',n:'警匪'},{v:'犯罪',n:'犯罪'},{v:'动画',n:'动画'},{v:'奇幻',n:'奇幻'},{v:'武侠',n:'武侠'},{v:'冒险',n:'冒险'},{v:'枪战',n:'枪战'},{v:'悬疑',n:'悬疑'},{v:'惊悚',n:'惊悚'},{v:'经典',n:'经典'},{v:'青春',n:'青春'},{v:'文艺',n:'文艺'},{v:'微电影',n:'微电影'},{v:'古装',n:'古装'},{v:'历史',n:'历史'},{v:'运动',n:'运动'},{v:'农村',n:'农村'},{v:'儿童',n:'儿童'},{v:'网络电影',n:'网络电影'} ],\n" +
        "        'dianshiju': [ {v:'',n:'全部'},{v:'古装',n:'古装'},{v:'战争',n:'战争'},{v:'青春偶像',n:'青春偶像'},{v:'喜剧',n:'喜剧'},{v:'家庭',n:'家庭'},{v:'犯罪',n:'犯罪'},{v:'动作',n:'动作'},{v:'奇幻',n:'奇幻'},{v:'剧情',n:'剧情'},{v:'历史',n:'历史'},{v:'经典',n:'经典'},{v:'乡村',n:'乡村'},{v:'情景',n:'情景'},{v:'商战',n:'商战'},{v:'网剧',n:'网剧'} ],\n" +
        "        'zongyi': [ {v:'',n:'全部'},{v:'情感',n:'情感'},{v:'科幻',n:'科幻'},{v:'热血',n:'热血'},{v:'推理',n:'推理'},{v:'搞笑',n:'搞笑'},{v:'冒险',n:'冒险'},{v:'萝莉',n:'萝莉'},{v:'校园',n:'校园'},{v:'动作',n:'动作'},{v:'机战',n:'机战'},{v:'运动',n:'运动'},{v:'战争',n:'战争'},{v:'少年',n:'少年'},{v:'少女',n:'少女'},{v:'社会',n:'社会'},{v:'原创',n:'原创'},{v:'亲子',n:'亲子'},{v:'益智',n:'益智'},{v:'励志',n:'励志'},{v:'其他',n:'其他'} ],\n" +
        "        'dongman': [ {v:'',n:'全部'},{v:'选秀',n:'选秀'},{v:'情感',n:'情感'},{v:'访谈',n:'访谈'},{v:'播报',n:'播报'},{v:'旅游',n:'旅游'},{v:'音乐',n:'音乐'},{v:'美食',n:'美食'},{v:'纪实',n:'纪实'},{v:'曲艺',n:'曲艺'},{v:'生活',n:'生活'},{v:'游戏互动',n:'游戏互动'},{v:'财经',n:'财经'},{v:'求职',n:'求职'} ]\n" +
        "    };\n" +
        "    var AREA = {\n" +
        "        'dianying': [ {v:'',n:'全部'},{v:'大陆',n:'大陆'},{v:'香港',n:'香港'},{v:'台湾',n:'台湾'},{v:'美国',n:'美国'},{v:'法国',n:'法国'},{v:'英国',n:'英国'},{v:'日本',n:'日本'},{v:'韩国',n:'韩国'},{v:'德国',n:'德国'},{v:'泰国',n:'泰国'},{v:'印度',n:'印度'},{v:'意大利',n:'意大利'},{v:'西班牙',n:'西班牙'},{v:'加拿大',n:'加拿大'},{v:'其他',n:'其他'} ],\n" +
        "        'dianshiju': [ {v:'',n:'全部'},{v:'中国大陆',n:'中国大陆'},{v:'中国香港',n:'中国香港'},{v:'中国台湾',n:'中国台湾'},{v:'美国',n:'美国'},{v:'韩国',n:'韩国'},{v:'日本',n:'日本'},{v:'泰国',n:'泰国'},{v:'新加坡',n:'新加坡'},{v:'德国',n:'德国'},{v:'马来西亚',n:'马来西亚'},{v:'印度',n:'印度'},{v:'英国',n:'英国'},{v:'法国',n:'法国'},{v:'加拿大',n:'加拿大'},{v:'西班牙',n:'西班牙'},{v:'俄罗斯',n:'俄罗斯'},{v:'其它',n:'其它'} ],\n" +
        "        'zongyi': [ {v:'',n:'全部'},{v:'国产',n:'国产'},{v:'日本',n:'日本'},{v:'欧美',n:'欧美'},{v:'其他',n:'其他'} ],\n" +
        "        'dongman': [ {v:'',n:'全部'},{v:'内地',n:'内地'},{v:'港台',n:'港台'},{v:'日韩',n:'日韩'},{v:'欧美',n:'欧美'} ]\n" +
        "    };\n" +
        "    var YEAR = [{v:'',n:'全部'},{v:'2027',n:'2027'},{v:'2026',n:'2026'},{v:'2025',n:'2025'},{v:'2024',n:'2024'},{v:'2023',n:'2023'},{v:'2022',n:'2022'},{v:'2021',n:'2021'},{v:'2020',n:'2020'},{v:'2019',n:'2019'},{v:'2018',n:'2018'}];\n" +
        "    var SORT = [{v:'',n:'默认'},{v:'time',n:'时间排序'},{v:'hits',n:'人气排序'},{v:'score',n:'评分排序'}];\n" +
        "    var filters = {};\n" +
        "    var cats = ['dianying','dianshiju','zongyi','dongman'];\n" +
        "    for (var i = 0; i < cats.length; i++) {\n" +
        "        var c = cats[i];\n" +
        "        filters[c] = [\n" +
        "            { key: 'class', name: '类型', value: TYPE[c] || [] },\n" +
        "            { key: 'story', name: '剧情', value: STORY[c] || [] },\n" +
        "            { key: 'area', name: '地区', value: AREA[c] || [] },\n" +
        "            { key: 'year', name: '年份', value: YEAR },\n" +
        "            { key: 'sort', name: '排序', value: SORT }\n" +
        "        ];\n" +
        "    }\n" +
        "    return filters;\n" +
        "}\n" +

        // 列表
        "function extractVods(html) {\n" +
        "    var list = [];\n" +
        "    if (!html) return list;\n" +
        "    var re = /<a[^>]*href=\"(\\/album\\/\\d+\\.html)\"[^>]*title=\"([^\"]*)\"[^>]*>[\\s\\S]*?<div[^>]*class=\"[^\"]*module-item-note[^\"]*\"[^>]*>([^<]*)<\\/div>[\\s\\S]*?<img[^>]*data-original=\"([^\"]+)\"[^>]*>/g;\n" +
        "    var m;\n" +
        "    while ((m = re.exec(html)) !== null) {\n" +
        "        var id = fixUrl(m[1]), name = m[2] ? m[2].trim() : '', remark = m[3] ? m[3].trim() : '', pic = fixUrl(m[4]);\n" +
        "        if (!name) continue;\n" +
        "        if (list.some(function(x){return x.vod_id===id;})) continue;\n" +
        "        list.push({ vod_id: id, vod_name: name, vod_pic: pic, vod_remarks: remark });\n" +
        "    }\n" +
        "    return list;\n" +
        "}\n" +

        // 详情
        "function parseDetail(html, id) {\n" +
        "    var info = { vod_id: id, vod_name: '未知', vod_pic: '', vod_content: '', vod_play_from: '', vod_play_url: '' };\n" +
        "    var titleM = html.match(/<h1[^>]*>([^<]+)<\\/h1>/);\n" +
        "    if (titleM) info.vod_name = titleM[1].trim();\n" +
        "    var picM = html.match(/<div[^>]*class=\"[^\"]*module-item-pic[^\"]*\"[^>]*>\\s*<img[^>]*data-original=\"([^\"]+)\"/);\n" +
        "    if (picM) info.vod_pic = fixUrl(picM[1]);\n" +
        "    var descM = html.match(/<div[^>]*class=\"[^\"]*module-info-introduction-content[^\"]*\"[^>]*>[\\s\\S]*?<p>([\\s\\S]*?)<\\/p>/);\n" +
        "    if (descM) info.vod_content = cleanHtml(descM[1]);\n" +

        // 线路名
        "    var tabs = [];\n" +
        "    var tRe = /<div[^>]*class=\"[^\"]*module-tab-item[^\"]*\"[^>]*data-dropdown-value=\"([^\"]+)\"[^>]*>/g;\n" +
        "    var tM;\n" +
        "    while ((tM = tRe.exec(html)) !== null) {\n" +
        "        var n = tM[1].trim();\n" +
        "        if (n && tabs.indexOf(n) < 0) tabs.push(n);\n" +
        "    }\n" +

        // 剧集
        "    var eps = [];\n" +
        "    var epRe = /<a[^>]*class=\"[^\"]*module-play-list-link[^\"]*\"[^>]*href=\"(\\/play\\/\\d+-\\d+-\\d+\\.html)\"[^>]*>[\\s\\S]*?<span>([^<]+)<\\/span>/g;\n" +
        "    var epM;\n" +
        "    while ((epM = epRe.exec(html)) !== null) {\n" +
        "        var u = fixUrl(epM[1]);\n" +
        "        var nm = epM[2].trim() || '第1集';\n" +
        "        if (u && !eps.some(function(x){return x.u===u;})) eps.push({ n: nm, u: u });\n" +
        "    }\n" +

        // 按 sid 分组
        "    var sidMap = {};\n" +
        "    for (var i = 0; i < eps.length; i++) {\n" +
        "        var sidM = eps[i].u.match(/\\/play\\/\\d+-(\\d+)-\\d+\\.html/);\n" +
        "        if (sidM) {\n" +
        "            var sid = sidM[1];\n" +
        "            if (!sidMap[sid]) sidMap[sid] = [];\n" +
        "            sidMap[sid].push(eps[i].n + '$' + eps[i].u);\n" +
        "        }\n" +
        "    }\n" +
        "    var sidKeys = Object.keys(sidMap).sort(function(a,b){ return parseInt(a) - parseInt(b); });\n" +
        "    var playFrom = [];\n" +
        "    var playUrl = [];\n" +
        "    for (var k = 0; k < sidKeys.length; k++) {\n" +
        "        playFrom.push(k < tabs.length ? tabs[k] : ('线路' + (k + 1)));\n" +
        "        playUrl.push(sidMap[sidKeys[k]].join('#'));\n" +
        "    }\n" +
        "    if (playFrom.length > 0) {\n" +
        "        info.vod_play_from = playFrom.join('$$$');\n" +
        "        info.vod_play_url = playUrl.join('$$$');\n" +
        "    }\n" +
        "    return info;\n" +
        "}\n" +

        // 播放
        "function parsePlay(html, id) {\n" +
        "    if (!html) return { parse: 1, url: id };\n" +
        "    var pM = html.match(/var\\s+player_aaaa\\s*=\\s*(\\{[^;]+\\})/);\n" +
        "    if (pM) {\n" +
        "        try {\n" +
        "            var pData = JSON.parse(pM[1]);\n" +
        "            if (pData.url) {\n" +
        "                var u = pData.url.replace(/\\\\\\//g, '/');\n" +
        "                if (u.indexOf('//') === 0) u = 'https:' + u;\n" +
        "                if (u.indexOf('.m3u8') >= 0 && u.indexOf('http') === 0) return { parse: 0, url: u };\n" +
        "                if (u.indexOf('http') === 0) return { parse: 1, url: u };\n" +
        "            }\n" +
        "        } catch (e) {}\n" +
        "    }\n" +
        "    var m = html.match(/https?:\\/\\/[^\\s\"']+\\.m3u8(?:\\?[^\\s\"']*)?/i);\n" +
        "    if (m) return { parse: 0, url: m[0] };\n" +
        "    var ifM = html.match(/<iframe[^>]*src=[\"']([^\"']+)[\"']/);\n" +
        "    if (ifM) {\n" +
        "        var u2 = ifM[1];\n" +
        "        if (u2.indexOf('//') === 0) u2 = 'https:' + u2;\n" +
        "        if (u2.indexOf('/') === 0) u2 = fixUrl(u2);\n" +
        "        return { parse: 1, url: u2 };\n" +
        "    }\n" +
        "    return { parse: 1, url: id };\n" +
        "}\n" +

        // 构建分类 URL
        "function buildCategoryUrl(tid, extend) {\n" +
        "    var cls = (extend && extend['class']) ? extend['class'] : '';\n" +
        "    var story = (extend && extend['story']) ? extend['story'] : '';\n" +
        "    var area = (extend && extend['area']) ? extend['area'] : '';\n" +
        "    var year = (extend && extend['year']) ? extend['year'] : '';\n" +
        "    var sort = (extend && extend['sort']) ? extend['sort'] : '';\n" +
        "    var mainType = cls ? cls : tid;\n" +
        "    var url = '/show/' + mainType;\n" +
        "    url += area ? ('-' + encodeURIComponent(area)) : '-';\n" +
        "    url += (sort && ['time','hits','score'].indexOf(sort) >= 0) ? ('-' + sort) : '-';\n" +
        "    url += story ? ('-' + encodeURIComponent(story)) : '-';\n" +
        "    var dashCount = (url.match(/-/g) || []).length;\n" +
        "    var need = 8 - dashCount;\n" +
        "    if (need < 0) need = 0;\n" +
        "    for (var i = 0; i < need; i++) url += '-';\n" +
        "    if (extend && extend.page && parseInt(extend.page) > 1) url += extend.page;\n" +
        "    url += '---';\n" +
        "    if (year) url += year;\n" +
        "    url += '.html';\n" +
        "    return url;\n" +
        "}\n" +

        // TVBox 接口
        "function init(cfg) { return true; }\n" +
        "function home(filter) {\n" +
        "    var classes = [\n" +
        "        { type_id: 'dianying', type_name: '电影' },\n" +
        "        { type_id: 'dianshiju', type_name: '电视剧' },\n" +
        "        { type_id: 'zongyi', type_name: '综艺' },\n" +
        "        { type_id: 'dongman', type_name: '动漫' }\n" +
        "    ];\n" +
        "    return JSON.stringify({ class: classes, filters: getFilters() });\n" +
        "}\n" +
        "function homeVod() {\n" +
        "    var html = fetchHtml(API_HOST + '/');\n" +
        "    var list = extractVods(html);\n" +
        "    if (list.length > 12) list = list.slice(0, 12);\n" +
        "    return JSON.stringify({ list: list });\n" +
        "}\n" +
        "function category(tid, pg, filter, extend) {\n" +
        "    var ext = extend || {};\n" +
        "    ext.page = parseInt(pg) || 1;\n" +
        "    var path = buildCategoryUrl(tid, ext);\n" +
        "    var html = fetchHtml(API_HOST + path);\n" +
        "    var list = extractVods(html);\n" +
        "    var pagecount = 1;\n" +
        "    var pm = html.match(/<a[^>]*href=\"[^\"]*---(\\d+)---[^\"]*\"[^>]*>(\\d+)<\\/a>/g);\n" +
        "    if (pm) {\n" +
        "        var nums = [];\n" +
        "        for (var i = 0; i < pm.length; i++) {\n" +
        "            var mm = pm[i].match(/---(\\d+)---/);\n" +
        "            if (mm) nums.push(parseInt(mm[1]));\n" +
        "        }\n" +
        "        if (nums.length > 0) pagecount = Math.max.apply(null, nums);\n" +
        "    }\n" +
        "    return JSON.stringify({ page: parseInt(pg) || 1, list: list, pagecount: pagecount, limit: 20, total: pagecount * 20 });\n" +
        "}\n" +
        "function detail(id) {\n" +
        "    var url = (id.indexOf('http') === 0) ? id : fixUrl(id);\n" +
        "    var html = fetchHtml(url);\n" +
        "    if (!html) return JSON.stringify({ list: [] });\n" +
        "    var info = parseDetail(html, id);\n" +
        "    return JSON.stringify({ list: [info] });\n" +
        "}\n" +
        "function search(wd, quick, pg) {\n" +
        "    var url = API_HOST + '/search/' + encodeURIComponent(wd) + '-------------.html';\n" +
        "    var html = fetchHtml(url);\n" +
        "    var list = extractVods(html);\n" +
        "    return JSON.stringify({ list: list, page: 1, pagecount: 1 });\n" +
        "}\n" +
        "function play(flag, id, flags) {\n" +
        "    if (id && /\\.(m3u8|mp4|flv|mkv|webm|ts)/i.test(id)) return JSON.stringify({ parse: 0, url: id });\n" +
        "    var url = (id.indexOf('http') === 0) ? id : fixUrl(id);\n" +
        "    var html = fetchHtml(url);\n" +
        "    var r = parsePlay(html, url);\n" +
        "    return JSON.stringify(r);\n" +
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
                    header.put("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36");
                    header.put("Referer", "https://www.dushehub.com/");
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
                SpiderDebug.log("DuShe init OK");
            } else {
                SpiderDebug.log("DuShe: 导出失败");
            }
        } catch (Exception e) {
            SpiderDebug.log("DuShe init FAIL: " + e.getMessage());
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