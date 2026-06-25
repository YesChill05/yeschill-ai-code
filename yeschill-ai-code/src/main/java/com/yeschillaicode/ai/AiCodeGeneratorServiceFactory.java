package com.yeschillaicode.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yeschillaicode.ai.guardrail.PromptSafetyInputGuardrail;
import com.yeschillaicode.ai.guardrail.RetryOutputGuardrail;
import com.yeschillaicode.ai.tool.*;
import com.yeschillaicode.exception.BusinessException;
import com.yeschillaicode.exception.ErrorCode;
import com.yeschillaicode.model.enums.CodeGenTypeEnum;
import com.yeschillaicode.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.yeschillaicode.utils.SpringContextUtil;

import java.time.Duration;

/**
 * AI代码生成器服务工厂类
 * 用于创建和管理AI代码生成相关的服务实例
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private ToolManager toolManager;
    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return  getaiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }
    /**
     * 根据 appId 获取服务（带缓存）
     */
    public AiCodeGeneratorService getaiCodeGeneratorService(long appId,CodeGenTypeEnum codeGenType) {
        String cacheKey = bulidCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key-> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 创建新的 AI 服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        // 根据代码生成类型选择不同的模型配置
        return switch (codeGenType) {
            case VUE_PROJECT -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModel", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools(toolManager.getAllTools())
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .outputGuardrails(new RetryOutputGuardrail())
                        .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                                toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                        ))
                        .build();
            }
            case HTML, MULTI_FILE -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory)
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .outputGuardrails(new RetryOutputGuardrail())
                        .build();
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }


    /**
 * 创建并配置一个AI代码生成器服务实例
 * 该方法会根据传入的appId构建独立的对话记忆，并返回一个配置好的AiCodeGeneratorService实例
 *
 * @param appId 应用ID，用于构建独立的对话记忆标识
 * @return 配置好的AiCodeGeneratorService实例，包含聊天模型、流式聊天模型和对话记忆
 */
    public AiCodeGeneratorService getaiCodeGeneratorService(long appId) {
        //根据appId构建独立的对话记忆
    // 使用MessageWindowChatMemory来管理对话历史，限制最多保存20条消息
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)            // 设置记忆ID，使用appId确保每个应用有独立的对话记忆
                .chatMemoryStore(redisChatMemoryStore)  // 使用Redis作为聊天记忆的存储后端
                .maxMessages(20)     // 设置最大保存的消息数量为20条
                .build();
    // 使用多例模式的 StreamingChatModel 解决并发问题
    StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
    // 使用AiServices构建器创建AiCodeGeneratorService实例
    // 配置聊天模型、流式聊天模型和对话记忆
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)            // 设置普通聊天模型
                .streamingChatModel(openAiStreamingChatModel)  // 设置流式聊天模型
                .chatMemory(chatMemory)          // 设置对话记忆
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .outputGuardrails(new RetryOutputGuardrail())
                .build();
    }

/**
   创建ai代码生成器服务
 */
  @Bean
public AiCodeGeneratorService aiCodeGeneratorService() {
      return getaiCodeGeneratorService(0);
  }


  private  String  bulidCacheKey(long appId,CodeGenTypeEnum codeGenType) {
      return appId + "_" + codeGenType.getValue();
  }
   }




