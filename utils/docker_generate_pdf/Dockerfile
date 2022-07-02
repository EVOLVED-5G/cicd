from node:18

ARG PATH
ENV OPENSSL_CONF=/etc/ssl/
RUN npm install -g markdown-pdf
RUN apt update
RUN apt-get install poppler-utils -y
COPY $PATH /usr/src/app/
CMD ["/bin/bash"]