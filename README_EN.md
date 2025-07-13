# 🧠 Java Search Engine

This is the final project of the "Java Developer from Scratch" course.  
A local search engine has been implemented that crawls specified websites, extracts page content, performs text lemmatization, stores data in a database, and provides keyword search with relevance ranking and snippets.

## 🔍 Main Features

- Multithreaded website crawling using ForkJoinPool
- Text lemmatization (Russian language) via Apache Lucene Morphology
- Indexing pages into MySQL database
- REST API for starting/stopping indexing and performing searches
- Calculation of search result relevance
- Snippet generation
- Simple web admin interface using Thymeleaf for management

## ⚙️ Technologies Used

- Java 17+
- Spring Boot 2.7+
- Spring Data JPA
- MySQL 8+
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
    username: root
    password: "password"
    url: jdbc:mysql://localhost:3306/search_engine?useSSL=false&requireSSL=false&allowPublicKeyRetrieval=true
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate.dialect: org.hibernate.dialect.MySQL8Dialect

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
- `spring.datasource.*` — MySQL database connection settings
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

#### 🚀 Local Project Launch

**Requirements:**

- Java 17 or higher
- Maven
- MySQL (local or remote)

**Steps to Launch:**

1. **Clone the repository**

    ```bash
    git clone <repository_URL>
    cd <project_folder_name>
    ```

2. **Set up the database**

    - Make sure MySQL is running and accessible
    - Create a database named `search_engine` (or other name if changed in `application.yaml`)
    - Check DB credentials in `application.yaml`

3. **Build and run the project**

    ```bash
    mvn clean install
    mvn spring-boot:run
    ```

4. **Access the application**

    - Open `http://localhost:8080` in your browser
    - Web UI will be available on the main page

5. **Use the API**

    - Start full indexing:  
      `http://localhost:8080/api/startIndexing`
    - Perform search:  
      `http://localhost:8080/api/search?query=your_query`

**Optional API:**

- Stop indexing:  
  `http://localhost:8080/api/stopIndexing`

- Index a single page (POST request):  
  `http://localhost:8080/api/indexPage` with URL param

---

If needed, adjust log level in `application.yaml` and watch the console for debugging.