# TradeMartServer

This will serve as the backend server for our SOFTENG2 project.


## Building
to build the project, just use the wrapper gradle file:

on Linux:
```
./gradlew build
```

on Windows:
```
gradle.bat build
```

## Running 
The server will look try to connect to a mysql database as soon as it starts so
make sure that a mysql server is running on your machine before you run the
server.

The server also makes use of [FFmpeg](https://ffmpeg.org/) to encode videos and
creating thumbnails. You should install FFmpeg on your machine for these
features to work. While the server will still start without FFmpeg, crashes
when using media features should be expected.

The server also looks for a `.dbconfig.json` configuration file for the
database connection and a `.media_conf.json` configuration file for the media
path configuration. allowing for flexibility with different machines and
setups. The configuration file is ignored in git for security reasons, so make
sure to make it yourself. These files should be made under the `app` directory.

Here is an example `.dbconfig.json` file:
```json
{
    "db_name": "trademart",
    "username": "root",
    "password": "root",
    "address": "localhost",
    "port": 3306
}
```

Here is an example `.media_conf.json` file:
```json
{
    "storage_path": "/home/username/Storage/media"
}
```

Here is an example `.media_conf.json` file for **Windows**:
```json
{
    "storage_path": "C:\\Users\\username\\Documents\\media"
}
```

Since this project uses gradle, It would be much easier to run the project
through the gradle wrapper script.

on Linux:
```
./gradlew bootRun
```

on Windows:
```
gradle.bat bootRun
```

## Requests
I have provided a script for making test http requests to the server. it can be
executed using the following command (if you're on windows, I advise you use
git bash):
```bash
scripts/request.sh [METHOD] [PATH] [CONTENT_FILE] [ADDRESS] [PORT]
```

METHOD could be one of the following:
- GET
- POST
- PUT
- DELETE

PATH will depend on the mappings found in the code (e.g. @GetMapping(),
@PostMapping()). 

CONTENT_FILE is supposedly one of the json files in docs/

> [!WARNING]
> the `docs` directory was added to `.gitignore`. Create the docs directory and
> please your test jsons in there if needed.

ADDRESS is the address of the server. User of the script must provide one if
the address of the spring server is not localhost. By default, this is
127.0.0.1 so it can be left empty if the server uses localhost.

PORT is the port that the server uses. By default this is 8080 so this can be
left empty.
