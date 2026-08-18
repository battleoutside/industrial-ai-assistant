package com.cetus.industrialai.designguidance.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 工程设计指导模块的请求。
 *
 * <p>保留产品描述、工程阶段、问题和已知条件四项核心信息，
 * 既能帮助RAG形成有效检索词，也避免MVP阶段枚举过多专业参数。</p>
 *
 * @param productDescription 产品类型、结构、型号或关键规格
 * @param engineeringStage   当前工程阶段
 * @param question           需要解决的工程问题
 * @param knownConditions    已知材料、结构、测试条件或异常现象，可为空
 */
public record DesignGuidanceRequest(
        @NotBlank(message = "产品描述不能为空")
        @Size(max = 500, message = "产品描述不能超过500个字符")
        String productDescription,

        @NotNull(message = "工程阶段不能为空")
        EngineeringStage engineeringStage,

        @NotBlank(message = "工程问题不能为空")
        @Size(max = 1000, message = "工程问题不能超过1000个字符")
        String question,

        @Size(max = 1000, message = "已知条件不能超过1000个字符")
        String knownConditions
) {

    /**
     * 工程阶段枚举，用于为检索词补充上下文环境。
     */
    public enum EngineeringStage {
        /**
         * 产品概念设计或架构设计阶段，关注可行性、布局与初期选型
         */
        DESIGN,
        /**
         * 样机或原型验证阶段，关注功能实现、首版测试与设计缺陷排查
         */
        PROTOTYPE,
        /**
         * 系统性测试或可靠性验证阶段，关注性能指标、耐久性与极限工况
         */
        TEST,
        /**
         * 量产导入与大批量制造阶段，关注工艺稳定性、良率及成本优化
         */
        MASS_PRODUCTION
    }
}
