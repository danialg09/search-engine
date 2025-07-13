package searchengine.dto.searching;

import lombok.Data;
import searchengine.model.Page;

@Data
public class RelevanceItem {
    private final Page page;
    private final double relevance;
}
