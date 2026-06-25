package com.yeschillaicode.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yeschillaicode.model.dto.ChatHistory.ChatHistoryQueryRequest;
import com.yeschillaicode.model.entity.ChatHistory;
import com.yeschillaicode.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 */
public interface ChatHistoryService extends IService<ChatHistory> {
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    boolean addChatMessage(Long appid, String message, String messageType, Long userId);

    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);



    boolean  deleteByappid(Long appid);

    /**
     * 查询指定应用的聊天历史记录数
     */
    long countByAppId(Long appId);

    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}
