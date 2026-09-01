#!/usr/bin/env bash

if [ "$1" = "" ]; then
  echo "You should specify the command to run pmd as the first argument."
  # PMD 5.0
  #  echo 'Example: ./pmd.sh ~/pmd-bin-5.0.5/bin/run.sh'
  # PMD 7.0
  echo 'Example: ./pmd.sh ~/Software/pmd-bin-7.0.0-rc2/bin/pmd'
  exit 1
fi 

# PMD 5.0
#$@ pmd -f html -d ../src/ -rulesets java-basic,java-design -encoding UTF-8 >fidocadj_pmd.html

# PMD 7.0
# $@ check -f html --minimum-priority low -d ../src -R rulesets/java/quickstart.xml -R rulesets/java/design.xml -r fidocadj_pmd.html

$@ check -f html --minimum-priority low -d ../src -R rulesets/java/quickstart.xml -r fidocadj_pmd.html


echo "PMD results stored in fidocadj_pmd.html"

#$@ cpd --minimum-tokens 100 --files ../src/ >fidocadj_cpd.txt
#echo "CPD results stored in fidocadj_cpd.txt"
