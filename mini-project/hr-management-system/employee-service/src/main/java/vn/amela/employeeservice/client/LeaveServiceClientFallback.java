package vn.amela.employeeservice.client;

import org.springframework.stereotype.Component;
import vn.amela.employeeservice.dto.response.PageResponse;
import vn.amela.employeeservice.exception.BusinessException;

@Component
public class LeaveServiceClientFallback implements LeaveServiceClient {
    @Override
    public PageResponse<Object> findLeaves(Long employeeId, String status, int page, int size) {
        throw new BusinessException("Cannot verify leave status: leave-service unavailable");
    }
}
