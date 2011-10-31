#!/bin/bash
while read line;do
  echo Deleting $line
  curl -X DELETE $line
done
