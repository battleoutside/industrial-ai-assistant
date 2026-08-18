package com.cetus.industrialai.designguidance;

import com.cetus.industrialai.designguidance.knowledge.GuidanceKnowledgeIngestor;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工程指导知识库的一次性构建测试。
 *
 * <p>测试会调用千问Embedding并生成独立向量文件，
 * 因此通过integration标签与默认单元测试隔离。</p>
 */
@Tag("integration")
@SpringBootTest
class DesignGuidanceKnowledgeBaseIngestorTest {

    private static final Path EMBEDDING_STORE_PATH =
            Path.of("data", "design-guidance-embedding-store.json");

    @Resource
    private GuidanceKnowledgeIngestor guidanceKnowledgeIngestor;

    @Test
    void shouldBuildDesignGuidanceEmbeddingStoreOnce() {
        assertFalse(
                Files.exists(EMBEDDING_STORE_PATH),
                "工程指导向量库已存在。为避免重复调用Embedding，请确认后再重建。"
        );

        guidanceKnowledgeIngestor.ingest();

        assertTrue(
                Files.exists(EMBEDDING_STORE_PATH),
                "工程指导向量库生成失败"
        );
    }
}
