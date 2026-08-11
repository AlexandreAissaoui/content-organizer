#!/bin/zsh

# Test script for Content Organizer API
# Password is stored as {noop}rawpassword (plaintext, dev only)

psql -h localhost -U admin -d postgres -c 
"TRUNCATE TABLE content_sources, content_authors, Content, users; 
ALTER SEQUENCE Content_id_seq RESTART WITH 1;"

set -e

# ── Auth ────────────────────────────────────────────────────

echo "=== Register users ==="
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"member1","password":"pass123"}' | jq .

curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin1","password":"pass123"}' | jq .

echo "=== Login & save tokens ==="
MEMBER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"member1","password":"pass123"}' | jq -r '.token')

ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin1","password":"pass123"}' | jq -r '.token')

# ── Content CRUD ────────────────────────────────────────────

echo "=== GET /api/contents (MEMBER) ==="
curl -s http://localhost:8080/api/contents \
  -H "Authorization: Bearer $MEMBER_TOKEN" | jq .

echo "=== POST /api/contents (ADMIN) ==="
curl -s -X POST http://localhost:8080/api/contents \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Test Article","description":"A test","type":"ARTICLE","status":"IDEA","sources":["https://example.com"]}' | jq .

echo "=== PUT /api/contents/1 (ADMIN) ==="
curl -s -X PUT http://localhost:8080/api/contents/1 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Updated","description":"Updated desc","type":"VIDEO","status":"IN_PROGRESS"}' | jq .

echo "=== GET /api/contents/1 (MEMBER) ==="
curl -s http://localhost:8080/api/contents/1 \
  -H "Authorization: Bearer $MEMBER_TOKEN" | jq .

echo "=== GET /api/contents/filter/Test (MEMBER) ==="
curl -s "http://localhost:8080/api/contents/filter/Test" \
  -H "Authorization: Bearer $MEMBER_TOKEN" | jq .

echo "=== GET /api/contents/filter/status/IDEA (MEMBER) ==="
curl -s "http://localhost:8080/api/contents/filter/status/IDEA" \
  -H "Authorization: Bearer $MEMBER_TOKEN" | jq .

echo "=== DELETE /api/contents/1 (ADMIN) ==="
curl -s -X DELETE http://localhost:8080/api/contents/1 \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .

# ── Edge cases ──────────────────────────────────────────────

echo "=== No token (expect 401) ==="
curl -s http://localhost:8080/api/contents | jq .

echo "=== MEMBER tries POST (expect 403) ==="
curl -s -X POST http://localhost:8080/api/contents \
  -H "Authorization: Bearer $MEMBER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Hack","type":"ARTICLE","status":"IDEA"}' | jq .

echo "=== Bad token (expect 401) ==="
curl -s http://localhost:8080/api/contents \
  -H "Authorization: Bearer invalidtoken" | jq .
