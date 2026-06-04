package vn.amela.employeeservice.client;

import org.springframework.stereotype.Component;
import vn.amela.employeeservice.exception.ServiceUnavailableException;

@Component
public class LeaveServiceClientFallback implements LeaveServiceClient {
    @Override
    public int countPendingLeavesByEmployeeId(Long employeeId) {
        throw new ServiceUnavailableException("Leave service is currently unavailable. Please try again later.");
    }
}
