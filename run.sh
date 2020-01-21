#! /usr/bin/env bash

javac -d out/ src/site/jiyang/*
echo "========================================================================="
java -cp out/ site.jiyang.Main out/site/jiyang/Main.class