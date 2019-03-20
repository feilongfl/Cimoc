package com.hiroshi.cimoc.source;

import android.util.Pair;

import com.google.common.collect.Lists;
import com.hiroshi.cimoc.model.Chapter;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.ImageUrl;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.parser.MangaCategory;
import com.hiroshi.cimoc.parser.MangaParser;
import com.hiroshi.cimoc.parser.NodeIterator;
import com.hiroshi.cimoc.parser.SearchIterator;
import com.hiroshi.cimoc.parser.UrlFilter;
import com.hiroshi.cimoc.soup.Node;
import com.hiroshi.cimoc.utils.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by WinterWhisper on 2019/2/25.
 */
public class CCMH extends MangaParser {

    public static final int TYPE = 23;
    public static final String DEFAULT_TITLE = "CC漫画";

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    public CCMH(Source source) {
        init(source, null);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
        String url = "";
        if (page == 1) {
            url = "http://m.ccmh6.com/Search";
        }
        RequestBody requestBodyPost = new FormBody.Builder()
                .add("Key", keyword)
                .build();

        return new Request.Builder()
                .addHeader("Referer", "http://m.ccmh6.com/Search")
                .addHeader("Origin", "http://m.ccmh6.com")
                .addHeader("Host", "m.ccmh6.com")
                .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/12.0 Mobile/15A372 Safari/604.1")
                .url(url)
                .post(requestBodyPost)
                .build();

    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        Node body = new Node(html);
        return new NodeIterator(body.list("div.list > div > a")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.attr("href").replace("/manhua/", "");
                String cover = node.src("img");

                String[] titleAndAuthor = node.text().split("[连载|完结]");
                String title = titleAndAuthor[0];
                String author = titleAndAuthor[1];
                return new Comic(TYPE, cid, title, cover, null, author);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = StringUtils.format("http://m.ccmh6.com/manhua/%s", cid);
        return new Request.Builder()
                .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/12.0 Mobile/15A372 Safari/604.1")
                .url(url).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) {
        Node body = new Node(html);
        String intro = body.text("div.intro");
        String cover = body.src("div.cover > img");

        String title = body.text("div.other > div:eq(1) > strong");
        String author = body.text("div.other > div:eq(5) > span");
        boolean status = isFinish(body.text("div.other > div:eq(6) > span"));
        String update = body.text("div.other > div:eq(7) > span");
        comic.setInfo(title, cover, update, intro, author, status);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        Node body = new Node(html);
        for (Node node : body.list("div.list > a")) {
            String title = node.attr("title");
            String path = node.hrefWithSplit(2);
            list.add(0, new Chapter(title, path));
        }

        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("http://m.ccmh6.com/manhua/%s/%s.html", cid, path);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();

        int pageNum = Integer.parseInt(StringUtils.match("Jump\\((.*?),", html, 1));
        Node body = new Node(html);
        // 第一张图片的地址
        String firstAddr = body.text("div.img > img");
        firstAddr = firstAddr.substring(0, firstAddr.length() - 7);
        if (firstAddr != null) {
            try {
                for (int i = 1; i <= pageNum; i++) {
                    String lastNum = "";
                    if (i < 10) {
                        lastNum = "00" + i;
                    } else if (i < 100) {
                        lastNum = "0" + i;
                    } else {
                        lastNum = "" + i;
                    }
                    String url = firstAddr + lastNum + ".jpg";
                    list.add(new ImageUrl(i + 1, url, false));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return list;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        // 这里是更新时间
        return new Node(html).text("div.other > div:eq(7) > span");
    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "http://m.ccmh6.com/");
    }

}
