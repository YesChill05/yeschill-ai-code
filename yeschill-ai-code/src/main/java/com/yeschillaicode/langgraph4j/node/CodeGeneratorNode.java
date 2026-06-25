package com.yeschillaicode.langgraph4j.node;

import com.yeschillaicode.constant.AppConstant;
import com.yeschillaicode.core.AiCodeGeneratorFacade;
import com.yeschillaicode.langgraph4j.model.QualityResult;
import com.yeschillaicode.langgraph4j.state.WorkflowContext;
import com.yeschillaicode.model.enums.CodeGenTypeEnum;
import com.yeschillaicode.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 网站代码生成节点
 */
@Slf4j
public class CodeGeneratorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 代码生成");
            String userMessage = buildUserMessage(context);
            CodeGenTypeEnum generationType = context.getGenerationType();

            AiCodeGeneratorFacade codeGeneratorFacade = SpringContextUtil.getBean(AiCodeGeneratorFacade.class);
            log.info("开始生成代码，类型: {} ({})", generationType.getValue(), generationType.getText());
            Long appId = context.getAppId() != null ? context.getAppId() : 0L;

            // 收集流式片段，后续在工作流完成后回放给前端
            List<String> chunks = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
            Flux<String> codeStream = codeGeneratorFacade.generateAndSaveCodeStream(userMessage, generationType, appId)
                    .doOnNext(chunk -> {
                        if (chunk != null && !chunk.isEmpty()) {
                            chunks.add(chunk);
                        }
                    });
            codeStream.blockLast(Duration.ofMinutes(10));

            String generatedCodeDir = String.format("%s/%s_%s", AppConstant.CODE_OUTPUT_ROOT_DIR, generationType.getValue(), appId);
            log.info("AI 代码生成完成，生成目录: {}, 收集到 {} 个片段", generatedCodeDir, chunks.size());

            context.setCurrentStep("代码生成");
            context.setGeneratedCodeDir(generatedCodeDir);
            context.setCodeGenChunks(new java.util.ArrayList<>(chunks));
            return WorkflowContext.saveContext(context);
        });
    }

    private static String buildUserMessage(WorkflowContext context) {
        String userMessage = context.getEnhancedPrompt();
        QualityResult qualityResult = context.getQualityResult();
        if (isQualityCheckFailed(qualityResult)) {
            userMessage = buildErrorFixPrompt(qualityResult);
        }
        return userMessage;
    }

    private static boolean isQualityCheckFailed(QualityResult qualityResult) {
        return qualityResult != null &&
                !qualityResult.getIsValid() &&
                qualityResult.getErrors() != null &&
                !qualityResult.getErrors().isEmpty();
    }

    private static String buildErrorFixPrompt(QualityResult qualityResult) {
        StringBuilder errorInfo = new StringBuilder();
        errorInfo.append("\n\n## 上次生成的代码存在以下问题，请修复：\n");
        qualityResult.getErrors().forEach(error ->
                errorInfo.append("- ").append(error).append("\n"));
        if (qualityResult.getSuggestions() != null && !qualityResult.getSuggestions().isEmpty()) {
            errorInfo.append("\n## 修复建议：\n");
            qualityResult.getSuggestions().forEach(suggestion ->
                    errorInfo.append("- ").append(suggestion).append("\n"));
        }
        errorInfo.append("\n请根据上述问题和建议重新生成代码，确保修复所有提到的问题。");
        return errorInfo.toString();
    }
}
