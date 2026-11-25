#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
javac -encoding UTF-8 -d "$work" demo/src/main/java/com/example/demo/service/UsuarioPolicy.java demo/src/main/java/com/example/demo/service/ValidacaoContaException.java scripts/PolicySmoke.java
java -Dfile.encoding=UTF-8 -cp "$work" PolicySmoke
java -Dfile.encoding=UTF-8 scripts/ParseJava.java demo/src
