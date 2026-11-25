#!/usr/bin/env bash
# Runs the MySQL-independent logic checks (hashing, cart, GST, receipt).
cd "$(dirname "$0")/.."
rm -rf /tmp/emall_chk && mkdir /tmp/emall_chk
javac -d /tmp/emall_chk $(find src -name '*.java') && java -cp /tmp/emall_chk com.emall.SelfCheck
