#!/usr/bin/env bash

SCRIPT_DIR=$(dirname $(readlink -f $BASH_SOURCE[0]))

year=${1:-2006}

outputFolder=${2:-$year-img-src}


spark-submit \
    --master yarn-client \
    --queue "p003-batch" \
    "$SCRIPT_DIR/target/spark_link_extractor-1.0-SNAPSHOT-hadoop.jar" \
    "/projects/p003/webpages-$year-Extracts-strings/" \
    "$outputFolder"
