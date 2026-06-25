package com.yeschillaicode.model.dto.app;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 生成模式：normal-快速模式（默认），workflow-工作流模式
     */
    private String genMode;

    private static final long serialVersionUID = 1L;
}
