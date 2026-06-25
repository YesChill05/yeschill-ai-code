package com.yeschillaicode.langgraph4j;

import com.yeschillaicode.langgraph4j.model.ImageCollectionPlan;
import com.yeschillaicode.langgraph4j.model.ImageResource;
import com.yeschillaicode.langgraph4j.model.QualityResult;
import com.yeschillaicode.langgraph4j.model.enums.ImageCategoryEnum;
import com.yeschillaicode.langgraph4j.state.WorkflowContext;
import com.yeschillaicode.model.enums.CodeGenTypeEnum;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CodeGenWorkflowTest {

    @Test
    @DisplayName("工作流图创建成功")
    void createWorkflow_shouldReturnCompiledGraph() {
        CodeGenWorkflow workflow = new CodeGenWorkflow();
        CompiledGraph<MessagesState<String>> graph = workflow.createFullWorkflow();
        assertNotNull(graph);
    }

    @Test
    @DisplayName("简化工作流图创建成功")
    void createSimplifiedWorkflow_shouldReturnCompiledGraph() {
        CodeGenWorkflow workflow = new CodeGenWorkflow();
        CompiledGraph<MessagesState<String>> graph = workflow.createSimplifiedWorkflow();
        assertNotNull(graph);
    }

    @Test
    @DisplayName("工作流图 Mermaid 表示包含所有节点")
    void workflowGraph_shouldContainAllNodes() {
        CodeGenWorkflow workflow = new CodeGenWorkflow();
        CompiledGraph<MessagesState<String>> graph = workflow.createFullWorkflow();
        GraphRepresentation mermaid = graph.getGraph(GraphRepresentation.Type.MERMAID);
        String content = mermaid.content();
        assertTrue(content.contains("image_collector"));
        assertTrue(content.contains("prompt_enhancer"));
        assertTrue(content.contains("router"));
        assertTrue(content.contains("code_generator"));
        assertTrue(content.contains("code_quality_check"));
        assertTrue(content.contains("project_builder"));
    }

    @Test
    @DisplayName("WorkflowContext 创建和获取")
    void workflowContext_shouldCreateAndRetrieve() {
        WorkflowContext context = WorkflowContext.builder()
                .appId(123L)
                .originalPrompt("test prompt")
                .currentStep("初始化")
                .generationType(CodeGenTypeEnum.HTML)
                .build();
        assertEquals(123L, context.getAppId());
        assertEquals("test prompt", context.getOriginalPrompt());
        assertEquals("初始化", context.getCurrentStep());
        assertEquals(CodeGenTypeEnum.HTML, context.getGenerationType());
    }

    @Test
    @DisplayName("WorkflowContext 保存和恢复到 MessagesState")
    void workflowContext_shouldSaveAndRestore() {
        WorkflowContext context = WorkflowContext.builder()
                .originalPrompt("test")
                .currentStep("step1")
                .build();
        Map<String, Object> saved = WorkflowContext.saveContext(context);
        assertNotNull(saved);
        assertEquals(context, saved.get(WorkflowContext.WORKFLOW_CONTEXT_KEY));
    }

    @Test
    @DisplayName("ImageResource 构建")
    void imageResource_shouldBuildCorrectly() {
        ImageResource resource = ImageResource.builder()
                .category(ImageCategoryEnum.CONTENT)
                .description("test image")
                .url("https://example.com/img.jpg")
                .build();
        assertEquals(ImageCategoryEnum.CONTENT, resource.getCategory());
        assertEquals("test image", resource.getDescription());
        assertEquals("https://example.com/img.jpg", resource.getUrl());
    }

    @Test
    @DisplayName("QualityResult 构建")
    void qualityResult_shouldBuildCorrectly() {
        QualityResult result = QualityResult.builder()
                .isValid(true)
                .errors(List.of())
                .suggestions(List.of("improve naming"))
                .build();
        assertTrue(result.getIsValid());
        assertTrue(result.getErrors().isEmpty());
        assertEquals(1, result.getSuggestions().size());
    }

    @Test
    @DisplayName("ImageCategoryEnum 通过 value 查找")
    void imageCategoryEnum_shouldFindByValue() {
        assertEquals(ImageCategoryEnum.CONTENT, ImageCategoryEnum.getEnumByValue("CONTENT"));
        assertEquals(ImageCategoryEnum.LOGO, ImageCategoryEnum.getEnumByValue("LOGO"));
        assertEquals(ImageCategoryEnum.ILLUSTRATION, ImageCategoryEnum.getEnumByValue("ILLUSTRATION"));
        assertEquals(ImageCategoryEnum.ARCHITECTURE, ImageCategoryEnum.getEnumByValue("ARCHITECTURE"));
        assertNull(ImageCategoryEnum.getEnumByValue("UNKNOWN"));
        assertNull(ImageCategoryEnum.getEnumByValue(null));
    }

    @Test
    @DisplayName("ImageCollectionPlan 内部 record 创建")
    void imageCollectionPlan_recordsShouldWork() {
        ImageCollectionPlan plan = new ImageCollectionPlan();
        plan.setContentImageTasks(List.of(new ImageCollectionPlan.ImageSearchTask("nature")));
        plan.setIllustrationTasks(List.of(new ImageCollectionPlan.IllustrationTask("tech")));
        plan.setDiagramTasks(List.of(new ImageCollectionPlan.DiagramTask("graph TD; A-->B", "架构图")));
        plan.setLogoTasks(List.of(new ImageCollectionPlan.LogoTask("科技公司Logo")));

        assertEquals(1, plan.getContentImageTasks().size());
        assertEquals("nature", plan.getContentImageTasks().get(0).query());
        assertEquals(1, plan.getIllustrationTasks().size());
        assertEquals("tech", plan.getIllustrationTasks().get(0).query());
        assertEquals(1, plan.getDiagramTasks().size());
        assertEquals("graph TD; A-->B", plan.getDiagramTasks().get(0).mermaidCode());
        assertEquals(1, plan.getLogoTasks().size());
        assertEquals("科技公司Logo", plan.getLogoTasks().get(0).description());
    }
}
