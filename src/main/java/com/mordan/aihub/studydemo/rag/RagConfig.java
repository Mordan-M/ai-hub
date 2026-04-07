package com.mordan.aihub.studydemo.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "rag", name = "enable", havingValue = "true", matchIfMissing = false)
public class RagConfig {

    private static final Logger log = LoggerFactory.getLogger(RagConfig.class);

    // 向量库持久化文件路径
    @Value("${rag.store-file:embedding-store.json}")
    private String storeFile;

    // 文档目录路径
    @Value("${rag.docs-dir:src/main/resources/docs}")
    private String docsDir;

    // 每段最大字符数（接口限制 256，留余量给文件名前缀）
    @Value("${rag.max-segment-size:150}")
    private int maxSegmentSize;

    // 相邻段重叠字符数
    @Value("${rag.max-overlap-size:15}")
    private int maxOverlapSize;

    @Resource
    private EmbeddingModel embeddingModel;

    // 标志位：是否是首次创建（需要 ingest）
    private boolean needsIngest = false;

    // 保存引用，供 @PreDestroy 持久化使用
    private InMemoryEmbeddingStore<TextSegment> inMemoryEmbeddingStore;

    /**
     * 注册向量库 Bean
     * - 文件存在 → 直接加载，跳过 ingest
     * - 文件不存在 → 创建新向量库，标记需要 ingest
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        Path storePath = Path.of(storeFile);
        if (Files.exists(storePath)) {
            log.info("[RAG] 检测到向量库文件，从文件加载: {}", storePath.toAbsolutePath());
            inMemoryEmbeddingStore = InMemoryEmbeddingStore.fromFile(storePath);
            needsIngest = false;
        } else {
            log.info("[RAG] 未检测到向量库文件，创建新的内存向量库");
            inMemoryEmbeddingStore = new InMemoryEmbeddingStore<>();
            needsIngest = true; // ✅ 标记需要 ingest
        }
        return inMemoryEmbeddingStore;
    }

    /**
     * 注册内容检索器 Bean
     * - 根据 needsIngest 标志位决定是否执行文档向量化
     * - 避免在 contentRetriever 中重复判断文件是否存在
     */
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore) {
        if (needsIngest) {
            ingestDocuments(embeddingStore);
        } else {
            log.info("[RAG] 向量库已从文件加载，跳过 ingest");
        }

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.75)
                .build();
    }

    /**
     * 执行文档加载、切割、向量化并写入向量库
     */
    private void ingestDocuments(EmbeddingStore<TextSegment> embeddingStore) {
        log.info("[RAG] 开始加载并向量化文档，目录: {}", docsDir);

        List<Document> documents = FileSystemDocumentLoader.loadDocuments(docsDir);
        if (documents.isEmpty()) {
            log.warn("[RAG] 未在 {} 目录下找到任何文档，跳过 ingest", docsDir);
            return;
        }
        log.info("[RAG] 共加载文档 {} 篇", documents.size());

        // ✅ 按字符数切割，严格控制每段不超过接口字符数限制
        DocumentByCharacterSplitter splitter = new DocumentByCharacterSplitter(maxSegmentSize, maxOverlapSize);

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                // 在每段前拼接文件名，提升检索相关性
                .textSegmentTransformer(textSegment -> TextSegment.from(
                        textSegment.metadata().getString("file_name") + "\n" + textSegment.text(),
                        textSegment.metadata()
                ))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(documents);
        log.info("[RAG] 文档向量化完成，共处理 {} 篇文档", documents.size());
    }

    /**
     * 应用关闭时将内存向量库持久化到文件
     * 下次启动直接加载，无需重新 ingest
     */
    @PreDestroy
    public void saveStore() {
        if (inMemoryEmbeddingStore != null) {
            inMemoryEmbeddingStore.serializeToFile(storeFile);
            log.info("[RAG] 向量库已持久化到文件: {}", Path.of(storeFile).toAbsolutePath());
        }
    }
}