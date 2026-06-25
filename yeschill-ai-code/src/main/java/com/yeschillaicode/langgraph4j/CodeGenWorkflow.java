package com.yeschillaicode.langgraph4j;

import cn.hutool.json.JSONUtil;
import com.yeschillaicode.exception.BusinessException;
import com.yeschillaicode.exception.ErrorCode;
import com.yeschillaicode.langgraph4j.model.QualityResult;
import com.yeschillaicode.langgraph4j.node.*;
import com.yeschillaicode.langgraph4j.state.WorkflowContext;
import com.yeschillaicode.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * 代码生成工作流
 */
@Slf4j
@Component
public class CodeGenWorkflow {

    /**
     * 创建全流程工作流（首次生成）
     * START → image_collector → prompt_enhancer → router → code_generator → code_quality_check → project_builder/END
     */
    public CompiledGraph<MessagesState<String>> createFullWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("image_collector", ImageCollectorNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("router", RouterNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("code_quality_check", CodeQualityCheckNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())
                    .addEdge(START, "image_collector")
                    .addEdge("image_collector", "prompt_enhancer")
                    .addEdge("prompt_enhancer", "router")
                    .addEdge("router", "code_generator")
                    .addEdge("code_generator", "code_quality_check")
                    .addConditionalEdges("code_quality_check",
                            edge_async(this::routeAfterQualityCheck),
                            Map.of(
                                    "build", "project_builder",
                                    "skip_build", END,
                                    "fail", "code_generator"
                            ))
                    .addEdge("project_builder", END)
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "全流程工作流创建失败");
        }
    }

    /**
     * 创建简化工作流（后续修改）
     * START → code_generator → code_quality_check → project_builder/END
     */
    public CompiledGraph<MessagesState<String>> createSimplifiedWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("code_quality_check", CodeQualityCheckNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())
                    .addEdge(START, "code_generator")
                    .addEdge("code_generator", "code_quality_check")
                    .addConditionalEdges("code_quality_check",
                            edge_async(this::routeAfterQualityCheck),
                            Map.of(
                                    "build", "project_builder",
                                    "skip_build", END,
                                    "fail", "code_generator"
                            ))
                    .addEdge("project_builder", END)
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "简化工作流创建失败");
        }
    }

    /**
     * 根据是否首次生成选择工作流图
     */
    private CompiledGraph<MessagesState<String>> selectWorkflow(boolean isFirstGeneration) {
        if (isFirstGeneration) {
            return createFullWorkflow();
        }
        return createSimplifiedWorkflow();
    }

    /**
     * 执行工作流
     */
    public WorkflowContext executeWorkflow(Long appId, String originalPrompt, CodeGenTypeEnum codeGenType, boolean isFirstGeneration) {
        CompiledGraph<MessagesState<String>> workflow = selectWorkflow(isFirstGeneration);

        WorkflowContext initialContext = WorkflowContext.builder()
                .appId(appId)
                .originalPrompt(originalPrompt)
                .generationType(codeGenType)
                .currentStep("初始化")
                .build();

        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("工作流图:\n{}", graph.content());
        log.info("开始执行代码生成工作流，模式: {}", isFirstGeneration ? "全流程" : "简化流程");

        WorkflowContext finalContext = null;
        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
            log.info("--- 第 {} 步完成 ---", stepCounter);
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                finalContext = currentContext;
                log.info("当前步骤上下文: {}", currentContext);
            }
            stepCounter++;
        }
        log.info("代码生成工作流执行完成！");
        return finalContext;
    }

    /**
     * 执行工作流（Flux 流式输出版本）
     */
    public Flux<String> executeWorkflowWithFlux(Long appId, String originalPrompt, CodeGenTypeEnum codeGenType, boolean isFirstGeneration) {
        return Flux.create(sink -> {
            Thread.startVirtualThread(() -> {
                try {
                    CompiledGraph<MessagesState<String>> workflow = selectWorkflow(isFirstGeneration);
                    WorkflowContext initialContext = WorkflowContext.builder()
                            .appId(appId)
                            .originalPrompt(originalPrompt)
                            .generationType(codeGenType)
                            .currentStep("初始化")
                            .build();
                    sink.next(formatSseEvent("workflow_start", Map.of(
                            "message", "开始执行代码生成工作流",
                            "mode", isFirstGeneration ? "全流程" : "简化流程",
                            "originalPrompt", originalPrompt
                    )));
                    GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                    log.info("工作流图:\n{}", graph.content());

                    int stepCounter = 1;
                    for (NodeOutput<MessagesState<String>> step : workflow.stream(
                            Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                        log.info("--- 第 {} 步完成 ---", stepCounter);
                        WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                        if (currentContext != null) {
                            sink.next(formatSseEvent("step_completed", Map.of(
                                    "stepNumber", stepCounter,
                                    "currentStep", currentContext.getCurrentStep()
                            )));
                            log.info("当前步骤上下文: {}", currentContext);
                        }
                        stepCounter++;
                    }
                    sink.next(formatSseEvent("workflow_completed", Map.of(
                            "message", "代码生成工作流执行完成！"
                    )));
                    log.info("代码生成工作流执行完成！");
                    sink.complete();
                } catch (Exception e) {
                    log.error("工作流执行失败: {}", e.getMessage(), e);
                    sink.next(formatSseEvent("workflow_error", Map.of(
                            "error", e.getMessage(),
                            "message", "工作流执行失败"
                    )));
                    sink.error(e);
                }
            });
        });
    }

    private String formatSseEvent(String eventType, Object data) {
        try {
            String jsonData = JSONUtil.toJsonStr(data);
            return "event: " + eventType + "\ndata: " + jsonData + "\n\n";
        } catch (Exception e) {
            log.error("格式化 SSE 事件失败: {}", e.getMessage(), e);
            return "event: error\ndata: {\"error\":\"格式化失败\"}\n\n";
        }
    }

    /**
     * 执行工作流（SSE 流式输出版本）
     */
    public SseEmitter executeWorkflowWithSse(Long appId, String originalPrompt, CodeGenTypeEnum codeGenType, boolean isFirstGeneration, Runnable onComplete) {
        // 全流程：图片收集 → 提示词增强 → 智能路由 → 代码生成 → 代码质量检查 → 项目构建
        // 简化流程：代码生成 → 代码质量检查 → 项目构建
        List<String> stepNames = isFirstGeneration
                ? List.of("图片收集", "提示词增强", "智能路由", "代码生成", "代码质量检查", "项目构建")
                : List.of("代码生成", "代码质量检查", "项目构建");

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        Thread.startVirtualThread(() -> {
            try {
                CompiledGraph<MessagesState<String>> workflow = selectWorkflow(isFirstGeneration);
                WorkflowContext initialContext = WorkflowContext.builder()
                        .appId(appId)
                        .originalPrompt(originalPrompt)
                        .generationType(codeGenType)
                        .currentStep("初始化")
                        .build();
                sendSseEvent(emitter, "workflow_start", Map.of(
                        "message", "开始执行代码生成工作流",
                        "mode", isFirstGeneration ? "全流程" : "简化流程",
                        "totalSteps", stepNames.size(),
                        "steps", stepNames
                ));
                GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                log.info("工作流图:\n{}", graph.content());

                // 推送第一步开始事件
                sendSseEvent(emitter, "step_start", Map.of(
                        "stepNumber", 1,
                        "currentStep", stepNames.get(0)
                ));

                int stepCounter = 1;
                WorkflowContext finalCtx = null;
                for (NodeOutput<MessagesState<String>> ignored : workflow.stream(
                        Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                    log.info("--- 第 {} 步完成 ---", stepCounter);
                    finalCtx = WorkflowContext.getContext(ignored.state());
                    sendSseEvent(emitter, "step_completed", Map.of(
                            "stepNumber", stepCounter,
                            "currentStep", stepNames.get(stepCounter - 1)
                    ));

                    // 推送下一步开始事件
                    if (stepCounter < stepNames.size()) {
                        sendSseEvent(emitter, "step_start", Map.of(
                                "stepNumber", stepCounter + 1,
                                "currentStep", stepNames.get(stepCounter)
                        ));
                    }
                    stepCounter++;
                }
                // 工作流成功后回调（保存聊天历史等）
                if (onComplete != null) {
                    try {
                        onComplete.run();
                    } catch (Exception e) {
                        log.error("onComplete 回调执行失败: {}", e.getMessage(), e);
                    }
                }
                // 回放代码生成阶段收集的流式片段，格式与快速模式一致
                List<String> chunks = finalCtx != null ? finalCtx.getCodeGenChunks() : null;
                if (chunks != null && !chunks.isEmpty()) {
                    log.info("开始回放 {} 个代码片段", chunks.size());
                    for (int i = 0; i < chunks.size(); i++) {
                        // 格式与快速模式一致: {"d":"chunk"}
                        String chunk = chunks.get(i);
                        String json = "{\"d\":" + JSONUtil.toJsonStr(chunk) + "}";
                        emitter.send(SseEmitter.event().data(json));
                        // 5ms 间隔平滑回放
                        if (i < chunks.size() - 1) {
                            try {
                                Thread.sleep(5);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                    log.info("代码片段回放完成");
                }
                sendSseEvent(emitter, "codegen_done", Map.of(
                        "message", "代码生成完成"
                ));
                sendSseEvent(emitter, "workflow_completed", Map.of(
                        "message", "代码生成工作流执行完成！"
                ));
                log.info("代码生成工作流执行完成！");
                emitter.complete();
            } catch (Exception e) {
                log.error("工作流执行失败: {}", e.getMessage(), e);
                sendSseEvent(emitter, "workflow_error", Map.of(
                        "error", e.getMessage(),
                        "message", "工作流执行失败"
                ));
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    private void sendSseEvent(SseEmitter emitter, String eventType, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(data));
        } catch (IOException e) {
            log.error("发送 SSE 事件失败: {}", e.getMessage(), e);
        }
    }

    private String routeAfterQualityCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        QualityResult qualityResult = context.getQualityResult();
        if (qualityResult == null || !qualityResult.getIsValid()) {
            log.error("代码质检失败，需要重新生成代码");
            return "fail";
        }
        log.info("代码质检通过，继续后续流程");
        return routeBuildOrSkip(state);
    }

    private String routeBuildOrSkip(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        CodeGenTypeEnum generationType = context.getGenerationType();
        if (generationType == CodeGenTypeEnum.HTML || generationType == CodeGenTypeEnum.MULTI_FILE) {
            return "skip_build";
        }
        return "build";
    }
}
