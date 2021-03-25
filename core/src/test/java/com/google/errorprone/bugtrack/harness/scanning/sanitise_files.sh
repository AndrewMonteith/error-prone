#!/bin/bash
java -jar "../google-format.jar" -i --skip-sorting-imports --skip-removing-unused-imports "$@"