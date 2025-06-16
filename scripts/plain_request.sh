#!/usr/bin/env bash

request="GET /videoranged HTTP/1.1\r\n\
Host: 127.0.0.1\r\n\
Content-Range: bytes 0-1023/4098\r\n\
Content-Type: text/plain\r\n\
Content-Length: 1024\r\n\
Connection: close\r\n\
\r\n\
"

video_data=$(cat ~/Videos/memes/fallguys_battlepass.mp4 | base64)

post_video_request="POST /post/publish/95704/media HTTP/1.1\r\n\
Host: 127.0.0.1\r\n\
Content-Type: application/json\r\n\
Connection: close\r\n\
\r\n\
{\"filename\":\"fortnite battlepass\",\"data\":\"$video_data\"}\
\r\n\
"

printf "$post_video_request" | netcat 127.0.0.1 8080
