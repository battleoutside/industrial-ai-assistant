package com.cetus.industrialai.industryintelligence;

import com.cetus.industrialai.industryintelligence.model.IndustryIntelligenceRequest;
import com.cetus.industrialai.industryintelligence.model.IndustryIntelligenceResponse;
import com.cetus.industrialai.industryintelligence.service.IndustryIntelligenceService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 行业信息咨询真实 Agent + MCP 集成测试。
 *
 * <p>该测试会调用千问 ChatModel 和外部 MCP Web Search，
 * 因此通过 integration 标签与默认单元测试隔离，
 * 只在显式验证第三模块真实工具调用链路时运行。</p>
 */
@Tag("integration")
@SpringBootTest
class IndustryIntelligenceIntegrationTest {

    @Resource
    private IndustryIntelligenceService industryIntelligenceService;

    @Test
    void shouldResearchCurrentIndustryInformationWithMcpWebSearch() {
        IndustryIntelligenceRequest request = new IndustryIntelligenceRequest(
                "最近高速连接器行业有哪些值得关注的公开产品或技术动态？"
        );

        IndustryIntelligenceResponse response = industryIntelligenceService.analyze(request);

        assertNotNull(response);
        assertNotNull(response.status());
        assertNotEquals(
                IndustryIntelligenceResponse.Status.SEARCH_FAILED,
                response.status()
        );

        if (response.status() == IndustryIntelligenceResponse.Status.SUCCESS) {
            assertFalse(response.findings().isEmpty());
            assertFalse(response.sources().isEmpty());
            assertTrue(response.sourceCount() > 0);
        }
    }
}
