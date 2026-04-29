package com.sparta.delivery.ai.infrastructure.external.llm;

import com.sparta.delivery.ai.domain.entity.LlmProvider;
import com.sparta.delivery.ai.domain.vo.ActiveLlmInfo;

public interface LlmClient {
    boolean supports(LlmProvider provider);

    LlmGenerateResponse generate(ActiveLlmInfo llm, LlmGenerateRequest request);
}
