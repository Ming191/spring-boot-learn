package vn.amela.employeeservice.kafka;

public final class EmployeeKafkaTopic {
    public static final String EMPLOYEE_CREATED = "employee.created";
    public static final String EMPLOYEE_STATUS_CHANGED = "employee.status.changed";
    public static final String EMPLOYEE_DEACTIVATED = "employee.deactivated";

    private EmployeeKafkaTopic() {
    }
}
