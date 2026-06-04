package vn.amela.employeeservice.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class LeaveServiceClientContractTest {

    @Test
    void countPendingLeavesByEmployeeId_usesPendingCountEndpointFromPlan() throws Exception {
        Method method = LeaveServiceClient.class.getMethod(
                "countPendingLeavesByEmployeeId",
                Long.class
        );

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping);
        assertArrayEquals(new String[]{"/api/leaves/pending-count"}, getMapping.value());

        RequestParam[] requestParams = method.getParameters()[0].getAnnotationsByType(RequestParam.class);
        assertEquals("employeeId", requestParams[0].value());
    }

    @Test
    void hasPendingLeavesByEmployeeId_usesPendingCount() {
        CapturingLeaveServiceClient client = new CapturingLeaveServiceClient(1);

        assertTrue(client.hasPendingLeavesByEmployeeId(10L));
        assertEquals(10L, client.employeeId);
    }

    private static class CapturingLeaveServiceClient implements LeaveServiceClient {
        private final int response;
        private Long employeeId;

        private CapturingLeaveServiceClient(int response) {
            this.response = response;
        }

        @Override
        public int countPendingLeavesByEmployeeId(Long employeeId) {
            this.employeeId = employeeId;
            return response;
        }
    }
}
