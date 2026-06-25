package com.yeschillaicode.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * 生成 HTML 代码文件的结果类
 * 使用@Data注解自动生成getter、setter等方法
 */
@Description("生成 HTML 代码文件的结果")
@Data
public class HtmlCodeResult {

    @Description("HTML代码")
    private String htmlCode;  // 存储生成的HTML代码内容

    @Description("生成代码的描述")
    private String description;  // 存储对生成代码的描述信息
}

