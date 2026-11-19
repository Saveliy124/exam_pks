#!/bin/sh
# Source this file from a launcher; do not change the user's global Java settings.
if [ "$(uname -s)" = Darwin ]; then
  EXAM_JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
else
  EXAM_JAVA_HOME=${JAVA_HOME:-}
fi
if [ -n "$EXAM_JAVA_HOME" ]; then export JAVA_HOME="$EXAM_JAVA_HOME"; export PATH="$JAVA_HOME/bin:$PATH"; fi
if ! java -version 2>&1 | head -n 1 | grep -Eq 'version "21[.]'; then
  echo 'Для КР2 требуется JDK 21. Установите его и задайте JAVA_HOME.' >&2; exit 1
fi
command -v mvn >/dev/null 2>&1 || { echo 'Установите Maven 3.9 или новее.' >&2; exit 1; }
