#!/bin/sh
# Double-click on macOS, or run ./launch.command from a terminal.
set -eu
cd "$(dirname "$0")"
if [ "$(uname -s)" = "Darwin" ]; then
  JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
  export JAVA_HOME
  PATH="$JAVA_HOME/bin:$PATH"
  export PATH
fi
exec mvn compile exec:java "$@"
