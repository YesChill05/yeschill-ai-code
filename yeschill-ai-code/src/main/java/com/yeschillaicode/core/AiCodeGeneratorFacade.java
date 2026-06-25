package com.yeschillaicode.core;

import cn.hutool.json.JSONUtil;
import com.yeschillaicode.ai.AiCodeGeneratorService;
import com.yeschillaicode.ai.AiCodeGeneratorServiceFactory;
import com.yeschillaicode.ai.model.HtmlCodeResult;
import com.yeschillaicode.ai.model.MultiFileCodeResult;
import com.yeschillaicode.ai.model.message.AiResponseMessage;
import com.yeschillaicode.ai.model.message.ToolExecutedMessage;
import com.yeschillaicode.ai.model.message.ToolRequestMessage;
import com.yeschillaicode.constant.AppConstant;
import com.yeschillaicode.core.bulider.VueProjectBuilder;
import com.yeschillaicode.core.parser.CodeParserExecutor;
import com.yeschillaicode.core.saver.CodeFileSaverExecutor;
import com.yeschillaicode.exception.BusinessException;
import com.yeschillaicode.exception.ErrorCode;
import com.yeschillaicode.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI代码生成门面类，组合代码生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {
  @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;
  @Resource
    VueProjectBuilder vueProjectBuilder;
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenTypeEnum,Long appid) {
        //字符串拼接器，用于当流式返回所有的代码后
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(chunk-> {
            //实时收集代码片段
            codeBuilder.append(chunk);
        }).doOnComplete(() -> {
            try{
                //当流式返回所有的代码后，保存代码
                String completeCode = codeBuilder.toString();
            //使用解析器解析返回的代码
                Object parserResult = CodeParserExecutor.executeParser(completeCode, codeGenTypeEnum);
                //保存代码
                File saveDir = CodeFileSaverExecutor.executeSaver(parserResult, codeGenTypeEnum,appid);
                log.info("保存代码成功:{}",saveDir.getAbsolutePath());
            }catch (Exception e){
                log.error("保存代码失败:{}",e.getMessage());
            }
        });
    }

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum,Long appid) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getaiCodeGeneratorService(appid, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML,appid);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE,appid);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum,Long appid) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getaiCodeGeneratorService(appid, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML,appid);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE,appid);
            }
            case VUE_PROJECT -> {
               TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appid, userMessage);
                yield  processTokenStream(tokenStream,appid);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream,Long appId) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        // 执行 Vue 项目构建（同步执行，确保预览时项目已就绪）
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId;
                        vueProjectBuilder.buildProject(projectPath);
                        sink.complete();
                    })

                    .onError((Throwable error) -> {
                        log.error("TokenStream 错误", error);
                        sink.error(error);
                    })
                    .start();
        });
    }



}
