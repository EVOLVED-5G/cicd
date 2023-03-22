#!/bin/bash

echo HOLA

SERVICES=$(ls /home/labuser/cicd/cd/helm/fogus/templates/*-service.y*ml)
echo $SERVICES

# for service in $SERVICES
# do 
# echo "servicio: $service"
# done


echo "Get Service names"

#sudo snap install yq
# sudo yq .metadata.name < /home/labuser/cicd/cd/helm/fogus/templates/dbnetapp-service.yaml
# sudo yq .spec.ports.[].port < /home/labuser/cicd/cd/helm/infolysis/templates/infolysis-netapp-service.yaml

for service in $SERVICES
do 
  NAME=$(sudo yq .metadata.name < $service)
  PORTS=$(sudo yq .spec.ports.[].port < $service)
  POD=$(kubectl get pods |awk "/$NAME/{ print \$1 }")
  echo "Service $NAME with POD $POD has next ports defined: $PORTS"
  echo "Checking ports"
  kubectl exec -it $POD -- apt update > /dev/null
  kubectl exec -it $POD -- apt install -y net-tools > /dev/null
  kubectl exec -it $POD -- apt install -y nmap > /dev/null
  for port in $PORTS
  do
    STATUS=$(kubectl exec $POD -- nmap -p$port localhost|awk "/^$port/{ print \$2 }")
    # kubectl exec dbnetapp-dd9758b9b-khxsk -- nmap -p5432 localhost|awk "/^5432/{ print \$2 }"
    echo "STATUS: $STATUS"
    if [ "$STATUS" != "open" ]
    then
        exit 255
    fi
  done
done



#  2081  kubectl get namespaces
#  2082  kubectl get pods
#  2083  kubectl exec -it netappdjango-cd98566d-ggn8q -- /bin/bash
#  2084  kubectl exec --help
#  2085  kubectl exec -i netappdjango-cd98566d-ggn8q -- /bin/bash
#  2086  kubectl exec -t netappdjango-cd98566d-ggn8q -- /bin/bash
#  2087  kubectl exec -it netappdjango-cd98566d-ggn8q -- ls -la
#  2088  kubectl exec -it netappdjango-cd98566d-ggn8q -- apt install net-tools
#  2089  kubectl exec -it netappdjango-cd98566d-ggn8q -- apt install net-tools|yes
#  2090  kubectl exec -it netappdjango-cd98566d-ggn8q -- nmap
#  2091  kubectl exec -it netappdjango-cd98566d-ggn8q -- nmap localhost
#  2092  kubectl exec -it netappdjango -- nmap localhost
#  2093  kubectl exec -it netappdjango-cd98566d-ggn8q -- nmap localhost
#  2094  kubectl exec -it netappdjango -- apt install -y nano
#  2095  kubectl exec -it netappdjango-cd98566d-ggn8q -- apt install -y nano
#  2096  kubectl exec -it netappdjango-cd98566d-ggn8q -- nmap localhost
#  2097  kubectl exec -it netappdjango-cd98566d-ggn8q -- nmap localhost |grep "^[0-9]*"
#  2098  kubectl exec -it netappdjango-cd98566d-ggn8q -- nmap localhost |awk  /^[0-9]*/{ print $1 }
#  2099  kubectl exec -it netappdjango-cd98566d-ggn8q -- nmap localhost |awk '/^[0-9]*/{ print $1 }'
#  2100  kubectl exec -it netappdjango-cd98566d-ggn8q -- "nmap localhost "
#  2101  kubectl exec -it netappdjango-cd98566d-ggn8q -- namp localhost
#  2102  kubectl exec -it netappdjango-cd98566d-ggn8q -- "nmap localhost"
#  2103  kubectl exec -it netappdjango-cd98566d-ggn8q -- namp localhost
#  2104  kubectl exec -it netappdjango-cd98566d-ggn8q -- nmap localhost
#  2105  kubectl exec netappdjango-cd98566d-ggn8q -- nmap localhost
#  2106  kubectl exec netappdjango-cd98566d-ggn8q -- nmap localhost|grep "^[0-9]*"
#  2107  kubectl exec netappdjango-cd98566d-ggn8q -- nmap localhost|grep "^[0-9]*" > test.txt
#  2108  kubectl exec netappdjango-cd98566d-ggn8q -- nmap localhost|grep "^[0-9]*" > ~/test.txt
#  2110  kubectl get pods -n nef
#  2146  history |grep kubectl