package searchengine.dto.indexing;

import org.jsoup.Connection;

public record PageData(Connection connection, int statusCode) {
}
