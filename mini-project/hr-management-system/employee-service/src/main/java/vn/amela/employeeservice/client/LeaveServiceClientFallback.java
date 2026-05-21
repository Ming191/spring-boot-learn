package vn.amela.employeeservice.client;

import org.springframework.stereotype.Component;
import vn.amela.employeeservice.exception.BusinessException;

@Component
public class LeaveServiceClientFallback implements LeaveServiceClient {
    @Override
    public int countPendingLeavesByEmployeeId(Long employeeId) {
        throw new BusinessException("Cannot verify leave status: leave-service unavailable");
    }
}
