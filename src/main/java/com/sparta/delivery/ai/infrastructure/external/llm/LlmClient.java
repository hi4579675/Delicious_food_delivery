package com.sparta.delivery.ai.infrastructure.external.llm;

import com.sparta.delivery.ai.domain.entity.Llm;
import com.sparta.delivery.ai.domain.entity.LlmProvider;

public interface LlmClient {
    boolean supports(LlmProvider provider);

    LlmGenerateResponse generate(Llm llm, LlmGenerateRequest request);
}
