package vn.amela.leaveservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vn.amela.leaveservice.dto.request.LeaveFilterRequest;
import vn.amela.leaveservice.entity.LeaveRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface LeaveMapper {
    void insert(LeaveRequest leaveRequest);
    Optional<LeaveRequest> findById(@Param("id") Long id);
    List<LeaveRequest> search(LeaveFilterRequest filter);
    long countByFilter(LeaveFilterRequest filter);
    List<LeaveRequest> findByEmployeeId(
            @Param("employeeId") Long employeeId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );
    long countByEmployeeId(@Param("employeeId") Long employeeId);
    boolean existsOverlappingLeave(
            @Param("employeeId") Long employeeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
    int countPendingByEmployeeId(@Param("employeeId") Long employeeId);
    int approve(
            @Param("id") Long id,
            @Param("reviewedBy") Long reviewedBy,
            @Param("reviewerNote") String reviewerNote,
            @Param("reviewedAt") LocalDateTime reviewedAt
    );
    int reject(
            @Param("id") Long id,
            @Param("reviewedBy") Long reviewedBy,
            @Param("reviewerNote") String reviewerNote,
            @Param("reviewedAt") LocalDateTime reviewedAt
    );
    int cancel(@Param("id") Long id);
}
