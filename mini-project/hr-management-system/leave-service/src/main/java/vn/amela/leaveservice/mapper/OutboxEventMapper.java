package vn.amela.leaveservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vn.amela.leaveservice.entity.OutboxEvent;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OutboxEventMapper {
    void insert(OutboxEvent outboxEvent);

    List<Long> findPendingIds(
            @Param("limit") int limit,
            @Param("processingTimeoutAt") LocalDateTime processingTimeoutAt
    );

    int claimPending(
            @Param("id") Long id,
            @Param("processingTimeoutAt") LocalDateTime processingTimeoutAt
    );

    OutboxEvent findById(@Param("id") Long id);

    int markPublished(@Param("id") Long id);

    int markFailed(
            @Param("id") Long id,
            @Param("lastError") String lastError
    );

    int markRetry(
            @Param("id") Long id,
            @Param("lastError") String lastError,
            @Param("nextRetryAt") LocalDateTime nextRetryAt
    );
}
