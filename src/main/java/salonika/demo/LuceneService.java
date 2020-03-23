package salonika.demo;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.IOUtils;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 每个文档由若干个 field:text 组成
 * 对于每个文档的操作：
 * - 新增文档
 * - - 文档必须有唯一标识，其他任意
 * - 修改文档
 * - - 文档唯一标识是判定修改的关键，其他为修改内容的可能
 * - 删除文档
 * - - 文档唯一标识是判定删除的关键，其他为删除的匹配可能
 * - 查询文档
 * - - 根据 关键词 在某个 field 中查询
 * - - query方式 关键词也会分词
 * - - term方式 关键词不会分词
 * FIXME [TODO.1] 索引数据存储在系统临时目录中，没有实现数据的恢复
 */
public class LuceneService implements IDocIndexService {

    public static final String FIELD_DOC_INDEX = "_index";
    public static final String FIELD_DOC_ID = "_id";
    public static final String FIELD_DOC_NAME = "_name";
    public static final String FIELD_DOC_CONTENT = "_content";

    /** 分词器 */
    private Analyzer analyzer;

    /** 暂存的索引目录 */
    public Map<String, TempDirectory> indexDirMap = new HashMap<>();

    @Data
    @AllArgsConstructor
    public static class TempDirectory{
        private Path path;
        private Directory directory;
    }

    /** constructor */
    public LuceneService(Analyzer analyzer){
        this.analyzer = analyzer;
    }

    /** 新建索引 */
    @Override
    public void newIndex(String indexName) throws IOException {
        Path indexPath = Files.createTempDirectory(indexName);
        Directory directory = FSDirectory.open(indexPath);
        indexDirMap.put(indexName, new TempDirectory(indexPath, directory));
    }

    /** 关闭索引并删除 */
    @Override
    public void close(String indexName) throws IOException {
        indexDirMap.get(indexName).getDirectory().close();
        IOUtils.rm(indexDirMap.get(indexName).getPath());
        indexDirMap.remove(indexName);
    }

    /** 关闭所有索引并删除 */
    @Override
    public void closeAll() throws IOException {
        for (TempDirectory td : indexDirMap.values()) {
            td.getDirectory().close();
            IOUtils.rm(td.getPath());
        }
        indexDirMap.clear();
    }

    /**
     * 添加文档, 文档名称具有唯一性（即其md5作为了id，后同）
     * 同一个名称会重复添加
     *
     * 所以使用saveDoc方法更具有实际意义
     */
    @Override
    public void addDoc(String indexName, String docName, String docContent) throws IOException {
        Directory directory = indexDirMap.get(indexName).getDirectory();

        try (IndexWriter iwriter = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
            Document doc = new Document();
            // 使用 StringField 让文本不分词，可用于名称等
            // 使用 TextField 让文本依照 analyzer 的类型分词，这是默认的实现
            // 使用 docName 的 md5 作为唯一标识
            doc.add(new StringField(FIELD_DOC_INDEX, indexName, Field.Store.YES));
            doc.add(new StringField(FIELD_DOC_ID, DigestUtils.md5DigestAsHex(docName.getBytes()), Field.Store.YES));
            doc.add(new TextField(FIELD_DOC_NAME, docName, Field.Store.YES));
            doc.add(new TextField(FIELD_DOC_CONTENT, docContent, Field.Store.YES));
            iwriter.addDocument(doc);
        }
    }

    /**
     * 添加未存在的文档或修改已存在的文档
     * 根据文档名称，先删除已有的文档，然后添加文档
     *
     * 采用简单方式，删除再添加；这里并没有使用lucene中IndexWriter的api
     */
    @Override
    public void saveDoc(String indexName, String docName, String docContent) throws IOException{
        delDoc(indexName, docName);
        addDoc(indexName, docName, docContent);
    }

    /**
     * 删除文档
     * 根据文档的名称md5作为的id删除
     */
    @Override
    public void delDoc(String indexName, String docName) throws IOException {
        Directory directory = indexDirMap.get(indexName).getDirectory();

        try (IndexWriter iwriter = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
            iwriter.deleteDocuments(new Term(FIELD_DOC_ID, DigestUtils.md5DigestAsHex(docName.getBytes())));
        }
    }

    /** 查询文档 */
    @Override
    public List<Document> searchDoc(String indexName, String keywords) throws IOException, ParseException {
        List<Document> result = new ArrayList<>();
        // ** 开始查询
        // Now search the index:
        try (DirectoryReader ireader = DirectoryReader.open(indexDirMap.get(indexName).getDirectory())) {
            IndexSearcher isearcher = new IndexSearcher(ireader);
            // 使用多字段查询器来匹配关键词
            QueryParser parser = new MultiFieldQueryParser(new String[]{FIELD_DOC_NAME, FIELD_DOC_CONTENT}, analyzer);
            Query query = parser.parse(keywords);
            ScoreDoc[] hits = isearcher.search(query, 10).scoreDocs;
            // Iterate through the results:
            for (ScoreDoc hit : hits) {
                Document hitDoc = isearcher.doc(hit.doc);
                result.add(hitDoc);
            }
        }
        return result;
    }

    /** 返回所有文档 */
    @Override
    public List<Document> allDocs(String indexName) throws IOException {
        List<Document> result = new ArrayList<>();
        // ** 开始查询
        // Now search the index:
        try (DirectoryReader ireader = DirectoryReader.open(indexDirMap.get(indexName).getDirectory())) {
            IndexSearcher isearcher = new IndexSearcher(ireader);
            // Iterate through the results:
            for (int i = 0; i < ireader.maxDoc(); i ++) {
                Document hitDoc = isearcher.doc(i);
                result.add(hitDoc);
            }
        }
        return result;
    }

}
