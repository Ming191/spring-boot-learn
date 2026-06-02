package vn.amela.notificationservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@ConfigurationProperties(prefix = "notification")
public class NotificationTemplateProperties {

    private Map<String, Template> templates = new HashMap<>();

    public Template template(String eventType) {
        return templates.getOrDefault(eventType, templates.get("default"));
    }

    @Setter
    @Getter
    public static class Template {
        private String recipientType;
        private String recipientIdField;
        private String title;
        private String message;

    }
}
