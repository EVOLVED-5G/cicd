#############################################
# Pull minimal:7 image.
#############################################
FROM dockerhub.hi.inet/dcip/minimal:7

############################################################################
# vars
############################################################################
ENV LC_ALL=en_US.utf8
ENV TERRAFORM_VERSION=0.15.3
ENV ANSIBLE_VERSION=2.8.5
ENV DOCKER_VERSION=1.10.6
ENV AWS_CLI2_VERSION=2.1.29
ENV PACKER_VERSION=1.6.0
ENV BOTO3_VERSION=1.9.248
ENV DATACLASSES_VERSION=0.6

############################################################################
# Install the required software
############################################################################
RUN yum -y update && \
    yum -y install wget unzip epel-release jq && \
    yum clean all && rm -rf /var/cache/yum

########################################
# Install Terraform
########################################
RUN wget --quiet https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip \
    && unzip terraform_${TERRAFORM_VERSION}_linux_amd64.zip \
    && mv terraform /usr/bin \
    && rm terraform_${TERRAFORM_VERSION}_linux_amd64.zip

########################################
# Install AWS-cli v2
########################################
RUN wget --quiet https://awscli.amazonaws.com/awscli-exe-linux-x86_64-${AWS_CLI2_VERSION}.zip \
    && unzip awscli-exe-linux-x86_64-${AWS_CLI2_VERSION}.zip \
    && ./aws/install

########################################
# Install Python, Ansible, Docker
########################################
RUN yum install -y python-pip && \
    yum install -y python3 && \
    yum install -y python3-pip && \
    pip3 install --upgrade pip && \
    pip3 install dataclasses==${DATACLASSES_VERSION} && \
    pip3 install ansible==${ANSIBLE_VERSION} && \
    pip3 install docker-py==${DOCKER_VERSION} && \
    pip3 install boto3==${BOTO3_VERSION}

########################################
# Install Telnet
########################################
RUN yum install -y telnet

########################################
# Install Azure-cli
########################################
RUN rpm --import https://packages.microsoft.com/keys/microsoft.asc && \
    sh -c 'echo -e "[azure-cli]\nname=Azure CLI\nbaseurl=https://packages.microsoft.com/yumrepos/azure-cli\nenabled=1\ngpgcheck=1\ngpgkey=https://packages.microsoft.com/keys/microsoft.asc" > /etc/yum.repos.d/azure-cli.repo' && \
    yum install -y azure-cli

########################################
# Install Packer
########################################
RUN mv /usr/sbin/packer /usr/sbin/packerOld
RUN wget --quiet https://releases.hashicorp.com/packer/${PACKER_VERSION}/packer_${PACKER_VERSION}_linux_amd64.zip \
    && unzip packer_${PACKER_VERSION}_linux_amd64.zip \
    && mv packer /usr/bin \
    && rm packer_${PACKER_VERSION}_linux_amd64.zip

########################################
# Install OC client
########################################

RUN wget --no-check-certificate --quiet https://artifactory.hi.inet/artifactory/list/common/openshift/oc-3.11.439-linux.tar.gz \
    && tar xzvf oc-3.11.439-linux.tar.gz \
    && mv oc /usr/bin