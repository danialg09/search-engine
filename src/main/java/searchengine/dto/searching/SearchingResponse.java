package searchengine.dto.searching;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchingResponse {
    private String result;
    private Long count;
    private List<SearchingData> data;
}
