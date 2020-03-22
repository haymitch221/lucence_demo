package salonika.test;

import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.testng.Assert;
import org.testng.annotations.Test;
import salonika.demo.LuceneService;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public class LuceneTests02 {

    @Test
    public void test01() throws IOException, ParseException {
        LuceneService lcs = new LuceneService(new SmartChineseAnalyzer());

        String indexName = "luceneIndex1";
        lcs.newIndex(indexName);
        lcs.saveDoc(indexName, "文档1", "凡尔纳之后，纽约时报评价最好看的环球冒险小说");
        lcs.saveDoc(indexName, "文档1", "这本小说英文版上市一周即空间纽约时报畅销书青少年排行榜冒险类第5名");
        List<Document> docs1 = lcs.searchDoc(indexName, "环球");
        List<Document> docs2 = lcs.searchDoc(indexName, "环游");
        List<Document> docs3 = lcs.searchDoc(indexName, "环球电影");
        List<Document> docs4 = lcs.searchDoc(indexName, "凡尔纳");
        List<Document> docs5 = lcs.searchDoc(indexName, "凡尔赛");
        List<Document> docs6 = lcs.searchDoc(indexName, "环球冒险");
        List<Document> docs7 = lcs.searchDoc(indexName, "最好");
        List<Document> docs8 = lcs.searchDoc(indexName, "最好看");
        List<Document> docs9 = lcs.searchDoc(indexName, "最好看的");
        List<Document> docs10 = lcs.searchDoc(indexName, "好看的");
        List<Document> docs11 = lcs.searchDoc(indexName, "好看");
        List<Document> docs12 = lcs.searchDoc(indexName, "很好看");
        List<Document> docs13 = lcs.searchDoc(indexName, "纽约");
        List<Document> docs14 = lcs.searchDoc(indexName, "纽约上市");

        Assert.assertEquals(1, docs1.size());

        lcs.closeAll();
    }

}