[Russian Version](README_RU.md)

# 🧠 Java Search Engine

A local search engine has been implemented that crawls specified websites, extracts page content, performs text lemmatization, stores data in a database, and provides keyword search with relevance ranking and snippets.

## 🔍 Main Features

- Multithreaded website crawling using ForkJoinPool
- Text lemmatization (Russian language) via Apache Lucene Morphology
- Indexing pages into database
- REST API for starting/stopping indexing and performing searches
- Calculation of search result relevance
- Snippet generation
- Simple web admin interface using Thymeleaf for management

## ⚙️ Technologies Used

- Java 21+
- Spring Boot 3.5+
- Spring Data JPA
- PostgreSQL 15+
- Liquibase
- Jsoup
- Apache Lucene Morphology
- Lombok
- Maven

## 🧪 Main API Endpoints

| Method | URL               | Description                          |
|--------|-------------------|------------------------------------|
| GET    | /api/statistics    | Get current statistics              |
| GET    | /api/startIndexing | Start full indexing of all sites   |
| GET    | /api/stopIndexing  | Stop current indexing process       |
| POST   | /api/indexPage     | Index a single page                 |
| GET    | /api/search        | Search by keywords (with morphology support) |

## 🏗️ Project Architecture

- **Controllers** — REST controllers for API and UI
- **DTO** — request and response classes (statistics, search, errors)
- **Services**
    - IndexingService — multithreaded crawling and indexing
    - LemmaService — morphological analysis and lemma processing
    - SearchService — lemma-based search with ranking
    - SiteDataService — managing sites and pages
    - StatisticsService — statistics generation
- **Model** — JPA entities: SiteEntity, Page, Lemma, Index
- **Repositories** — Spring Data repositories for each entity
- **Resources**
    - `templates/index.html` — web interface
    - `static/` — JS, CSS, images
    - `application.yaml` — launch configuration

## 📈 Search Algorithm

1. Query → lemmatization → filtering by stop words
2. Find all pages containing each lemma
3. Calculate absolute and relative relevance
4. Sort results by descending relevance
5. Generate snippet (with text fragment and `<b>` highlights)

## 📌 Notes

- If indexing fails, the page is marked with status FAILED and an error message
- Requests to broken links (4xx, 5xx) are ignored
- You can start indexing a single page via the web interface or API

## ⚙️ Configuration

The project uses an `application.yaml` file to configure the server, database connection, indexing, and search engine settings.

Example configuration:

```yaml
server:
  port: 8080

spring:
  datasource:
    username: postgres
    password: postgres
    url: jdbc:postgresql://localhost:5433/search_engine
    hikari:
      maximum-pool-size: 96
      connection-timeout: 40000
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
    hibernate:
      ddl-auto: none
    show-sql: false
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    enabled: true
    drop-first: false
indexing-settings:
  sites:
    - url: https://www.playback.ru
      name: PlayBack.Ru
    - url: https://dimonvideo.ru
      name: DimonVideo.ru
    - url: https://www.svetlovka.ru/
      name: Svetlovka.ru

search-engine:
  user-agent: MersinaSearchBot/1.0 (+https://example.com/bot)
  referrer: http://www.google.com
  waitingTime: 5s
  timeout: 5000
  maxDepth: 3

logging.level.root: INFO
```
### 🔧 Key Parameters & Local Launch Instructions

- `server.port` — port on which the application runs
- `spring.datasource.*` — database connection settings
- `spring.jpa.hibernate.ddl-auto` — database schema management mode
  - e.g. `update` automatically updates the schema
- `indexing-settings.sites` — list of sites to crawl with URLs and names
- `indexing-settings.sites` — list of sites to crawl with URLs and names
- `search-engine.user-agent` — User-Agent header for crawling requests
- `search-engine.referrer` — HTTP Referer header for requests
- `search-engine.waitingTime` — delay between requests to a site
- `search-engine.timeout` — HTTP request timeout in milliseconds
- `search-engine.maxDepth` — maximum link crawl depth
- `logging.level.root` — application logging level

## 🚀 Local Project Launch

### Requirements

- Java 21 or higher
- Docker & Docker Compose
- Maven

### Steps to Run

1. **Clone the repository**

    ```bash
    git clone <REPO_URL>
    cd <project_folder_name>
    ```

2. **Start the Database**

   The project uses PostgreSQL via Docker.

     ```bash
    cd docker
    docker compose up -d
    cd ..
    ```
   > **Note:** The database will be available on port **5433** (to avoid conflicts with local Postgres on 5432).

3. **Build and Run the Project**

    ```bash
    mvn clean install
    mvn spring-boot:run
    ```

4. **Verify Launch**

    - By default, the application is available at:  
      `http://localhost:8080`

5. **Use the API**

    - **Start full indexing** (GET):  
      `http://localhost:8080/api/startIndexing`
    - **Search** (GET):  
      `http://localhost:8080/api/search?query=your_query`
    - **Stop indexing** (GET):  
      `http://localhost:8080/api/stopIndexing`
    - **Index specific page** (POST):  
      `http://localhost:8080/api/indexPage` (parameter `url`)

---

If needed, adjust log level in `application.yaml` and watch the console for debugging.