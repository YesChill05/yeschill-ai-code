package com.yeschillaicode.ai.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import lombok.Data;

@Data
public class RetryOutputGuardrail implements OutputGuardrail {

    @Override
    public OutputGuardrailResult validate(AiMessage aiMessage) {
        // 简单的输出验证逻辑
        String output = aiMessage.text();
        
        // 检查输出是否为空
        if (output == null || output.trim().isEmpty()) {
            return retry("输出内容为空，请重新生成");
        }
        
        // 检查输出长度
        if (output.length() < 10) {
            return retry("输出内容过短，请提供更详细的信息");
        }
        
        return success();
    }
}
