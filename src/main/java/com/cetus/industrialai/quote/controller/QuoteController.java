package com.cetus.industrialai.quote.controller;

import com.cetus.industrialai.quote.model.QuoteRequest;
import com.cetus.industrialai.quote.model.QuoteResult;
import com.cetus.industrialai.quote.service.QuoteAgentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能报价HTTP接口。
 *
 * <p>负责接收前端提交的报价请求，执行基础参数校验，
 * 再将请求交给QuoteAgentService完成历史案例检索、
 * 千问需求分析和Java确定性报价计算。</p>
 *
 * <p>Controller不理解报价需求，也不计算任何金额，
 * 只作为前端与报价业务编排服务之间的HTTP入口。</p>
 */
@RestController
@RequestMapping("/quote")
public class QuoteController {

    private final QuoteAgentService quoteAgentService;

    public QuoteController(QuoteAgentService quoteAgentService) {
        this.quoteAgentService = quoteAgentService;
    }

    /**
     * 生成10PCS、1K和5K三阶梯报价。
     *
     * <p>前端通过POST方式提交JSON请求。
     * 系统会在调用云端千问模型之前完成参数校验，
     * 对空字段、非法枚举值、非法长度以及错误的百分率格式进行拦截，
     * 并返回HTTP 400。
     *
     * <p>百分率统一使用小数，例如5%传入0.05；
     * 用户无需也不能通过该接口传入利润率。</p>
     *
     * @param request 前端提交的报价请求
     * @return 智能报价Agent生成的完整报价结果
     */
    @PostMapping("/generate")
    public QuoteResult generateQuote(
            @Valid @RequestBody QuoteRequest request
    ) {
        return quoteAgentService.generateQuote(request);
    }
}