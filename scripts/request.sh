#!/bin/bash

method="$1"
path="$2"
content_file="$3"
host="$4"
port="$5"

if [ -z $method ] || [ -z $path ]; then
    printf "Please provide at least the method and the path\n"
    exit
fi

if [ -z $content_file ]; then
    content_file="docs/testrequest1.json"
fi

if [ -z $host ]; then
    host=127.0.0.1
fi

if [ -z $port ]; then
    port=8080
fi

content=$(cat $content_file)
content_type="application/json"
content_length=$(wc -c < $content_file)

request="$method $path HTTP/1.1\r\n\
Host: $host\r\n\
Content-Type: $content_type\r\n\
Content-Length: $content_length\r\n\
Connection: close\r\n\
\r\n\
$content\r\n
"

printf "$request" | netcat $host $port
