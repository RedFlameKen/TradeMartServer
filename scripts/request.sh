#!/bin/bash

method="$1"
path="$2"

content=$(cat docs/testrequest1.json)
content_type="application/json"
content_length=$(wc -c < docs/testrequest1.json)

request="$method $path HTTP/1.1\r\n\
Host: localhost\r\n\
Content-Type: $content_type\r\n\
Content-Length: $content_length\r\n\
Connection: close\r\n\
\r\n\
$content\r\n
"

printf "$request" | netcat 127.0.0.1 8080
