# uploader001

Java 17 / Javalin 製のシンプルなファイルアップロードマイクロサービスです。  
n8n などのワークフローツールから画像をアップロードし、URLで取得・配信するために作りました。

## 概要

- バイナリ（画像）を POST するとMD5ハッシュ名で保存し、URLを返す
- 保存したファイルを GET で画像として配信する
- SSL はリバースプロキシ（Nginx 等）側で処理する想定
- 認証もリバースプロキシ側の Basic 認証で対応

## 要件

- Java 17 以上
- Maven 3.x（ビルド時のみ）
- 設定ファイルで指定したディレクトリへの書き込み権限

## 設定

起動ディレクトリに `uploader001.properties` を作成します。

```bash
cp uploader001.properties.example uploader001.properties
```

```properties
port=18999
upload.dir=/var/uploads/
base.url=https://your-domain.com/your-path/
```

| キー | 説明 |
|---|---|
| port | リッスンするポート番号 |
| upload.dir | ファイルの保存先ディレクトリ |
| base.url | レスポンスで返すURLのベース（末尾 `/` 必須） |

`uploader001.properties` は `.gitignore` に含まれています。リポジトリにはコミットしないでください。

## ビルド

```bash
mvn package
```

`target/uploader001-1.0.0.jar` が生成されます（依存関係込みのfat jar）。

## 起動

```bash
java -jar target/uploader001-1.0.0.jar
```

設定ファイルはカレントディレクトリから読み込みます。

## API

### `GET /health`

死活確認。

```
200 OK
OK
```

---

### `POST /upload`

バイナリファイルをアップロードします。  
`Content-Type` から拡張子を判定し、MD5ハッシュ名で保存します。

**リクエスト**

```
POST /upload
Content-Type: image/png
Body: <バイナリ>
```

**レスポンス**

```
200 OK
https://your-domain.com/your-path/d41d8cd98f00b204e9800998ecf8427e.png
```

対応している Content-Type:

| Content-Type | 拡張子 |
|---|---|
| image/jpeg | .jpg |
| image/png | .png |
| image/gif | .gif |
| image/webp | .webp |
| image/bmp | .bmp |
| image/svg+xml | .svg |

---

### `GET /files/{filename}`

保存済みのファイルを配信します。

```
GET /files/d41d8cd98f00b204e9800998ecf8427e.png
```

```
200 OK
Content-Type: image/png
Body: <バイナリ>
```

ファイルが存在しない場合は `404`、ファイル名が不正な場合は `400` を返します。

---

### `GET /list`

保存済みの全ファイルのURLを1行ずつ返します。

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

サーバーを停止します。

```
200 OK
Stopping
```

## デプロイ

### 1. ディレクトリ・設定ファイルの準備

```bash
sudo mkdir -p /var/uploads
sudo chown youruser:youruser /var/uploads

cp uploader001.properties.example uploader001.properties
# uploader001.properties を編集して設定値を入力
```

### 2. jar を配置

```bash
scp target/uploader001-1.0.0.jar yourserver:/opt/uploader001/
scp uploader001.properties yourserver:/opt/uploader001/
```

### 3. 起動

```bash
cd /opt/uploader001
java -jar uploader001-1.0.0.jar
```

### systemd で自動起動する場合

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

## Nginx リバースプロキシ設定例

```nginx
location /your-path/ {
    auth_basic "Restricted";
    auth_basic_user_file /etc/nginx/.htpasswd;
    proxy_pass http://127.0.0.1:18999/;
    client_max_body_size 50M;
}
```

Basic 認証ユーザーの作成:

```bash
sudo apt install apache2-utils
sudo htpasswd -c /etc/nginx/.htpasswd youruser
```
