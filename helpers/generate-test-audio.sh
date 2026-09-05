#!/usr/bin/env bash
# Generates the test audio files in src/test/resources/audio (see ADR 001 and ADR 003).
#
# tone-with-silences: a 440 Hz tone, 44.1 kHz, mono, 10.5 seconds, with
# silences at known positions.
#
#   0.0 -  1.0  sound
#   1.0 -  4.0  silence (3.0 s, a pause for shadowing)
#   4.0 -  5.0  sound
#   5.0 -  5.5  silence (0.5 s, a pause inside a sentence, must be ignored)
#   5.5 -  6.5  sound
#   6.5 -  9.5  silence (3.0 s, a pause for shadowing)
#   9.5 - 10.5  sound
#
# Requires ffmpeg on the PATH.
set -eu
out="$(cd "$(dirname "$0")/.." && pwd)/src/test/resources/audio"
mkdir -p "$out"
silent='between(t,1,4)+between(t,5,5.5)+between(t,6.5,9.5)'
tone='0.5*sin(440*2*PI*t)'
ffmpeg -y -loglevel error -f lavfi -i "aevalsrc='if($silent,0,$tone)':s=44100:d=10.5" \
  -ac 1 -c:a pcm_s16le "$out/tone-with-silences.wav"
ffmpeg -y -loglevel error -i "$out/tone-with-silences.wav" \
  -c:a libmp3lame -b:a 128k "$out/tone-with-silences.mp3"
echo "generated:"
ls -l "$out"
