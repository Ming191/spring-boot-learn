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
import vn.amela.leaveservice.entity.LeaveRejectedPayload;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private static final String LEAVE_REQUESTED_EVENT = "leave.requested";
    private static final String LEAVE_CANCELLED_EVENT = "leave.cancelled";
    private static final String LEAVE_REJECTED_EVENT = "leave.rejected";
    private static final String LEAVE_APPROVED_EVENT = "leave.approved";
    private static final String LEAVE_AGGREGATE_TYPE = "LeaveRequest";
    private static final String DEFAULT_SORT_BY = "createdAt";
    private static final String DEFAULT_SORT_DIRECTION = "desc";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "employeeId",
            "employeeCode",
            "employeeName",
            "departmentName",
            "leaveType",
            "fromDate",
            "toDate",
            "totalDays",
            "status",
            "reviewedAt",
            "createdAt"
    );


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
        requireHrRole(user);

        LeaveFilterRequest normalizedFilter = normalizeFilter(filter);
        List<LeaveResponse> items = leaveMapper.search(normalizedFilter)
                .stream()
                .map(this::toResponse)
                .toList();
        long totalElements = leaveMapper.countByFilter(normalizedFilter);
        int totalPages = (int) Math.ceil((double) totalElements / normalizedFilter.size());

        return PageResponse.<LeaveResponse>builder()
                .items(items)
                .page(normalizedFilter.page())
                .size(normalizedFilter.size())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    @Override
    public PageResponse<LeaveResponse> findMyLeaves(CurrentUser user, int page, int size) {
        requireLeaveViewerRole(user);

        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = normalizedPage * normalizedSize;

        EmployeeSnapshotResponse employee = employeeSnapshotService.getEmployeeSnapshotByAuthUserId(user.userId());
        List<LeaveResponse> items = leaveMapper.findByEmployeeId(employee.id(), normalizedSize, offset)
                .stream()
                .map(this::toResponse)
                .toList();
        long totalElements = leaveMapper.countByEmployeeId(employee.id());
        int totalPages = (int) Math.ceil((double) totalElements / normalizedSize);

        return PageResponse.<LeaveResponse>builder()
                .items(items)
                .page(normalizedPage)
                .size(normalizedSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    @Override
    public LeaveResponse getById(Long id, CurrentUser user) {
        requireLeaveViewerRole(user);

        LeaveRequest leaveRequest = loadLeaveRequest(id);
        if (!user.isHr()) {
            EmployeeSnapshotResponse employee = employeeSnapshotService.getEmployeeSnapshotByAuthUserId(user.userId());
            if (!leaveRequest.getEmployeeId().equals(employee.id())) {
                throw new ForbiddenActionException("You can only view your own leave requests");
            }
        }

        return toResponse(leaveRequest);
    }

    @Override
    @Transactional
    public LeaveResponse approve(Long id, ReviewLeaveRequest request, CurrentUser user) {
        requireHrRole(user);

        loadLeaveRequest(id);
        int updatedRows = leaveMapper.approve(
                id,
                user.userId(),
                request == null ? null : request.reviewerNote(),
                LocalDateTime.now()
        );
        if (updatedRows == 0) {
            throw new BusinessException("Only pending leave requests can be approved");
        }

        LeaveRequest approvedLeaveRequest = loadLeaveRequest(id);
        saveOutboxEvent(LEAVE_AGGREGATE_TYPE, id, LEAVE_APPROVED_EVENT, approvedLeaveRequest);

        return toResponse(approvedLeaveRequest);
    }

    @Override
    @Transactional
    public LeaveResponse reject(Long id, RejectLeaveRequest request, CurrentUser user) {
        requireHrRole(user);
        String reviewerNote = normalizeRequiredText(
                request == null ? null : request.reviewerNote(),
                "Reviewer note"
        );

        loadLeaveRequest(id);
        int updatedRows = leaveMapper.reject(id, user.userId(), reviewerNote, LocalDateTime.now());
        if (updatedRows == 0) {
            throw new BusinessException("Only pending leave requests can be rejected");
        }

        LeaveRequest rejectedLeaveRequest = loadLeaveRequest(id);
        saveLeaveRejectedEvent(rejectedLeaveRequest);

        return toResponse(rejectedLeaveRequest);
    }

    @Override
    @Transactional
    public LeaveResponse cancel(Long id, CurrentUser user) {
        requireEmployeeRoleForCancel(user);

        LeaveRequest currentLeaveRequest = loadLeaveRequest(id);
        EmployeeSnapshotResponse employee = employeeSnapshotService.getEmployeeSnapshotByAuthUserId(user.userId());
        if (!currentLeaveRequest.getEmployeeId().equals(employee.id())) {
            throw new ForbiddenActionException("You can only cancel your own leave requests");
        }

        int updatedRows = leaveMapper.cancel(id, employee.id());
        if (updatedRows == 0) {
            throw new BusinessException("Only pending leave requests can be cancelled");
        }

        LeaveRequest cancelledLeaveRequest = loadLeaveRequest(id);
        saveOutboxEvent(LEAVE_AGGREGATE_TYPE, id, LEAVE_CANCELLED_EVENT, cancelledLeaveRequest);

        return toResponse(cancelledLeaveRequest);
    }

    private void requireLeaveCreatorRole(CurrentUser user) {
        if (!user.isHr() && !user.isEmployee()) {
            throw new ForbiddenActionException("Only HR or Employee can create leave requests");
        }
    }

    private String normalizeRequiredText(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(fieldName + " is required");
        }
        return text.trim();
    }

    private void requireLeaveViewerRole(CurrentUser user) {
        if (user == null || (!user.isHr() && !user.isEmployee())) {
            throw new ForbiddenActionException("Only HR or Employee can view leave requests");
        }
    }

    private void requireEmployeeRoleForCancel(CurrentUser user) {
        if (user == null || !user.isEmployee()) {
            throw new ForbiddenActionException("Only Employee can cancel leave requests");
        }
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }

    private void requireHrRole(CurrentUser user) {
        if (user == null || !user.isHr()) {
            throw new ForbiddenActionException("Only HR can search leave requests");
        }
    }

    private LeaveFilterRequest normalizeFilter(LeaveFilterRequest filter) {
        LeaveFilterRequest currentFilter = filter == null
                ? LeaveFilterRequest.builder().build()
                : filter;

        if (currentFilter.fromDate() != null
                && currentFilter.toDate() != null
                && currentFilter.toDate().isBefore(currentFilter.fromDate())) {
            throw new BusinessException("To date must be greater than or equal to from date");
        }

        return LeaveFilterRequest.builder()
                .employeeId(currentFilter.employeeId())
                .status(currentFilter.status())
                .leaveType(currentFilter.leaveType())
                .fromDate(currentFilter.fromDate())
                .toDate(currentFilter.toDate())
                .departmentName(normalizeOptionalText(currentFilter.departmentName()))
                .page(currentFilter.page())
                .size(currentFilter.size())
                .sortBy(normalizeSortBy(currentFilter.sortBy()))
                .sortDirection(normalizeSortDirection(currentFilter.sortDirection()))
                .build();
    }

    private String normalizeSortBy(String sortBy) {
        String normalizedSortBy = normalizeOptionalText(sortBy);
        if (normalizedSortBy == null || !ALLOWED_SORT_FIELDS.contains(normalizedSortBy)) {
            return DEFAULT_SORT_BY;
        }
        return normalizedSortBy;
    }

    private String normalizeSortDirection(String sortDirection) {
        String normalizedSortDirection = normalizeOptionalText(sortDirection);
        if (normalizedSortDirection == null) {
            return DEFAULT_SORT_DIRECTION;
        }
        return "asc".equalsIgnoreCase(normalizedSortDirection) ? "asc" : DEFAULT_SORT_DIRECTION;
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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

    private void saveLeaveRejectedEvent(LeaveRequest leaveRequest) {
        LeaveRejectedPayload payload = LeaveRejectedPayload.builder()
                .eventType(LEAVE_REJECTED_EVENT)
                .aggregateType(LEAVE_AGGREGATE_TYPE)
                .aggregateId(leaveRequest.getId())
                .employeeId(leaveRequest.getEmployeeId())
                .employeeName(leaveRequest.getEmployeeName())
                .reviewerNote(leaveRequest.getReviewerNote())
                .timestamp(Instant.now())
                .build();

        saveOutboxEvent(
                LEAVE_AGGREGATE_TYPE,
                leaveRequest.getId(),
                LEAVE_REJECTED_EVENT,
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

    private LeaveRequest loadLeaveRequest(Long id) {
        return leaveMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
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
