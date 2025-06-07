#!/usr/bin/env bash

# Simple test script for the APISIX Route Subscription Service.
# Uses curl to hit the service with various payloads.
# SERVICE_URL can be overridden via environment variable.

SERVICE_URL=${SERVICE_URL:-http://localhost:8080}

print_header() {
  echo -e "\n---- $1 ----"
}

# 1. Valid subscription
print_header "Valid subscription (persona-basic)"
curl -i -X POST "$SERVICE_URL/apisix/subscribe" \
    -H "Content-Type: application/json" \
    -d '{
      "userName": "alice",
      "personaType": "tenant",
      "apiKey": "alice-key",
      "apis": ["getUserInfo"],
      "extraParams": {}
    }'
echo

# 2. Unknown API should fail
print_header "Unknown API"
curl -i -X POST "$SERVICE_URL/apisix/subscribe" \
    -H "Content-Type: application/json" \
    -d '{
      "userName": "alice",
      "personaType": "tenant",
      "apiKey": "alice-key",
      "apis": ["doesNotExist"]
    }'
echo

# 3. Missing required field
print_header "Missing personaType"
curl -i -X POST "$SERVICE_URL/apisix/subscribe" \
    -H "Content-Type: application/json" \
    -d '{
      "userName": "bob",
      "apiKey": "bob-key",
      "apis": ["getUserInfo"]
    }'
echo

# 4. Invalid limit-count parameters
print_header "Invalid limit-count parameters"
curl -i -X POST "$SERVICE_URL/apisix/subscribe" \
    -H "Content-Type: application/json" \
    -d '{
      "userName": "carol",
      "personaType": "provider",
      "apiKey": "carol-key",
      "apis": ["limitCountTest"],
      "extraParams": { "count": 10 }
    }'
echo

# 5. Valid limit-count parameters
print_header "Valid limit-count parameters"
curl -i -X POST "$SERVICE_URL/apisix/subscribe" \
    -H "Content-Type: application/json" \
    -d '{
      "userName": "carol",
      "personaType": "provider",
      "apiKey": "carol-key",
      "apis": ["limitCountTest"],
      "extraParams": { "count": 10, "time_window": 60 }
    }'
echo

# 6. Multiple upstream router
print_header "Multiple upstream router"
curl -i -X POST "$SERVICE_URL/apisix/subscribe" \
    -H "Content-Type: application/json" \
    -d '{
      "userName": "dave",
      "personaType": "tenant",
      "apiKey": "dave-key",
      "apis": ["modelInference"]
    }'
echo
