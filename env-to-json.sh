#!/usr/bin/env bash

# Bash script to quickly convert .env file into one JSON object.
#
# Used to create the RENDER_ENV_VARS_JSON secret on GitHub to be used in the "Build, Push, Deploy" workflow.
# 
# File name can be passed as parameter. By default, uses .env in current directory. Result is printed out.

set -euo pipefail

ENV_FILE="${1:-.env}"

awk '
  /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }   # skip comments/blank lines
  {
    line=$0
    sub(/^[[:space:]]*export[[:space:]]+/, "", line)
    pos=index(line, "=")
    if (pos == 0) next

    key=substr(line, 1, pos-1)
    val=substr(line, pos+1)

    gsub(/^[[:space:]]+|[[:space:]]+$/, "", key)

    # remove surrounding quotes if present
    if (val ~ /^".*"$/ || val ~ /^'\''.*'\''$/) {
      val=substr(val, 2, length(val)-2)
    }

    # unit separator between key/value
    printf "%s\034%s\n", key, val
  }
' "$ENV_FILE" | jq -Rn '
  reduce inputs as $line ({}; 
    ($line | split("\u001c")) as $kv
    | . + { ($kv[0]): $kv[1] }
  )
'