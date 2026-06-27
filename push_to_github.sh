#!/usr/bin/env bash

OWNER="TermuxSupport"
REPO="termuxmod-app"
BRANCH="main"
API="https://api.github.com/repos/${OWNER}/${REPO}/contents"
TOKEN="${GITHUB_TOKEN}"

SUCCESS=0
FAIL=0
SKIP=0
LOG=/tmp/push_log.txt
> "$LOG"

push_file() {
  local filepath="$1"
  local remote_path="${filepath#./}"

  local content
  content=$(base64 -w 0 "$filepath" 2>/dev/null)
  if [ $? -ne 0 ] || [ -z "$content" ]; then
    echo "SKIP $remote_path" >> "$LOG"
    return 0
  fi

  local payload
  payload=$(printf '{"message":"Add %s","branch":"%s","content":"%s"}' \
    "$remote_path" "$BRANCH" "$content")

  local http_code
  http_code=$(curl -s -o /tmp/gh_resp_$$.json -w "%{http_code}" \
    -X PUT \
    -H "Authorization: token ${TOKEN}" \
    -H "Accept: application/vnd.github.v3+json" \
    -H "Content-Type: application/json" \
    "${API}/${remote_path}" \
    -d "$payload" 2>/dev/null)

  if [[ "$http_code" == "201" || "$http_code" == "200" ]]; then
    echo "OK $remote_path" >> "$LOG"
  else
    local errmsg
    errmsg=$(grep -o '"message":"[^"]*"' /tmp/gh_resp_$$.json 2>/dev/null | head -1)
    echo "FAIL[$http_code] $remote_path $errmsg" >> "$LOG"
  fi
  rm -f /tmp/gh_resp_$$.json
}

export -f push_file
export OWNER REPO BRANCH API TOKEN LOG

echo "=== Push ke github.com/${OWNER}/${REPO} ==="

mapfile -t FILES < <(find . -type f \
  ! -path './.git/*' \
  ! -path './.local/*' \
  ! -path './app/src/main/cpp/bootstrap-*.zip' \
  ! -path './push_to_github.sh' \
  ! -name '*.class' \
  ! -name '*.o' \
  ! -name '*.so' \
  | sort)

TOTAL=${#FILES[@]}
echo "Total file: $TOTAL — push paralel 8 thread..."
echo ""

printf '%s\n' "${FILES[@]}" | xargs -P 8 -I{} bash -c 'push_file "$@"' _ {}

SUCCESS=$(grep -c "^OK " "$LOG" 2>/dev/null || true)
FAIL=$(grep -c "^FAIL" "$LOG" 2>/dev/null || true)
SKIP=$(grep -c "^SKIP" "$LOG" 2>/dev/null || true)

echo ""
echo "=== HASIL ==="
echo "Sukses : $SUCCESS / $TOTAL"
echo "Gagal  : $FAIL"
echo "Skip   : $SKIP"

if [ "$FAIL" -gt 0 ]; then
  echo ""
  echo "File gagal:"
  grep "^FAIL" "$LOG" | head -20
fi

echo ""
echo "Repo  : https://github.com/${OWNER}/${REPO}"
echo "Actions: https://github.com/${OWNER}/${REPO}/actions"
