package com.github.catvod.spider;

import android.content.Context;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Filter;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NsVod extends Spider {

    private static final String JS_CODE =
        "var HOST = 'https://nsvod.cc';\n" +
        "var SALT = 'DCC147D11943AF75';\n" +
        "var UA_MB = 'Mozilla/5.0 (Linux; Android 13; SM-G9910) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36';\n" +

        // ============================================================
        // MD5
        // ============================================================
        "function md5(s) {\n" +
        "    function rl(n,c){return (n<<c)|(n>>>(32-c));}\n" +
        "    function au(x,y){var l=(x&0xFFFF)+(y&0xFFFF);var m=(x>>16)+(y>>16)+(l>>16);return (m<<16)|(l&0xFFFF);}\n" +
        "    function cmn(q,a,b,x,s,t){return au(rl(au(au(a,q),au(x,t)),s),b);}\n" +
        "    function ff(a,b,c,d,x,s,t){return cmn((b&c)|(~b&d),a,b,x,s,t);}\n" +
        "    function gg(a,b,c,d,x,s,t){return cmn((b&d)|(c&~d),a,b,x,s,t);}\n" +
        "    function hh(a,b,c,d,x,s,t){return cmn(b^c^d,a,b,x,s,t);}\n" +
        "    function ii(a,b,c,d,x,s,t){return cmn(c^(b|~d),a,b,x,s,t);}\n" +
        "    function sb(s){var i,blks=[],str=unescape(encodeURIComponent(s));for(i=0;i<str.length*8;i+=8)blks[i>>5]|=(str.charCodeAt(i/8)&0xFF)<<(i%32);blks[str.length*8>>5]|=0x80<<(str.length*8%32);blks[(((str.length*8+64)>>>9)<<4)+14]=str.length*8;return blks;}\n" +
        "    function hc(x){var h='0123456789abcdef',i,s='';for(i=0;i<x.length*4;i++)s+=h.charAt((x[i>>2]>>((i%4)*8+4))&0xF)+h.charAt((x[i>>2]>>((i%4)*8))&0xF);return s;}\n" +
        "    var x=sb(s),a=1732584193,b=-271733879,c=-1732584194,d=271733878;\n" +
        "    for(var i=0;i<x.length;i+=16){\n" +
        "        var oa=a,ob=b,oc=c,od=d;\n" +
        "        a=ff(a,b,c,d,x[i],7,-680876936);d=ff(d,a,b,c,x[i+1],12,-389564586);c=ff(c,d,a,b,x[i+2],17,606105819);b=ff(b,c,d,a,x[i+3],22,-1044525330);\n" +
        "        a=ff(a,b,c,d,x[i+4],7,-176418897);d=ff(d,a,b,c,x[i+5],12,1200080426);c=ff(c,d,a,b,x[i+6],17,-1473231341);b=ff(b,c,d,a,x[i+7],22,-45705983);\n" +
        "        a=ff(a,b,c,d,x[i+8],7,1770035416);d=ff(d,a,b,c,x[i+9],12,-1958414417);c=ff(c,d,a,b,x[i+10],17,-42063);b=ff(b,c,d,a,x[i+11],22,-1990404162);\n" +
        "        a=ff(a,b,c,d,x[i+12],7,1804603682);d=ff(d,a,b,c,x[i+13],12,-40341101);c=ff(c,d,a,b,x[i+14],17,-1502002290);b=ff(b,c,d,a,x[i+15],22,1236535329);\n" +
        "        a=gg(a,b,c,d,x[i+1],5,-165796510);d=gg(d,a,b,c,x[i+6],9,-1069501632);c=gg(c,d,a,b,x[i+11],14,643717713);b=gg(b,c,d,a,x[i],20,-373897302);\n" +
        "        a=gg(a,b,c,d,x[i+5],5,-701558691);d=gg(d,a,b,c,x[i+10],9,38016083);c=gg(c,d,a,b,x[i+15],14,-660478335);b=gg(b,c,d,a,x[i+4],20,-405537848);\n" +
        "        a=gg(a,b,c,d,x[i+9],5,568446438);d=gg(d,a,b,c,x[i+14],9,-1019803690);c=gg(c,d,a,b,x[i+3],14,-187363961);b=gg(b,c,d,a,x[i+8],20,1163531501);\n" +
        "        a=gg(a,b,c,d,x[i+13],5,-1444681467);d=gg(d,a,b,c,x[i+2],9,-51403784);c=gg(c,d,a,b,x[i+7],14,1735328473);b=gg(b,c,d,a,x[i+12],20,-1926607734);\n" +
        "        a=hh(a,b,c,d,x[i+5],4,-378558);d=hh(d,a,b,c,x[i+8],11,-2022574463);c=hh(c,d,a,b,x[i+11],16,1839030562);b=hh(b,c,d,a,x[i+14],23,-35309556);\n" +
        "        a=hh(a,b,c,d,x[i+1],4,-1530992060);d=hh(d,a,b,c,x[i+4],11,1272893353);c=hh(c,d,a,b,x[i+7],16,-155497632);b=hh(b,c,d,a,x[i+10],23,-1094730640);\n" +
        "        a=hh(a,b,c,d,x[i+13],4,681279174);d=hh(d,a,b,c,x[i],11,-358537222);c=hh(c,d,a,b,x[i+3],16,-722521979);b=hh(b,c,d,a,x[i+6],23,76029189);\n" +
        "        a=hh(a,b,c,d,x[i+9],4,-640364487);d=hh(d,a,b,c,x[i+12],11,-421815835);c=hh(c,d,a,b,x[i+15],16,530742520);b=hh(b,c,d,a,x[i+2],23,-995338651);\n" +
        "        a=ii(a,b,c,d,x[i],6,-198630844);d=ii(d,a,b,c,x[i+7],10,1126891415);c=ii(c,d,a,b,x[i+14],15,-1416354905);b=ii(b,c,d,a,x[i+5],21,-57434055);\n" +
        "        a=ii(a,b,c,d,x[i+12],6,1700485571);d=ii(d,a,b,c,x[i+3],10,-1894986606);c=ii(c,d,a,b,x[i+10],15,-1051523);b=ii(b,c,d,a,x[i+1],21,-2054922799);\n" +
        "        a=ii(a,b,c,d,x[i+8],6,1873313359);d=ii(d,a,b,c,x[i+15],10,-30611744);c=ii(c,d,a,b,x[i+6],15,-1560198380);b=ii(b,c,d,a,x[i+13],21,1309151649);\n" +
        "        a=ii(a,b,c,d,x[i+4],6,-145523070);d=ii(d,a,b,c,x[i+11],10,-1120210379);c=ii(c,d,a,b,x[i+2],15,718787259);b=ii(b,c,d,a,x[i+9],21,-343485551);\n" +
        "        a=au(a,oa);b=au(b,ob);c=au(c,oc);d=au(d,od);\n" +
        "    }\n" +
        "    return hc([a,b,c,d]);\n" +
        "}\n" +

        // ============================================================
        // 工具
        // ============================================================
        "function fixUrl(u) {\n" +
        "    if (!u) return '';\n" +
        "    u = String(u).replace(/\\\\\\//g, '/').trim();\n" +
        "    if (u.indexOf('//') === 0) return 'https:' + u;\n" +
        "    if (u.indexOf('http') === 0) return u;\n" +
        "    if (u.indexOf('/') === 0) return HOST + u;\n" +
        "    return HOST + '/' + u;\n" +
        "}\n" +
        "function stripTags(s) {\n" +
        "    return s ? String(s).replace(/<[^>]+>/g,'').replace(/&nbsp;/g,' ').replace(/\\s+/g,' ').trim() : '';\n" +
        "}\n" +
        "function decodeHtml(s) {\n" +
        "    if (!s) return '';\n" +
        "    return String(s).replace(/&amp;/g,'&').replace(/&lt;/g,'<').replace(/&gt;/g,'>')\n" +
        "        .replace(/&quot;/g,'\"').replace(/&#39;/g,\"'\").replace(/&nbsp;/g,' ');\n" +
        "}\n" +

        // ============================================================
        // 请求
        // ============================================================
        "function fetchHtml(url, opts) {\n" +
        "    try {\n" +
        "        opts = opts || {};\n" +
        "        var res = req(url, opts);\n" +
        "        return (res && res.content) ? res.content : '';\n" +
        "    } catch (e) { return ''; }\n" +
        "}\n" +

        // ============================================================
        // 列表解析
        // ============================================================
        "function parseHtmlList(html) {\n" +
        "    var list = [];\n" +
        "    if (!html) return list;\n" +
        "    var re = /<div class=\"public-list-box[^\"]*\">([\\s\\S]*?)<\\/div>\\s*<\\/div>\\s*<\\/div>/g;\n" +
        "    var m;\n" +
        "    while ((m = re.exec(html)) !== null) {\n" +
        "        var block = m[1];\n" +
        "        var idM = block.match(/href=\"\\/voddetail\\/(\\d+)\\.html\"/);\n" +
        "        if (!idM) continue;\n" +
        "        var id = idM[1];\n" +
        "        var nameM = block.match(/<a[^>]*class=\"time-title[^\"]*\"[^>]*title=\"([^\"]+)\"/) || block.match(/<a[^>]*class=\"time-title[^\"]*\"[^>]*>([^<]+)<\\/a>/);\n" +
        "        var name = nameM ? decodeHtml(nameM[1].trim()) : '';\n" +
        "        var picM = block.match(/data-src=\"([^\"]+)\"/) || block.match(/src=\"([^\"]+)\"/);\n" +
        "        var pic = (picM && picM[1] && picM[1].indexOf('data:image') < 0) ? fixUrl(picM[1]) : '';\n" +
        "        var remM = block.match(/<span class=\"public-list-prb[^\"]*\"[^>]*>([^<]+)<\\/span>/) || block.match(/<div class=\"public-list-subtitle[^\"]*\"[^>]*>([^<]*)<\\/div>/);\n" +
        "        var remark = remM ? stripTags(remM[1]).slice(0, 30) : '';\n" +
        "        list.push({ vod_id: id, vod_name: name, vod_pic: pic, vod_remarks: remark });\n" +
        "    }\n" +
        "    return list;\n" +
        "}\n" +

        // ============================================================
        // 首页（含筛选）
        // ============================================================
        "function init(cfg) { return true; }\n" +

        "function home(filter) {\n" +
        "    var classes = [\n" +
        "        { type_id: '1',  type_name: '电影' },\n" +
        "        { type_id: '2',  type_name: '连续剧' },\n" +
        "        { type_id: '3',  type_name: '综艺' },\n" +
        "        { type_id: '4',  type_name: '动漫' },\n" +
        "        { type_id: '41', type_name: '短剧' },\n" +
        "        { type_id: '40', type_name: '纪录片' },\n" +
        "        { type_id: '37', type_name: 'Netflix' }\n" +
        "    ];\n" +

        // 年份
        "    var YEARS = [{v:'',n:'全部'}];\n" +
        "    for (var y = 2026; y >= 2014; y--) YEARS.push({v: String(y), n: String(y)});\n" +

        // 地区
        "    var AREAS = [\n" +
        "        {v:'',n:'全部'},{v:'大陆',n:'大陆'},{v:'香港',n:'香港'},{v:'台湾',n:'台湾'},\n" +
        "        {v:'美国',n:'美国'},{v:'日本',n:'日本'},{v:'韩国',n:'韩国'},{v:'泰国',n:'泰国'},\n" +
        "        {v:'英国',n:'英国'},{v:'法国',n:'法国'},{v:'其他',n:'其他'}\n" +
        "    ];\n" +

        // 语言
        "    var LANGS = [\n" +
        "        {v:'',n:'全部'},{v:'国语',n:'国语'},{v:'英语',n:'英语'},\n" +
        "        {v:'粤语',n:'粤语'},{v:'韩语',n:'韩语'},{v:'日语',n:'日语'},{v:'其它',n:'其它'}\n" +
        "    ];\n" +

        // 排序
        "    var ORDERS = [\n" +
        "        {v:'time',n:'最新'},{v:'hits',n:'最热'},{v:'score',n:'评分'}\n" +
        "    ];\n" +

        // 各分类类型
        "    var MOVIE = [\n" +
        "        {v:'',n:'全部'},{v:'动作',n:'动作'},{v:'喜剧',n:'喜剧'},{v:'爱情',n:'爱情'},\n" +
        "        {v:'科幻',n:'科幻'},{v:'恐怖',n:'恐怖'},{v:'剧情',n:'剧情'},{v:'战争',n:'战争'},\n" +
        "        {v:'犯罪',n:'犯罪'},{v:'动画',n:'动画'},{v:'奇幻',n:'奇幻'},{v:'武侠',n:'武侠'},\n" +
        "        {v:'悬疑',n:'悬疑'},{v:'惊悚',n:'惊悚'},{v:'古装',n:'古装'}\n" +
        "    ];\n" +
        "    var TV = [\n" +
        "        {v:'',n:'全部'},{v:'古装',n:'古装'},{v:'战争',n:'战争'},{v:'喜剧',n:'喜剧'},\n" +
        "        {v:'家庭',n:'家庭'},{v:'犯罪',n:'犯罪'},{v:'动作',n:'动作'},{v:'奇幻',n:'奇幻'},\n" +
        "        {v:'剧情',n:'剧情'},{v:'历史',n:'历史'},{v:'网剧',n:'网剧'}\n" +
        "    ];\n" +
        "    var VARIETY = [\n" +
        "        {v:'',n:'全部'},{v:'选秀',n:'选秀'},{v:'情感',n:'情感'},{v:'访谈',n:'访谈'},\n" +
        "        {v:'旅游',n:'旅游'},{v:'音乐',n:'音乐'},{v:'美食',n:'美食'},{v:'生活',n:'生活'}\n" +
        "    ];\n" +
        "    var ANIME = [\n" +
        "        {v:'',n:'全部'},{v:'情感',n:'情感'},{v:'科幻',n:'科幻'},{v:'热血',n:'热血'},\n" +
        "        {v:'搞笑',n:'搞笑'},{v:'冒险',n:'冒险'},{v:'动作',n:'动作'},{v:'亲子',n:'亲子'},\n" +
        "        {v:'励志',n:'励志'}\n" +
        "    ];\n" +

        // 组装
        "    var filters = {\n" +
        "        '1': [\n" +
        "            { key: 'class', name: '类型', value: MOVIE },\n" +
        "            { key: 'area',  name: '地区', value: AREAS },\n" +
        "            { key: 'year',  name: '年份', value: YEARS },\n" +
        "            { key: 'lang',  name: '语言', value: LANGS },\n" +
        "            { key: 'by',    name: '排序', value: ORDERS }\n" +
        "        ],\n" +
        "        '2': [\n" +
        "            { key: 'class', name: '类型', value: TV },\n" +
        "            { key: 'area',  name: '地区', value: AREAS },\n" +
        "            { key: 'year',  name: '年份', value: YEARS },\n" +
        "            { key: 'lang',  name: '语言', value: LANGS },\n" +
        "            { key: 'by',    name: '排序', value: ORDERS }\n" +
        "        ],\n" +
        "        '3': [\n" +
        "            { key: 'class', name: '类型', value: VARIETY },\n" +
        "            { key: 'area',  name: '地区', value: AREAS },\n" +
        "            { key: 'year',  name: '年份', value: YEARS },\n" +
        "            { key: 'by',    name: '排序', value: ORDERS }\n" +
        "        ],\n" +
        "        '4': [\n" +
        "            { key: 'class', name: '类型', value: ANIME },\n" +
        "            { key: 'area',  name: '地区', value: AREAS },\n" +
        "            { key: 'year',  name: '年份', value: YEARS },\n" +
        "            { key: 'by',    name: '排序', value: ORDERS }\n" +
        "        ]\n" +
        "    };\n" +

        "    return JSON.stringify({ class: classes, filters: filters });\n" +
        "}\n" +

        "function homeVod() {\n" +
        "    var html = fetchHtml(HOST + '/');\n" +
        "    var list = parseHtmlList(html);\n" +
        "    return JSON.stringify({ list: list.slice(0, 30) });\n" +
        "}\n" +

        // ============================================================
        // 分类（POST + 签名）
        // ============================================================
        "function category(tid, pg, filter, extend) {\n" +
        "    try {\n" +
        "        var page = parseInt(pg) || 1;\n" +
        "        var cls = (extend && extend['class']) || '';\n" +
        "        var area = (extend && extend['area']) || '';\n" +
        "        var year = (extend && extend['year']) || '';\n" +
        "        var lang = (extend && extend['lang']) || '';\n" +
        "        var by = (extend && extend['by']) || '';\n" +
        "        if (cls === '全部' || cls === '不限') cls = '';\n" +
        "        if (area === '全部') area = '';\n" +
        "        if (year === '全部') year = '';\n" +
        "        var t = Math.floor(Date.now() / 1000);\n" +
        "        var key = md5('DS' + t + SALT);\n" +
        "        var parts = ['type=' + tid];\n" +
        "        parts.push('class=' + encodeURIComponent(cls));\n" +
        "        parts.push('area=' + encodeURIComponent(area));\n" +
        "        parts.push('lang=' + encodeURIComponent(lang));\n" +
        "        if (year) parts.push('year=' + encodeURIComponent(year));\n" +
        "        parts.push('version=');\n" +
        "        parts.push('state=');\n" +
        "        parts.push('letter=');\n" +
        "        if (by) parts.push('order=' + by);\n" +
        "        parts.push('page=' + page);\n" +
        "        parts.push('time=' + t);\n" +
        "        parts.push('key=' + key);\n" +
        "        var body = parts.join('&');\n" +
        "        var resp = fetchHtml(HOST + '/index.php/api/vod', {\n" +
        "            method: 'POST',\n" +
        "            body: body,\n" +
        "            headers: {\n" +
        "                'User-Agent': UA_MB,\n" +
        "                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',\n" +
        "                'X-Requested-With': 'XMLHttpRequest',\n" +
        "                'Referer': HOST + '/vodshow/' + tid + '-----------.html'\n" +
        "            }\n" +
        "        });\n" +
        "        var data = JSON.parse(resp || '{}');\n" +
        "        var list = (data.list || []).map(function(it) {\n" +
        "            return { vod_id: String(it.vod_id), vod_name: it.vod_name || '', vod_pic: fixUrl(it.vod_pic || ''), vod_remarks: it.vod_remarks || '' };\n" +
        "        });\n" +
        "        return JSON.stringify({ page: page, list: list, pagecount: data.pagecount || 1, limit: data.limit || 40, total: data.total || 0 });\n" +
        "    } catch (e) {\n" +
        "        return JSON.stringify({ page: parseInt(pg)||1, list: [], pagecount: 1, limit: 40, total: 0 });\n" +
        "    }\n" +
        "}\n" +

        // ============================================================
        // 详情
        // ============================================================
        "function detail(id) {\n" +
        "    try {\n" +
        "        var html = fetchHtml(HOST + '/voddetail/' + id + '.html');\n" +
        "        if (!html) return JSON.stringify({ list: [] });\n" +
        "        var info = { vod_id: String(id), vod_name: '', vod_pic: '', vod_content: '', vod_play_from: '', vod_play_url: '' };\n" +
        "        var tM = html.match(/<h3 class=\"slide-info-title[^\"]*\"[^>]*>([^<]+)<\\/h3>/);\n" +
        "        if (tM) info.vod_name = decodeHtml(tM[1].trim());\n" +
        "        var pM = html.match(/class=\"lazy lazy1 mask-1[^\"]*\"[^>]*(?:data-src|src)=\"([^\"]+)\"/);\n" +
        "        if (pM) info.vod_pic = fixUrl(pM[1]);\n" +
        "        var dM = html.match(/<div id=\"height_limit\"[^>]*class=\"text[^\"]*\"[^>]*>([\\s\\S]*?)<\\/div>/);\n" +
        "        if (dM) info.vod_content = stripTags(dM[1]);\n" +

        // 线路名
        "        var names = [];\n" +
        "        var tRe = /<a[^>]*class=\"[^\"]*swiper-slide[^\"]*\"[^>]*>[\\s\\S]*?<\\/a>/g;\n" +
        "        var tm;\n" +
        "        while ((tm = tRe.exec(html)) !== null) {\n" +
        "            var nm = tm[0].replace(/^<a[^>]*>/,'').replace(/<\\/a>\\s*$/,'')\n" +
        "                .replace(/<i[^>]*>[\\s\\S]*?<\\/i>/g,'').replace(/<span[^>]*class=\"badge\"[^>]*>[\\s\\S]*?<\\/span>/g,'')\n" +
        "                .replace(/<[^>]+>/g,'').replace(/&nbsp;/g,' ').replace(/\\s+/g,' ').trim();\n" +
        "            if (nm && names.indexOf(nm) < 0) names.push(nm);\n" +
        "        }\n" +
        "        if (names.length === 0) names.push('线路1');\n" +

        // 剧集
        "        var from = [];\n" +
        "        var url = [];\n" +
        "        var bRe = /<div class=\"anthology-list-box[^\"]*\"[^>]*>([\\s\\S]*?)<\\/ul>\\s*<\\/div>\\s*<\\/div>/g;\n" +
        "        var bm;\n" +
        "        var idx = 0;\n" +
        "        while ((bm = bRe.exec(html)) !== null) {\n" +
        "            var body = bm[1];\n" +
        "            var eps = [];\n" +
        "            var aRe = /<a[^>]*href=\"([^\"]+)\"[^>]*>([^<]+)<\\/a>/g;\n" +
        "            var am;\n" +
        "            while ((am = aRe.exec(body)) !== null) {\n" +
        "                if (am[1].indexOf('/vodplay/') >= 0) eps.push(am[2].trim() + '$' + fixUrl(am[1]));\n" +
        "            }\n" +
        "            if (eps.length > 0) {\n" +
        "                from.push(names[idx] || ('线路' + (idx + 1)));\n" +
        "                url.push(eps.join('#'));\n" +
        "                idx++;\n" +
        "            }\n" +
        "        }\n" +
        "        info.vod_play_from = from.join('$$$');\n" +
        "        info.vod_play_url = url.join('$$$');\n" +
        "        return JSON.stringify({ list: [info] });\n" +
        "    } catch (e) {\n" +
        "        return JSON.stringify({ list: [] });\n" +
        "    }\n" +
        "}\n" +

        // ============================================================
        // 搜索
        // ============================================================
        "function search(wd, quick, pg) {\n" +
        "    try {\n" +
        "        var page = parseInt(pg) || 1;\n" +
        "        var url = HOST + '/vodsearch/-------------.html?wd=' + encodeURIComponent(wd);\n" +
        "        if (page > 1) url += '&page=' + page;\n" +
        "        var html = fetchHtml(url);\n" +
        "        return JSON.stringify({ list: parseHtmlList(html), page: page, pagecount: 1 });\n" +
        "    } catch (e) {\n" +
        "        return JSON.stringify({ list: [] });\n" +
        "    }\n" +
        "}\n" +

        // ============================================================
        // 播放（直链 → jxapi → artplayer 嗅探）
        // ============================================================
        "function play(flag, id, flags) {\n" +
        "    try {\n" +
        // ① 入参直链
        "        if (id && /\\.(m3u8|mp4|flv|mkv|webm|ts)(\\?|$)/i.test(id)) {\n" +
        "            return JSON.stringify({ parse: 0, url: id, header: { 'User-Agent': UA_MB, 'Accept': '*/*' } });\n" +
        "        }\n" +
        // ② 抓播放页
        "        var pageUrl = id.indexOf('http') === 0 ? id : HOST + id;\n" +
        "        var html = fetchHtml(pageUrl);\n" +
        "        if (!html || html.indexOf('player_aaaa') < 0) html = fetchHtml(pageUrl);\n" +
        "        if (!html) return JSON.stringify({ parse: 0, url: '' });\n" +
        // ③ 抠 player_aaaa
        "        var realUrl = '', from = '';\n" +
        "        var pm = html.match(/var\\s+player_aaaa\\s*=\\s*(\\{[\\s\\S]*?\\})\\s*<\\/script>/);\n" +
        "        if (pm) {\n" +
        "            try {\n" +
        "                var p = JSON.parse(pm[1]);\n" +
        "                if (p.url) { realUrl = p.url.replace(/\\\\\\//g,'/'); if (realUrl.indexOf('//')===0) realUrl='https:'+realUrl; }\n" +
        "                if (p.from) from = p.from;\n" +
        "            } catch (e) {}\n" +
        "        }\n" +
        // ④ 是 m3u8 → 直连
        "        if (realUrl && /\\.(m3u8|mp4|flv)(\\?|$)/i.test(realUrl)) {\n" +
        "            return JSON.stringify({ parse: 0, url: realUrl, header: { 'User-Agent': UA_MB, 'Accept': '*/*' } });\n" +
        "        }\n" +
        // ⑤ 第三方源 → 抠 jxapi
        "        if (realUrl) {\n" +
        "            var cfgM = html.match(/src=\"([^\"]*playerconfig\\.js[^\"]*)\"/);\n" +
        "            var cfgUrl = cfgM ? cfgM[1] : '/static/js/playerconfig.js';\n" +
        "            if (cfgUrl.indexOf('//') === 0) cfgUrl = 'https:' + cfgUrl;\n" +
        "            else if (cfgUrl.indexOf('/') === 0) cfgUrl = HOST + cfgUrl;\n" +
        "            var cfgJs = fetchHtml(cfgUrl);\n" +
        "            var jxM = cfgJs ? cfgJs.match(/\"parse\"\\s*:\\s*\"([^\"]*jxapi[^\"]*)\"/) : null;\n" +
        "            if (jxM) {\n" +
        "                var tpl = jxM[1].replace(/\\\\\\//g,'/');\n" +
        "                if (/[?&]player(&|$)/.test(tpl)) tpl = tpl.replace(/([?&])player(&|$)/, '$1from=player$2');\n" +
        "                else tpl += (tpl.indexOf('?') > -1 ? '&' : '?') + 'from=player';\n" +
        "                var apiUrl = tpl + encodeURIComponent(realUrl);\n" +
        "                var resp = fetchHtml(apiUrl, { headers: { 'User-Agent': UA_MB, 'Accept': 'application/json,*/*', 'Referer': HOST + '/' } });\n" +
        "                if (resp) {\n" +
        "                    try {\n" +
        "                        var j = JSON.parse(resp);\n" +
        "                        if (j && j.code === 200 && j.url) {\n" +
        "                            var vu = j.url.replace(/\\\\\\//g,'/');\n" +
        "                            return JSON.stringify({ parse: 0, url: vu, header: { 'User-Agent': UA_MB, 'Accept': '*/*' } });\n" +
        "                        }\n" +
        "                    } catch (e) {}\n" +
        "                }\n" +
        "            }\n" +
        // ⑥ artplayer → 嗅探
        "            var artM = cfgJs ? cfgJs.match(/\"parse\"\\s*:\\s*\"([^\"]*artplayer\\.html[^\"]*)\"/) : null;\n" +
        "            if (artM) {\n" +
        "                var at = artM[1].replace(/\\\\\\//g,'/');\n" +
        "                if (!/[?&]url=$/.test(at)) at += (at.indexOf('?') > -1 ? '&url=' : '?url=');\n" +
        "                return JSON.stringify({ parse: 1, url: at + encodeURIComponent(realUrl), header: { 'User-Agent': UA_MB, 'Referer': HOST + '/' } });\n" +
        "            }\n" +
        "        }\n" +
        "        return JSON.stringify({ parse: 0, url: '' });\n" +
        "    } catch (e) {\n" +
        "        return JSON.stringify({ parse: 0, url: '' });\n" +
        "    }\n" +
        "}\n" +

        // ============================================================
        // 导出
        // ============================================================
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

            // req() 支持 GET / POST
            ctx.getGlobalObject().setProperty("req", args -> {
                JSObject ret = ctx.createNewJSObject();
                try {
                    String url = (args == null || args.length == 0 || args[0] == null)
                            ? "" : args[0].toString();
                    String method = "GET";
                    String body = null;
                    Map<String, String> header = new HashMap<>();
                    header.put("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G9910) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
                    header.put("Referer", "https://nsvod.cc/");

                    if (args.length > 1 && args[1] instanceof JSObject) {
                        JSObject opts = (JSObject) args[1];
                        Object mObj = opts.getProperty("method");
                        if (mObj != null) method = mObj.toString();
                        Object bObj = opts.getProperty("body");
                        if (bObj != null) body = bObj.toString();
                        Object hObj = opts.getProperty("headers");
                        if (hObj instanceof JSObject) {
                            JSObject h = (JSObject) hObj;
                            // 遍历 headers
                            String[] keys = h.getOwnPropertyNames();
                            if (keys != null) {
                                for (String k : keys) {
                                    Object v = h.getProperty(k);
                                    if (v != null) header.put(k, v.toString());
                                }
                            }
                        }
                    }

                    String content;
                    if ("POST".equalsIgnoreCase(method)) {
                        content = OkHttp.post(url, body == null ? "" : body, header);
                    } else {
                        content = OkHttp.string(url, header);
                    }
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
                SpiderDebug.log("NaiShi init OK");
            } else {
                SpiderDebug.log("NaiShi: 导出失败");
            }
        } catch (Exception e) {
            SpiderDebug.log("NaiShi init FAIL: " + e.getMessage());
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

    // ============================================================
    // TVBox 接口实现
    // ============================================================

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

            // ★ 解析 filters
            LinkedHashMap<String, List<Filter>> filters = new LinkedHashMap<>();
            JSONObject fObj = obj.optJSONObject("filters");
            if (fObj != null) {
                Iterator<String> keys = fObj.keys();
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
        if (extend != null)