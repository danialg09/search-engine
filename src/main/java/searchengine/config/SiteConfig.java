package searchengine.config;

import lombok.*;

@Setter
@Getter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SiteConfig {
    private String url;
    private String name;
}
