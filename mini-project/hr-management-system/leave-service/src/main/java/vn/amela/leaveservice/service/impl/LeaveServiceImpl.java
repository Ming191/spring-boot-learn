package vn.amela.leaveservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import vn.amela.leaveservice.dto.request.CreateLeaveRequest;
import vn.amela.leaveservice.dto.request.LeaveFilterRequest;
import vn.amela.leaveservice.dto.request.RejectLeaveRequest;
import vn.amela.leaveservice.dto.request.ReviewLeaveRequest;
import vn.amela.leaveservice.dto.response.EmployeeSnapshotResponse;
import vn.amela.leaveservice.dto.response.LeaveResponse;
import vn.amela.leaveservice.dto.response.PageResponse;
import vn.amela.leaveservice.entity.LeaveRequest;
import vn.amela.leaveservice.entity.LeaveRequestedPayload;
import vn.amela.leaveservice.entity.OutboxEvent;
import vn.amela.leaveservice.entity.enums.LeaveStatus;
import vn.amela.leaveservice.exception.BusinessException;
import vn.amela.leaveservice.exception.ForbiddenActionException;
import vn.amela.leaveservice.exception.ResourceNotFoundException;
import vn.amela.leaveservice.mapper.LeaveMapper;
import vn.amela.leaveservice.mapper.OutboxEventMapper;
import vn.amela.leaveservice.security.CurrentUser;
import vn.amela.leaveservice.service.EmployeeSnapshotService;
import vn.amela.leaveservice.service.LeaveService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private static final String LEAVE_REQUESTED_EVENT = "leave.requested";
    private static final String LEAVE_AGGREGATE_TYPE = "LeaveRequest";


    private final LeaveMapper leaveMapper;
    private final OutboxEventMapper outboxEventMapper;
    private final EmployeeSnapshotService employeeSnapshotService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public LeaveResponse create(CreateLeaveRequest request, CurrentUser user) {
        requireLeaveCreatorRole(user);
        validateCreateRequest(request);

        EmployeeSnapshotResponse employee = employeeSnapshotService.getEmployeeSnapshotByAuthUserId(user.userId());

        boolean overlapped = leaveMapper.existsOverlappingLeave(
                employee.id(),
                request.fromDate(),
                request.toDate()
        );

        if (overlapped) {
            throw new BusinessException("Leave request overlaps with an existing pending or approved leave");
        }

        int totalDays = Math.toIntExact(
                ChronoUnit.DAYS.between(request.fromDate(), request.toDate()) + 1
        );

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employeeId(employee.id())
                .employeeCode(employee.employeeCode())
                .employeeName(employee.fullName())
                .departmentName(employee.departmentName())
                .leaveType(request.leaveType())
                .fromDate(request.fromDate())
                .toDate(request.toDate())
                .totalDays(totalDays)
                .reason(request.reason())
                .status(LeaveStatus.PENDING)
                .build();

        leaveMapper.insert(leaveRequest);

        LeaveRequest createdLeaveRequest = loadCreatedLeaveRequest(leaveRequest.getId());

        saveLeaveRequestedEvent(createdLeaveRequest);

        return toResponse(createdLeaveRequest);
    }

    @Override
    public PageResponse<LeaveResponse> search(LeaveFilterRequest filter, CurrentUser user) {
        return null;
    }

    @Override
    public PageResponse<LeaveResponse> findMyLeaves(CurrentUser user, int page, int size) {
        return null;
    }

    @Override
    public LeaveResponse getById(Long id, CurrentUser user) {
        return null;
    }

    @Override
    public LeaveResponse approve(Long id, ReviewLeaveRequest request, CurrentUser user) {
        return null;
    }

    @Override
    public LeaveResponse reject(Long id, RejectLeaveRequest request, CurrentUser user) {
        return null;
    }

    @Override
    public LeaveResponse cancel(Long id, CurrentUser user) {
        return null;
    }

    private void requireLeaveCreatorRole(CurrentUser user) {
        if (!user.isHr() && !user.isEmployee()) {
            throw new ForbiddenActionException("Only HR or Employee can create leave requests");
        }
    }

    private void saveLeaveRequestedEvent(LeaveRequest leaveRequest) {
        LeaveRequestedPayload payload = new LeaveRequestedPayload(
                LEAVE_REQUESTED_EVENT,
                LEAVE_AGGREGATE_TYPE,
                leaveRequest.getId(),
                leaveRequest.getEmployeeId(),
                leaveRequest.getEmployeeCode(),
                leaveRequest.getEmployeeName(),
                leaveRequest.getDepartmentName(),
                leaveRequest.getLeaveType(),
                leaveRequest.getFromDate(),
                leaveRequest.getToDate(),
                leaveRequest.getTotalDays(),
                leaveRequest.getStatus(),
                Instant.now()
        );

        saveOutboxEvent(
                LEAVE_AGGREGATE_TYPE,
                leaveRequest.getId(),
                LEAVE_REQUESTED_EVENT,
                payload
        );
    }

    private void validateCreateRequest(CreateLeaveRequest request) {
        if (request == null) {
            throw new BusinessException("Leave request body is required");
        }

        if (request.fromDate() == null) {
            throw new BusinessException("From date is required");
        }

        if (request.toDate() == null) {
            throw new BusinessException("To date is required");
        }

        if (request.toDate().isBefore(request.fromDate())) {
            throw new BusinessException("To date must be greater than or equal to from date");
        }

        if (request.fromDate().isBefore(LocalDate.now())) {
            throw new BusinessException("From date must not be in the past");
        }
    }

    private void saveOutboxEvent(
            String aggregateType,
            Long aggregateId,
            String eventType,
            Object payload
    ) {
        String serializedPayload;
        try {
            serializedPayload = objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new BusinessException("Failed to serialize outbox event payload", exception);
        }

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(serializedPayload)
                .build();

        outboxEventMapper.insert(event);
    }

    private LeaveRequest loadCreatedLeaveRequest(Long id) {
        return leaveMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Created leave request not found"));
    }

    private LeaveResponse toResponse(LeaveRequest request) {
        return LeaveResponse.builder()
                .id(request.getId())
                .employeeId(request.getEmployeeId())
                .employeeCode(request.getEmployeeCode())
                .employeeName(request.getEmployeeName())
                .departmentName(request.getDepartmentName())
                .leaveType(request.getLeaveType())
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .totalDays(request.getTotalDays())
                .reason(request.getReason())
                .status(request.getStatus())
                .reviewedBy(request.getReviewedBy())
                .reviewerNote(request.getReviewerNote())
                .reviewedAt(request.getReviewedAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
