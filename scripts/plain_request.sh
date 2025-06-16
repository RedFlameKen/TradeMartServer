#!/usr/bin/env bash

request="GET /videoranged HTTP/1.1\r\n\
Host: 127.0.0.1\r\n\
Content-Range: bytes 0-1023/4098\r\n\
Content-Type: text/plain\r\n\
Content-Length: 1024\r\n\
Connection: close\r\n\
\r\n\
"

printf "$request" | netcat 127.0.0.1 8080
