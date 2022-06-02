variable "DOMAIN" {
  description = "Dominio"
  type        = string
  default     = "localhost"
}

variable "DOCKER_IMAGE_BACKEND" {
  description = "backend "
  type        = string
  default     = "backend"
}

variable "DOCKER_IMAGE_FRONTEND" {
  description = "DOCKER_IMAGE_FRONTEND"
  type        = string
  default     = "frontend"
}

##BACKEND
variable "SERVER_NAME" {
  description = "Actions for trust identity"
  type        = string
  default     = "localhost"
}

variable "SERVER_HOST" {
  description = "Principal type service for trust identity"
  type        = string
  default     = "localhost"
}

variable "SERVER_PORT" {
  description = "Principal service identifier for trust identity"
  type        = string
  default     = 8888
}

variable "BACKEND_CORS_ORIGINS" {
  description = " backend cors"
  type        = list(string)
  default     = ["https://5g-api-emulator.medianetlab.eu", "http://localhost"]
}

variable "PROJECT_NAME" {
  description = "Geprom EC2 service policy"
  type        = string
  default     = "NEF_Emulator"
}

variable "SECRET_KEY" {
  description = "Secret key"
  type        = string
  default     = "2D47CF2958CEC7CC86C988E9F9684"
}

variable "FIRST_SUPERUSER" {
  description = "first super user"
  type        = string
  default     = "admin@my-email.com"
}

variable "FIRST_SUPERUSER_PASSWORD" {
  description = "password"
  type        = string
  default     = "pass"
}

variable "SMTP_TLS" {
  description = "TLS ?"
  type        = string
  default     = "True"
}

variable "SMTP_PORT" {
  description = "Secret key"
  type        = string
  default     = 465
}

variable "SMTP_HOST" {
  description = "first super user"
  type        = string
  default     = "mail.host.com"
}

variable "SMTP_USER" {
  description = "stmp user"
  type        = string
  default     = "user"
}

variable "SMTP_PASSWORD" {
  description = "Principal service identifier for trust identity"
  type        = string
  default     = "pass"
}

variable "EMAILS_FROM_EMAIL" {
  description = "first super user"
  type        = string
  default     = "user@my-email.com"
}

variable "SENTRY_DSN" {
  description = "password"
  type        = string
  default     = ""
}

variable "USERS_OPEN_REGISTRATION" {
  description = "Principal service identifier for trust identity"
  type        = string
  default     = "true"
}

# Postgres

variable "POSTGRES_SERVER" {
  description = "db service "
  type        = string
  default     = "db"
}

variable "POSTGRES_USER" {
  description = "first super user"
  type        = string
  default     = "postgres"
}

variable "POSTGRES_PASSWORD" {
  description = "password"
  type        = string
  default     = "pass"
}

variable "POSTGRES_DB" {
  description = " Postgres DB Identifier"
  type        = string
  default     = "app"
}


# PgAdmin
variable "PGADMIN_LISTEN_PORT" {
  description = "PGADMIN Port"
  type        = string
  default     = 5050
}

variable "PGADMIN_DEFAULT_EMAIL" {
  description = " email for the pgadmin user"
  type        = string
  default     = "admin@my-email.com"
}

variable "PGADMIN_DEFAULT_PASSWORD" {
  description = " Postgres DB default password"
  type        = string
  default     = "app"
}


# Mongo 
variable "MONGO_USER" {
  description = " default user for mongo"
  type        = string
  default     = "root"
}

variable "MONGO_PASSWORD" {
  description = " default password"
  type        = string
  default     = "pass"
}
