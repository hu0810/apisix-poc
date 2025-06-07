#!/bin/bash
# Reset etcd and MariaDB data and restart docker-compose stack

set -euo pipefail

# Stop containers and remove volumes
# -v ensures anonymous and named volumes are removed (MariaDB data)
docker-compose down -v

# Clear etcd data (host directory mounted into etcd container)
sudo rm -rf ./etcd_data
mkdir ./etcd_data
sudo chown -R $USER:$USER ./etcd_data
chmod -R 777 ./etcd_data

# Start stack again
docker-compose up -d
