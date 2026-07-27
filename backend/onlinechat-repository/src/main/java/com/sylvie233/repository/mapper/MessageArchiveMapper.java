package com.sylvie233.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sylvie233.repository.entity.MessageArchive;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息归档 Mapper
 */
@Mapper
public interface MessageArchiveMapper extends BaseMapper<MessageArchive> {
}
