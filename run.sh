#!/usr/bin/env bash
# Linux / macOS: ./run.sh   (needs Java 17+ and Maven)
cd "$(dirname "$0")"
command -v java >/dev/null || { echo "Install Java 17+ first (https://adoptium.net)"; exit 1; }
command -v mvn  >/dev/null || { echo "Install Maven first (https://maven.apache.org)"; exit 1; }
exec mvn spring-boot:run "$@"
