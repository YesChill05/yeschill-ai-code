package com.yeschillaicode.core;

import com.yeschillaicode.ai.AiCodeGeneratorService;
import com.yeschillaicode.ai.model.HtmlCodeResult;
import com.yeschillaicode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class AiCodeGeneratorFacadeTest {
  @Resource
  private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateAndSaveCode() {
        File file = aiCodeGeneratorFacade.generateAndSaveCode("生成一个登录页面,有一个标题为臭jk与困困鱼,不超过20行代码", CodeGenTypeEnum.MULTI_FILE,1L);
        Assertions.assertNotNull(file);
    }

    @Test
    void generateAndSaveCodeStream() {
       Flux<String> codeStream= aiCodeGeneratorFacade.generateAndSaveCodeStream("生成一个登录页面,有一个标题为臭jk与困困鱼,不超过20行代码", CodeGenTypeEnum.MULTI_FILE,1L);
        List<String> result = codeStream.collectList().block();
       Assertions.assertNotNull(result);
        String completecontent = String.join("", result);
        Assertions.assertNotNull(completecontent);
    }
    @Test
    void generateVueProjectCodeStream() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(
                "简单的任务记录网站，总代码量不超过 200 行",
                CodeGenTypeEnum.VUE_PROJECT, 1L);
        // 阻塞等待所有数据收集完成，设置5分钟超时
        List<String> result = codeStream.collectList().block(Duration.ofMinutes(5));
        // 验证结果
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }


}