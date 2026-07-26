# Content Organizer

A Spring Boot REST API for organizing and managing content such as articles, videos, courses, and conference talks. Each piece of content supports multiple sources, enabling well-documented and reference-rich entries.

Based on the project by [Dan Vega](https://www.danvega.dev/).

## Tech Stack

- Java 21
- Spring Boot 4.1.0
- Spring Data JPA + PostgreSQL
- Spring Boot Starter Validation

## Getting Started

### Prerequisites

- Java 21+
- PostgreSQL running locally on port `5432`
- A database named `postgres` with user `admin`

### Run

```bash
./mvnw spring-boot:run
```

The API starts at `http://localhost:8080`.

## Content Model

| Field        | Type                                          | Description                                     |
|--------------|-----------------------------------------------|-------------------------------------------------|
| id           | Integer                                       | Auto-generated primary key                      |
| title        | String                                        | Required, cannot be blank                       |
| description  | String                                        | Optional                                        |
| status       | `IDEA`, `IN_PROGRESS`, `COMPLETED`, `PUBLISHED` | Current stage of the content               |
| contentType  | `ARTICLE`, `VIDEO`, `COURSE`, `CONFERENCE_TALK` | The format of the content                   |
| sources      | List\<String\>                                | Multiple reference URLs supporting the content  |
| dateCreated  | LocalDateTime                                 | Set automatically on creation                   |
| dateUpdated  | LocalDateTime                                 | Set automatically on update                     |

## API Endpoints

| Method   | Endpoint                        | Description                    |
|----------|---------------------------------|--------------------------------|
| `GET`    | `/api/content`                  | List all content               |
| `GET`    | `/api/content/{id}`             | Get content by ID              |
| `POST`   | `/api/content`                  | Create new content             |
| `PUT`    | `/api/content/{id}`             | Update existing content        |
| `DELETE` | `/api/content/{id}`             | Delete content by ID           |
| `GET`    | `/api/content/filter/{keyword}` | Search content by title        |
| `GET`    | `/api/content/filter/status/{status}` | Filter content by status |

### Create Content

```json
POST /api/content
{
  "title": "Spring Security Guide",
  "description": "A comprehensive guide to securing Spring apps",
  "status": "PUBLISHED",
  "contentType": "ARTICLE",
  "dateCreated": "2024-01-15T10:30:00",
  "url": [
    "https://example.com/part-1",
    "https://example.com/part-2"
  ]
}
```

## License

This project is based on work by Dan Vega. All rights to the original base project belong to him.
