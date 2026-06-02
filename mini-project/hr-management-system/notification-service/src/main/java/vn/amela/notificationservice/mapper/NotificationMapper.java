package vn.amela.notificationservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vn.amela.notificationservice.entity.Notification;

@Mapper
public interface NotificationMapper {
    void insert(@Param("notification") Notification notification);

    boolean existsByEventId(@Param("eventId") String eventId);
}
