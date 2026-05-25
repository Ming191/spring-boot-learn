package vn.amela.leaveservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vn.amela.leaveservice.entity.OutboxEvent;

import java.util.List;

@Mapper
public interface OutboxEventMapper {
    void insert(OutboxEvent outboxEvent);
    List<OutboxEvent> findPending(@Param("limit") int limit);
    int markPublished(@Param("id") Long id);
    int markFailed(@Param("id") Long id);
}
