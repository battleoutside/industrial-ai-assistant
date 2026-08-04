package com.cetus.industrialai.ai;

import com.cetus.industrialai.ai.rag.KnowledgeBaseIngestor;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * description: RAG 本地持久化存储测试
 **/
@SpringBootTest
public class KnowledgeBaseIngestorTest {

    private static final Path EMBEDDING_STORE_PATH = Path.of("data", "embedding-store.json");

    @Resource
    private KnowledgeBaseIngestor ragIngestor;

    @Test
    void buildEmbeddingStore() {

        // 防止误操作导致重复向量化、重复消耗 Token
        assertFalse(
                Files.exists(EMBEDDING_STORE_PATH),
                "向量库文件已存在。为避免重复扣费，请确认是否真的需要重建。"
        );

        ragIngestor.ingest();

        // 验证本地向量库是否生成成功
        assertTrue(
                Files.exists(EMBEDDING_STORE_PATH),
                "向量库文件生成失败"
        );
    }
}
