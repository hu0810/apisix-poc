#!/bin/bash
# export all settings using APISIX Admin API

APISIX_ADMIN_URL=${APISIX_ADMIN_URL:-http://localhost:9180/apisix/admin}
APISIX_ADMIN_API_KEY=${APISIX_ADMIN_API_KEY:-admin123}

for item in routes services upstreams consumers ssl global_rules; do
  curl -s "$APISIX_ADMIN_URL/$item" -H "X-API-KEY: $APISIX_ADMIN_API_KEY" > ./config/raw/${item}.json
done

#when etcd_data cannot be loaded
sudo rm -rf ./etcd_data
mkdir ./etcd_data
sudo chown -R $USER:$USER ./etcd_data
chmod -R 777 ./etcd_data