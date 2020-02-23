#! /usr/bin/env bash

javac -verbose -d out src/site/jiyang/*
echo "========================= ^_^ Start running ================================================"
java -verbose -cp out/ site.jiyang.Main out/site/jiyang/Main.class