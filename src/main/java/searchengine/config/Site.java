package searchengine.config;

import lombok.*;

@Setter
@Getter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Site {
    private String url;
    private String name;
}
