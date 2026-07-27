package com.sylvie233.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sylvie233.repository.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 查询会话最新消息（私聊：按 from_user_id/to_id 双查；群聊：按 conversation_id）
     */
    @Select("SELECT * FROM message WHERE conversation_type = #{type} AND is_deleted = 0 "
            + "AND (conversation_id = #{conversationId} "
            + "  OR (#{type} = 0 AND (from_user_id = #{userId} OR to_id = #{userId}))) "
            + "ORDER BY seq DESC LIMIT #{limit}")
    List<Message> selectLatestByConversation(@Param("conversationId") Long conversationId,
                                              @Param("type") Integer type,
                                              @Param("userId") Long userId,
                                              @Param("limit") int limit);

    /**
     * 历史翻页（私聊：按 from_user_id/to_id 双查；群聊：按 conversation_id）
     */
    @Select("SELECT * FROM message WHERE conversation_type = #{type} "
            + "AND seq < #{cursorSeq} AND is_deleted = 0 "
            + "AND (conversation_id = #{conversationId} "
            + "  OR (#{type} = 0 AND (from_user_id = #{userId} OR to_id = #{userId}))) "
            + "ORDER BY seq DESC LIMIT #{limit}")
    List<Message> selectHistoryByConversation(@Param("conversationId") Long conversationId,
                                               @Param("type") Integer type,
                                               @Param("userId") Long userId,
                                               @Param("cursorSeq") Long cursorSeq,
                                               @Param("limit") int limit);

    /**
     * 增量同步
     */
    @Select("SELECT * FROM message WHERE conversation_type = #{type} "
            + "AND seq > #{sinceSeq} AND is_deleted = 0 "
            + "AND (conversation_id = #{conversationId} "
            + "  OR (#{type} = 0 AND (from_user_id = #{userId} OR to_id = #{userId}))) "
            + "ORDER BY seq ASC LIMIT #{limit}")
    List<Message> selectSyncByConversation(@Param("conversationId") Long conversationId,
                                            @Param("type") Integer type,
                                            @Param("userId") Long userId,
                                            @Param("sinceSeq") Long sinceSeq,
                                            @Param("limit") int limit);
}
