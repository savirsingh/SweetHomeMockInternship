#!/bin/sh
# Run from any directory; requires the same JDK/Maven as the assignment.
set -eu
cd "$(dirname "$0")/.."
mvn -q test
mkdir -p target/verification
javac -source 8 -target 8 -cp 'target/classes:SweetHome3D/lib/*' \
  -d target/verification verification/FeatureRegression.java
java -Djava.awt.headless=true \
  -cp 'target/verification:target/classes:SweetHome3D/lib/*' FeatureRegression
python3 test_submission.py
