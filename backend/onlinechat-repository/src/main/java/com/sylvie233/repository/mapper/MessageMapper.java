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
     * 查询会话的最新消息（按 seq 降序取前 N 条）
     */
    @Select("SELECT * FROM message WHERE conversation_id = #{conversationId} "
            + "AND conversation_type = #{type} AND is_deleted = 0 "
            + "ORDER BY seq DESC LIMIT #{limit}")
    List<Message> selectLatestByConversation(@Param("conversationId") Long conversationId,
                                              @Param("type") Integer type,
                                              @Param("limit") int limit);

    /**
     * 查询会话历史消息（向前翻页，seq 游标分页：seq < cursorSeq）
     */
    @Select("SELECT * FROM message WHERE conversation_id = #{conversationId} "
            + "AND conversation_type = #{type} AND seq < #{cursorSeq} AND is_deleted = 0 "
            + "ORDER BY seq DESC LIMIT #{limit}")
    List<Message> selectHistoryByConversation(@Param("conversationId") Long conversationId,
                                               @Param("type") Integer type,
                                               @Param("cursorSeq") Long cursorSeq,
                                               @Param("limit") int limit);

    /**
     * 增量同步 — 获取 seq > sinceSeq 的新消息（正序）
     */
    @Select("SELECT * FROM message WHERE conversation_id = #{conversationId} "
            + "AND conversation_type = #{type} AND seq > #{sinceSeq} AND is_deleted = 0 "
            + "ORDER BY seq ASC LIMIT #{limit}")
    List<Message> selectSyncByConversation(@Param("conversationId") Long conversationId,
                                            @Param("type") Integer type,
                                            @Param("sinceSeq") Long sinceSeq,
                                            @Param("limit") int limit);
}
