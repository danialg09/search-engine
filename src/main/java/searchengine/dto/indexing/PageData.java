package searchengine.dto.indexing;

import org.jsoup.nodes.Document;

public record PageData(Document document, int statusCode) {
}
