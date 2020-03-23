package salonika.demo;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.List;

/**
 * 文档索引接口
 */
public interface IDocIndexService {

    /** 新建索引 */
    void newIndex(String indexName) throws IOException;

    /** 关闭索引并删除 */
    void close(String indexName) throws IOException;

    /** 关闭所有索引并删除 */
    void closeAll() throws IOException;

    /** 添加文档 */
    void addDoc(String indexName, String docName, String docContent) throws IOException;

    /** 添加或修改文档 */
    void saveDoc(String indexName, String docName, String docContent) throws IOException;

    /** 删除文档 */
    void delDoc(String indexName, String docName) throws IOException;

    /** 查询文档 */
    List<Document> searchDoc(String indexName, String keywords) throws IOException, ParseException;

    /** 返回所有文档 */
    List<Document> allDocs(String indexName) throws IOException;
}
