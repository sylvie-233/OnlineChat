package com.sylvie233.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sylvie233.repository.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 查询会话最新消息（私聊：按用户对双查；群聊：按 conversation_id）
     */
    List<Message> selectLatestByConversation(@Param("conversationId") Long conversationId,
                                              @Param("type") Integer type,
                                              @Param("userId") Long userId,
                                              @Param("targetId") Long targetId,
                                              @Param("limit") int limit);

    /**
     * 历史翻页（私聊：按用户对双查；群聊：按 conversation_id）
     */
    List<Message> selectHistoryByConversation(@Param("conversationId") Long conversationId,
                                               @Param("type") Integer type,
                                               @Param("userId") Long userId,
                                               @Param("targetId") Long targetId,
                                               @Param("cursorTime") String cursorTime,
                                               @Param("limit") int limit);

    /**
     * 增量同步
     */
    List<Message> selectSyncByConversation(@Param("conversationId") Long conversationId,
                                            @Param("type") Integer type,
                                            @Param("userId") Long userId,
                                            @Param("targetId") Long targetId,
                                            @Param("cursorTime") String cursorTime,
                                            @Param("limit") int limit);
}
