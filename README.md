# uploader001

A simple file upload microservice built with Java 17 and Javalin.  
Designed for use with workflow tools like n8n to upload images and serve them via URL.

## Overview

- POST binary (image) data → saved with MD5 hash filename → returns URL
- GET saved files served as images
- SSL is handled by a reverse proxy (e.g. Nginx)
- Authentication is handled by Basic Auth on the reverse proxy

## Requirements

- Java 17+
- Maven 3.x (build only)
- Write permission to the directory specified in the config file

## Configuration

Create `uploader001.properties` in the working directory:

```bash
cp uploader001.properties.example uploader001.properties
```

```properties
port=18999
upload.dir=/var/uploads/
base.url=https://your-domain.com/your-path/
```

| Key | Description |
|---|---|
| port | Port number to listen on |
| upload.dir | Directory to store uploaded files |
| base.url | Base URL prefix for returned file URLs (must end with `/`) |

`uploader001.properties` is listed in `.gitignore`. Do not commit it to the repository.

## Build

```bash
mvn package
```

Produces `target/uploader001-1.0.0.jar` (fat jar with all dependencies included).

## Run

```bash
java -jar target/uploader001-1.0.0.jar
```

The properties file is loaded from the current working directory.

## API

### `GET /health`

Health check.

```
200 OK
OK
```

---

### `POST /upload`

Upload a binary file.  
The file extension is determined from the `Content-Type` header. The file is saved using its MD5 hash as the filename.

**Request**

```
POST /upload
Content-Type: image/png
Body: <binary>
```

**Response**

```
200 OK
https://your-domain.com/your-path/d41d8cd98f00b204e9800998ecf8427e.png
```

Supported Content-Types:

| Content-Type | Extension |
|---|---|
| image/jpeg | .jpg |
| image/png | .png |
| image/gif | .gif |
| image/webp | .webp |
| image/bmp | .bmp |
| image/svg+xml | .svg |

---

### `GET /files/{filename}`

Serve a saved file.

```
GET /files/d41d8cd98f00b204e9800998ecf8427e.png
```

```
200 OK
Content-Type: image/png
Body: <binary>
```

Returns `404` if the file does not exist, `400` if the filename is invalid.

---

### `GET /list`

Returns URLs of all saved files, one per line.

```
GET /list
```

```
200 OK
https://your-domain.com/your-path/abc123.png
https://your-domain.com/your-path/def456.jpg
```

---

### `GET /stop`

Gracefully stops the server.

```
200 OK
Stopping
```

## Deployment

### 1. Prepare directory and config

```bash
sudo mkdir -p /var/uploads
sudo chown youruser:youruser /var/uploads

cp uploader001.properties.example uploader001.properties
# Edit uploader001.properties with your settings
```

### 2. Copy files to server

```bash
scp target/uploader001-1.0.0.jar yourserver:/opt/uploader001/
scp uploader001.properties yourserver:/opt/uploader001/
```

### 3. Start

```bash
cd /opt/uploader001
java -jar uploader001-1.0.0.jar
```

### systemd (optional)

`/etc/systemd/system/uploader001.service`:

```ini
[Unit]
Description=uploader001
After=network.target

[Service]
WorkingDirectory=/opt/uploader001
ExecStart=/usr/bin/java -jar /opt/uploader001/uploader001-1.0.0.jar
Restart=always
User=youruser

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable uploader001
sudo systemctl start uploader001
```

## Nginx Reverse Proxy Example

```nginx
location /your-path/ {
    auth_basic "Restricted";
    auth_basic_user_file /etc/nginx/.htpasswd;
    proxy_pass http://127.0.0.1:18999/;
    client_max_body_size 50M;
}
```

Creating a Basic Auth user:

```bash
sudo apt install apache2-utils
sudo htpasswd -c /etc/nginx/.htpasswd youruser
```
