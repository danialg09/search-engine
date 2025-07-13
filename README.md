# 🧠 Поисковый движок на Java

Это итоговый проект курса «Java-разработчик с нуля». Реализован локальный поисковый движок, который выполняет обход заданных сайтов, извлекает содержимое страниц, производит лемматизацию текста, сохраняет данные в базу и предоставляет поиск по ключевым словам с релевантностью и сниппетами.

## 🔍 Основной функционал

- Многопоточный обход сайтов с использованием ForkJoinPool
- Лемматизация текста (русский язык) через Apache Lucene Morphology
- Индексация страниц в базу данных MySQL
- REST API для запуска/остановки индексации и выполнения поиска
- Расчёт релевантности результатов
- Генерация сниппетов
- Простая веб-админка на Thymeleaf для управления

## ⚙️ Используемые технологии

- Java 17+
- Spring Boot 2.7+
- Spring Data JPA
- MySQL 8+
- Jsoup
- Apache Lucene Morphology
- Lombok
- Maven

## 🧪 Основные команды API

| Метод | URL               | Описание                              |
|-------|-------------------|-------------------------------------|
| GET   | /api/statistics    | Получить текущую статистику          |
| GET   | /api/startIndexing | Запуск полной индексации всех сайтов |
| GET   | /api/stopIndexing  | Прерывание текущей индексации        |
| POST  | /api/indexPage     | Индексация одной страницы            |
| GET   | /api/search        | Поиск по словам (с поддержкой морфологии) |

## 🏗️ Архитектура проекта

- **Controllers** — REST-контроллеры для API и UI
- **DTO** — классы запросов и ответов (statistics, search, errors)
- **Services**
    - IndexingService — многопоточный обход и индексирование
    - LemmaService — морфологический анализ и работа с леммами
    - SearchService — поиск по леммам с ранжированием
    - SiteDataService — работа с сайтами и страницами
    - StatisticsService — генерация статистики
- **Model** — JPA-сущности: SiteEntity, Page, Lemma, Index
- **Repositories** — Spring Data репозитории для каждой сущности
- **Resources**
    - templates/index.html — веб-интерфейс
    - static/ — JS, CSS, изображения
    - application.yaml — конфигурация запуска

## 📈 Алгоритм поиска

1. Запрос → лемматизация → отфильтровка по стоп-словам
2. Ищутся все страницы, содержащие каждую из лемм
3. Вычисляется абсолютная и относительная релевантность
4. Результаты сортируются по убыванию
5. Формируется сниппет (с фрагментом текста и выделением `<b>`)

## 📌 Примечания

- При ошибке индексации страница помечается статусом FAILED, с сообщением.
- Запросы с ошибочными ссылками (4xx, 5xx) игнорируются.
- Можно запустить индексацию одной страницы через веб-интерфейс или API.

## ⚙️ Настройка конфигурации

В проекте используется файл `application.yaml` для настройки сервера, подключения к базе данных, параметров индексации и поискового движка.

Пример конфигурации:

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
### Основные параметры:

- `server.port` — порт, на котором запускается приложение  
- `spring.datasource.*` — настройки подключения к базе данных MySQL  
- `spring.jpa.hibernate.ddl-auto` — режим управления схемой базы данных  
  - Например, `update` автоматически обновляет схему  
- `indexing-settings.sites` — список сайтов для обхода с их URL и названиями  
- `search-engine.user-agent` — User-Agent для запросов при индексации  
- `search-engine.referrer` — заголовок Referer для HTTP-запросов  
- `search-engine.waitingTime` — время ожидания между запросами к сайту  
- `search-engine.timeout` — таймаут HTTP-запросов в миллисекундах  
- `search-engine.maxDepth` — максимальная глубина обхода ссылок  
- `logging.level.root` — уровень логирования приложения  

## 🚀 Инструкция по локальному запуску проекта

### Требования

- Java 17 или выше
- Maven
- MySQL (локально или удалённо)

### Шаги запуска

1. **Клонировать репозиторий**

    ```bash
    git clone <URL_репозитория>
    cd <название_папки_проекта>
    ```

2. **Настроить базу данных**

    - Убедитесь, что MySQL запущен и доступен
    - Создайте базу данных с именем `search_engine` (или другим, если измените в `application.yaml`)
    - Проверьте настройки подключения в `application.yaml` (логин, пароль, URL)

3. **Собрать проект и запустить**

    ```bash
    mvn clean install
    mvn spring-boot:run
    ```

4. **Проверить запуск**

    - По умолчанию приложение будет доступно по адресу:  
      `http://localhost:8080`
    - Веб-интерфейс доступен по корневому пути или согласно настройкам

5. **Использовать API**

    - Для запуска полной индексации отправьте GET-запрос:  
      `http://localhost:8080/api/startIndexing`
    - Для поиска используйте:  
      `http://localhost:8080/api/search?query=ваш_запрос`

---

### Дополнительно

- Для остановки индексации:  
  `http://localhost:8080/api/stopIndexing`

- Для индексирования одной страницы (POST-запрос):  
  `http://localhost:8080/api/indexPage` с параметром URL страницы

---

Если возникнут вопросы, проверьте логи в консоли или настройте уровень логирования в `application.yaml`.
