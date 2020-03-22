package salonika.demo;


import javafx.util.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.IOUtils;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
 * 存储在系统临时目录中，没有实现数据的恢复
 */
public class LuceneService {

    public static final String FIELD_DOC_INDEX = "_index";
    public static final String FIELD_DOC_NAME = "_name";
    public static final String FIELD_DOC_CONTENT = "_content";

    /** 分词器 */
    private Analyzer analyzer;

    /** 暂存的索引目录 */
    public Map<String, Pair<Path, Directory>> indexDirMap = new HashMap<>();

    public LuceneService(Analyzer analyzer){
        this.analyzer = analyzer;
    }

    /** 新建索引 */
    public void newIndex(String indexName) throws IOException {
        Path indexPath = Files.createTempDirectory(indexName);
        Directory directory = FSDirectory.open(indexPath);
        indexDirMap.put(indexName, new Pair<>(indexPath, directory));
    }

    /** 关闭索引 */
    public void close(String indexName) throws IOException {
        indexDirMap.get(indexName).getValue().close();
        IOUtils.rm(indexDirMap.get(indexName).getKey());
        indexDirMap.remove(indexName);
    }

    /** 关闭所有索引并删除 */
    public void closeAll() throws IOException {
        for (Pair<Path, Directory> pd : indexDirMap.values()) {
            pd.getValue().close();
            IOUtils.rm(pd.getKey());
        }
        indexDirMap.clear();
    }

    /** 添加文档, 文档名称具有唯一性（后同），已存在时为修改 */
    public void saveDoc(String indexName, String docName, String docContent) throws IOException {
        Directory directory = indexDirMap.get(indexName).getValue();

        try (IndexWriter iwriter = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
            Document doc = new Document();

            doc.add(new Field(FIELD_DOC_INDEX, indexName, TextField.TYPE_STORED));
            doc.add(new Field(FIELD_DOC_NAME, docName, TextField.TYPE_STORED));
            doc.add(new Field(FIELD_DOC_CONTENT, docContent, TextField.TYPE_STORED));
            iwriter.addDocument(doc);
        }
    }

    /** 删除文档 */
    public void delDoc(String indexName, String docName) throws IOException {
        Directory directory = indexDirMap.get(indexName).getValue();

        try (IndexWriter iwriter = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
            iwriter.deleteDocuments(new Term("_name", docName));
        }
    }

    /** 查询文档 */
    public List<Document> searchDoc(String indexName, String keywords) throws IOException, ParseException {
        List<Document> result = new LinkedList<>();
        // ** 开始查询
        // Now search the index:
        try (DirectoryReader ireader = DirectoryReader.open(indexDirMap.get(indexName).getValue())) {
            IndexSearcher isearcher = new IndexSearcher(ireader);
            // Parse a simple query that searches for "text":
            QueryParser parser = new QueryParser("_content", analyzer);
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

}
