# copy this file to .env
# cp env-file-for-local.dev .env

DOMAIN="localhost"
DOCKER_IMAGE_BACKEND="backend"
DOCKER_IMAGE_FRONTEND="frontend"
# Backend
SERVER_NAME="localhost"
SERVER_HOST="localhost"
SERVER_PORT=8888
BACKEND_CORS_ORIGINS=["https://5g-api-emulator.medianetlab.eu","http://localhost"]
PROJECT_NAME="NEF_Emulator"
SECRET_KEY="2D47CF2958CEC7CC86C988E9F9684"
FIRST_SUPERUSER="admin@my-email.com"
FIRST_SUPERUSER_PASSWORD="pass"
SMTP_TLS="True"
SMTP_PORT=465
SMTP_HOST="mail.host.com"
SMTP_USER="user"
SMTP_PASSWORD=pass
EMAILS_FROM_EMAIL="user@my-email.com"
SENTRY_DSN=""
USERS_OPEN_REGISTRATION="true"

# Postgres
# info: POSTGRES_USER value ('postgres') is hard-coded in /pgadmin/servers.json
POSTGRES_SERVER="db"
POSTGRES_USER="postgres"
POSTGRES_PASSWORD="pass"
POSTGRES_DB="app"

# PgAdmin
PGADMIN_LISTEN_PORT=5050
PGADMIN_DEFAULT_EMAIL="admin@my-email.com"
PGADMIN_DEFAULT_PASSWORD="pass"

# Mongo 
MONGO_USER="root"
MONGO_PASSWORD="pass"