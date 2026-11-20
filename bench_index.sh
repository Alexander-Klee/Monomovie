#!/usr/bin/env bash

set -u

url='http://localhost:8080'
runs="${1:-100}"
times=()

for ((i = 1; i <= runs; i++)); do
    printf 'Run %d/%d: ' "$i" "$runs"

    if elapsed=$(
        curl --request GET -sS -L \
             --output /dev/null \
             --write-out '%{time_total}' \
             --url "$url"
    ); then
        printf '%ss\n' "$elapsed"
        times+=("$elapsed")
    else
        echo "failed"
    fi
done

((${#times[@]} > 0)) || exit 1

printf '%s\n' "${times[@]}" | python3 -c '
import sys
import statistics

values = [float(line) for line in sys.stdin if line.strip()]

print("\nStatistics")
print(f"count:    {len(values)}")
print(f"min:      {min(values):.6f}s")
print(f"max:      {max(values):.6f}s")
print(f"average:  {statistics.mean(values):.6f}s")
print(f"median:   {statistics.median(values):.6f}s")
print(f"stdev:    {statistics.stdev(values):.6f}s" if len(values) > 1 else "stdev:    n/a")
'