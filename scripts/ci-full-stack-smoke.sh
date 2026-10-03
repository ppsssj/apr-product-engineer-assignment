#!/usr/bin/env bash
set -euo pipefail

: "${BACKEND_PORT:?BACKEND_PORT must be set}"
: "${FRONTEND_PORT:?FRONTEND_PORT must be set}"

docker compose config -q
docker compose up -d --build --wait --wait-timeout 240
docker compose ps

for service in db backend frontend; do
  state="$(docker compose ps --format '{{.State}}' "$service")"
  health="$(docker compose ps --format '{{.Health}}' "$service")"
  test "$state" = "running"
  test "$health" = "healthy"
done

python_bin="python3"
if ! "$python_bin" --version >/dev/null 2>&1; then
  python_bin="python"
fi

"$python_bin" - "$BACKEND_PORT" "$FRONTEND_PORT" <<'PY'
import html
import json
import sys
from urllib.error import HTTPError
from urllib.request import urlopen

backend_port, frontend_port = sys.argv[1:]


def request(url):
    try:
        with urlopen(url, timeout=20) as response:
            return response.status, response.read().decode("utf-8")
    except HTTPError as error:
        return error.code, error.read().decode("utf-8", errors="replace")


health_status, health_body = request(f"http://localhost:{backend_port}/health")
assert health_status == 200, (health_status, health_body)
health = json.loads(health_body)
assert health.get("status") == "ok", health
assert health.get("db") == "up", health

api_status, api_body = request(
    f"http://localhost:{backend_port}/api/posts?limit=1&sort=latest"
)
assert api_status == 200, (api_status, api_body)
latest = json.loads(api_body)
items = latest.get("items")
assert isinstance(items, list) and len(items) >= 1, latest
assert isinstance(latest.get("total"), int) and latest["total"] >= 1, latest
post = items[0]
for field in ("id", "title", "content", "createdAt"):
    assert post.get(field), (field, post)

frontend_status, frontend_html = request(f"http://localhost:{frontend_port}/")
assert frontend_status == 200, (frontend_status, frontend_html)
rendered = html.unescape(frontend_html)
post_link = f"/posts/{post['id']}"
assert post["title"] in rendered or post_link in rendered, {
    "title": post["title"],
    "post_link": post_link,
}

print(f"health={health}")
print(f"latest_post_id={post['id']}")
print(f"latest_post_title={post['title']}")
print(f"frontend_ssr_title={post['title'] in rendered}")
print(f"frontend_ssr_post_link={post_link in rendered}")
PY

seed_count="$(docker compose exec -T db psql -U community -d community -Atc \
  "SELECT COUNT(*) FROM seed_history WHERE seed_key='assignment-v1';")"
test "$seed_count" = "1"

docker compose exec -T frontend sh -c \
  "grep -R -F -q 'http://localhost:${BACKEND_PORT}/api' /app/.next"

echo "seed_history_assignment_v1=${seed_count}"
echo "browser_api_base_url=http://localhost:${BACKEND_PORT}/api"
