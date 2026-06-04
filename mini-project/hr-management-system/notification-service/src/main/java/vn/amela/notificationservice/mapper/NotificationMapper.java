package vn.amela.notificationservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import vn.amela.notificationservice.entity.Notification;

@Mapper
public interface NotificationMapper {
    int insertIgnore(Notification notification);
}
