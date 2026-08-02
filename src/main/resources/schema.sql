CREATE TABLE IF NOT EXISTS Content (
    id SERIAL PRIMARY KEY,
    title varchar(255) NOT NULL,
    description text,
    status VARCHAR(20) NOT NULL,
    type VARCHAR(50) NOT NULL,
    date_created TIMESTAMP NOT NULL,
    date_updated TIMESTAMP
);



CREATE TABLE IF NOT EXISTS content_sources (
    content_id INTEGER NOT NULL REFERENCES Content(id),
    source VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS content_authors (
    content_id INTEGER NOT NULL REFERENCES Content(id),
    author VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(255) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
);
