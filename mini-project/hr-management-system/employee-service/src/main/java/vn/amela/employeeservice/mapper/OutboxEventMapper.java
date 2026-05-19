package vn.amela.employeeservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vn.amela.employeeservice.entity.OutboxEvent;

import java.util.List;

@Mapper
public interface OutboxEventMapper {
    void insert(@Param("outboxEvent") OutboxEvent outboxEvent);
    List<OutboxEvent> findPending(@Param("limit") int limit);
    int markPublished(@Param("id") Long id);
    int markFailed(@Param("id") Long id);
}
