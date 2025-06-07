#!/usr/bin/env bash

# Test various subscription requests against the APISIX Route Subscription Service.
# The service is expected to run on localhost:8080 by default.
# Override SERVICE_URL to point to a different base URL.

SERVICE_URL=${SERVICE_URL:-http://localhost:8080}

print_case() {
  local title=$1
  local data=$2

  echo -e "\n---- $title ----"
  # -s suppress progress, -D - prints headers to stdout
  curl -s -D - -o /tmp/resp.$$ -H "Content-Type: application/json" \
       -d "$data" "$SERVICE_URL/apisix/subscribe"
  local status=$(grep -m1 HTTP /tmp/resp.$$ | awk '{print $2}')
  cat /tmp/resp.$$
  echo "HTTP_STATUS:$status"
  rm /tmp/resp.$$
}

print_case "Valid subscription (persona-basic)" '{
  "userName": "alice",
  "personaType": "tenant",
  "apiKey": "alice-key",
  "apis": ["getUserInfo"],
  "extraParams": {}
}'

print_case "Unknown API" '{
  "userName": "alice",
  "personaType": "tenant",
  "apiKey": "alice-key",
  "apis": ["doesNotExist"]
}'

print_case "Missing personaType" '{
  "userName": "bob",
  "apiKey": "bob-key",
  "apis": ["getUserInfo"]
}'

print_case "Invalid limit-count parameters" '{
  "userName": "carol",
  "personaType": "provider",
  "apiKey": "carol-key",
  "apis": ["limitCountTest"],
  "extraParams": { "count": 10 }
}'

print_case "Valid limit-count parameters" '{
  "userName": "carol",
  "personaType": "provider",
  "apiKey": "carol-key",
  "apis": ["limitCountTest"],
  "extraParams": { "count": 10, "time_window": 60 }
}'

print_case "Multiple upstream router" '{
  "userName": "dave",
  "personaType": "tenant",
  "apiKey": "dave-key",
  "apis": ["modelInference"]
}'
