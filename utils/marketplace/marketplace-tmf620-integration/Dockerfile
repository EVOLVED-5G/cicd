FROM alpine:3.12.0

# Create app directory
WORKDIR /usr/src/app

# Install app dependencies
# A wildcard is used to ensure both package.json AND package-lock.json are copied
# where available (npm@5+)
COPY package*.json ./
RUN apk add --update nodejs npm

RUN npm install
# If you are building your code for production
# RUN npm ci --only=production

# Bundle app source
COPY . .

ENV MONGO_URL=mongodb://evolved5g-pilot-tmf-mongo-container:27017/

EXPOSE 8080
CMD [ "node", "index.js" ]
