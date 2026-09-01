#!/usr/bin/env bash

if [ "$1" = "" ]; then
  echo "You should specify the checkstyle jar (with path) as an argument."
  echo 'Example: ./checkstyle.sh ~/CodeAnalysis/checkstyle/checkstyle-6.10-all.jar'
  exit 1
fi

java -jar $1 -c rules.xml ../src/fidocadj/**/*  ../src/fidocadj/* >fidocadj_check.txt

echo "Checkstyle results stored in fidocadj_check.txt"
echo "lines on fidocadj_check.txt"

wc -l fidocadj_check.txt
