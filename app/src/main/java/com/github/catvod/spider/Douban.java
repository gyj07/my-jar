/*
@header({
  searchable: 0,
  filterable: 0,
  quickSearch: 0,
  title: '豆瓣推荐',
  lang: 'cat'
})
*/

package com.github.catvod.spider;

import android.text.TextUtils;

import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Douban extends Spider {

    private String siteName = "豆瓣推荐";
    private String siteKey = "";
    private int siteType = 0;

    private static final String UA = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36 MicroMessenger/7.0.9.501 NetType/WIFI MiniProgramEnv/Windows WindowsWechat";
    private static final String API_KEY = "0ac44ae016490db2204ce0a042db2916";

    private Map<String, String> getHeaders() {
        Map<String, String> h = new HashMap<>();
        h.put("Host", "frodo.douban.com");
        h.put("Connection", "Keep-Alive");
        h.put("Referer", "https://servicewechat.com/wx2f9b06c1de1ccfca/84/page-frame.html");
        h.put("User-Agent", UA);
        return h;
    }

    // ============================================================
    // request（对应 JS 的 request 函数）
    // ============================================================
    private String request(String url) {
        try {
            String body = OkHttp.string(url, getHeaders());
            return body == null ? "" : body;
        } catch (Exception e) {
            return null;
        }
    }

    // ============================================================
    // init（对应 JS 的 init）
    // ============================================================
    @Override
    public void init(android.content.Context context, String extend) {
        // JS: siteName = cfg.skey?.split("_")[1] || cfg.skey || "豆瓣推荐"
        // Java 里 extend 是 cfg.skey，简单处理
        if (!TextUtils.isEmpty(extend)) {
            siteKey = extend;
            String[] parts = extend.split("_");
            siteName = parts.length > 1 ? parts[1] : extend;
        }
    }

    // ============================================================
    // home（对应 JS 的 home）
    // ============================================================
    @Override
    public String homeContent(boolean filter) {
        try {
            JSONObject result = new JSONObject();
            JSONArray classes = new JSONArray();

            String[][] cfg = {
                {"hot_gaia", "热门电影"},
                {"tv_hot", "热播剧集"},
                {"show_hot", "热播综艺"}
            };
            for (String[] c : cfg) {
                JSONObject o = new JSONObject();
                o.put("type_id", c[0]);
                o.put("type_name", c[1]);
                classes.put(o);
            }
            result.put("class", classes);
            return result.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ============================================================
    // homeVod（对应 JS 的 homeVod）
    // ============================================================
    @Override
    public String homeVideoContent() {
        return categoryContent("hot_gaia", "1", false, null);
    }

    // ============================================================
    // category（对应 JS 的 category）
    // ============================================================
    @Override
    public String categoryContent(String tid, String pg, boolean filter,
                                  HashMap<String, String> extend) {
        try {
            int page = 1;
            try { page = Integer.parseInt(pg); } catch (Exception ignored) {}
            if (page <= 0) page = 1;

            int limit = 20;
            int start = (page - 1) * limit;

            String url = "";
            String listKey = "items";

            if ("hot_gaia".equals(tid)) {
                url = "https://frodo.douban.com/api/v2/movie/hot_gaia?apikey=" + API_KEY
                    + "&sort=recommend&area=" + URLEncoder.encode("全部", "UTF-8")
                    + "&start=" + start + "&count=" + limit;
                listKey = "items";
            } else if ("tv_hot".equals(tid)) {
                url = "https://frodo.douban.com/api/v2/subject_collection/tv_hot/items?apikey=" + API_KEY
                    + "&start=" + start + "&count=" + limit;
                listKey = "subject_collection_items";
            } else if ("show_hot".equals(tid)) {
                url = "https://frodo.douban.com/api/v2/subject_collection/show_hot/items?apikey=" + API_KEY
                    + "&start=" + start + "&count=" + limit;
                listKey = "subject_collection_items";
            } else {
                url = "https://frodo.douban.com/api/v2/movie/hot_gaia?apikey=" + API_KEY
                    + "&sort=recommend&area=" + URLEncoder.encode("全部", "UTF-8")
                    + "&start=" + start + "&count=" + limit;
                listKey = "items";
            }

            String html = request(url);
            JSONArray list = new JSONArray();

            if (!TextUtils.isEmpty(html)) {
                try {
                    JSONObject data = new JSONObject(html);
                    JSONArray items = data.optJSONArray(listKey);
                    if (items != null) {
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject item = items.optJSONObject(i);
                            if (item == null) continue;

                            JSONObject vod = new JSONObject();

                            // vod_pic
                            String vodPic = "";
                            JSONObject pic = item.optJSONObject("pic");
                            if (pic != null) {
                                vodPic = pic.optString("normal", "");
                                if (TextUtils.isEmpty(vodPic)) vodPic = pic.optString("large", "");
                            }
                            if (!TextUtils.isEmpty(vodPic)) {
                                vodPic = vodPic
                                    + "@Referer=https://api.douban.com/"
                                    + "@User-Agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36";
                            }

                            // vod_remarks
                            String vodRemarks = "";
                            JSONObject rating = item.optJSONObject("rating");
                            if (rating != null && rating.has("value") && !rating.isNull("value")) {
                                vodRemarks = "评分：" + rating.opt("value");
                            }

                            // vod_id
                            String vodId = "msearch:" + item.optString("id", "");

                            vod.put("vod_id", vodId);
                            vod.put("vod_name", item.optString("title", ""));
                            vod.put("vod_pic", vodPic);
                            vod.put("vod_remarks", vodRemarks);

                            // 过滤没图片的
                            if (TextUtils.isEmpty(vodPic)) continue;

                            list.put(vod);
                        }
                    }
                } catch (Exception e) {
                    SpiderDebug.log("豆瓣推荐解析错误: " + e.getMessage());
                }
            }

            JSONObject result = new JSONObject();
            result.put("page", page);
            result.put("pagecount", 999);
            result.put("limit", 20);
            result.put("total", 99999);
            result.put("list", list);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log("categoryContent error: " + e.getMessage());
            return "";
        }
    }

    // ============================================================
    // detail（对应 JS 的 detail）
    // ============================================================
    @Override
    public String detailContent(List<String> ids) {
        try {
            JSONObject result = new JSONObject();
            result.put("list", new JSONArray());
            return result.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ============================================================
    // play（对应 JS 的 play）
    // ============================================================
    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        try {
            JSONObject result = new JSONObject();
            result.put("parse", 0);
            result.put("url", "");
            return result.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ============================================================
    // search（对应 JS 的 search）
    // ============================================================
    @Override
    public String searchContent(String wd, boolean quick) {
        try {
            JSONObject result = new JSONObject();
            result.put("list", new JSONArray());
            return result.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String searchContent(String wd, boolean quick, String pg) {
        return searchContent(wd, quick);
    }

    @Override
    public void destroy() {
        SpiderDebug.log("Douban destroy");
    }
}