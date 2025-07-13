package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "search-engine")
public class SearchEngineProperties {

    private String userAgent;

    private String referrer;

    private Duration waitingTime;

    private int timeout;

    private int maxDepth;
}
