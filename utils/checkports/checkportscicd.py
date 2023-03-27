import socket, os, shutil, re, git, nmap3, glob, sys, docker
import yaml
from yaml.loader import SafeLoader
import json

report=dict()


def isOpen(host,port,mapped_ports=None):
    #Netstat
    tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    #Nmap
    nmap = nmap3.NmapScanTechniques()
    port_to_check = port

    if mapped_ports != None:
        port_to_check = mapped_ports[port]


    try:
        print(port_to_check)
        #Checking ports with Netstat
        tcp.connect((host, int(port_to_check)))
        tcp.shutdown(2)
        #Checking ports with Nmap
        resultstcp = nmap.nmap_tcp_scan(host, args="-p " + str(port_to_check))
        print(json.dumps(resultstcp,indent=2))
        if resultstcp['127.0.0.1']['ports'][0]['state'] != 'open':
            raise Exception('Port ' + port_to_check + ' not open')
        
        if mapped_ports != None:
            print (f"NetApp TCP Port {int(port_to_check)} (Original Port {int(port)}), is LISTENING/OPENED")
        else:
            print (f"NetApp TCP Port {int(port_to_check)}, is LISTENING/OPENED")
        return True

    except Exception as e:
        print(e)
        if mapped_ports != None:
            print(f"NetApp TCP Port {int(port_to_check)} (Original Port {int(port)}), is NOT LISTENING/NOT OPENED.")
        else:
            print(f"NetApp TCP Port {int(port_to_check)}, is NOT LISTENING/NOT OPENED.")
        return False


def find_file(name, path):
    for root, dirs, files in os.walk(path):
        if name in files:
            return os.path.join(root, name)


if __name__ == '__main__':

    # total arguments
    n = len(sys.argv)
    
    if n < 3 or n > 4:
            print("expected: " + sys.argv[0] + " <netapp_branch> <netapp_repository> [<netapp_image_name>]")
            exit(255)
    
    netapp_branch = sys.argv[1]
    git_repository = sys.argv[2]
    netapp_host = 'localhost'

    print("Netapp branch: " + netapp_branch)
    print("Netapp repository: " + git_repository)
    print("Netapp Host: " + netapp_host )
    
    netapp_image_name=''
    if n==4:
        netapp_image_name=sys.argv[3]
        print("Netapp Image Name: " + netapp_image_name )

    NetApp_ports=dict()

    #Create repository folder (then it'll be removed)
    repo_dir = os.path.abspath(os.getcwd()) + "/ports_tmp/"
    
    if (os.path.isdir(repo_dir)):
      shutil.rmtree(repo_dir)

    try:
        git.Repo.clone_from(git_repository, repo_dir, branch=netapp_branch)
        repo_docker_compose_file = glob.glob(repo_dir + 'docker-compose.y*ml')
        repo_docker_file = glob.glob(repo_dir + 'Dockerfile')
        success = True
        
        # If there is no docker-compose yaml then, the python stops.
        if (bool(repo_docker_compose_file)):
            
            repo_dir_docker = find_file(os.path.basename(repo_docker_compose_file[0]), repo_dir)
            with open(repo_dir_docker) as f:
                data = yaml.load(f, Loader=SafeLoader)
                for service_name in data['services']:
                    service=data['services'][service_name]
                    # print(service)
                    ports = service.get('ports',None)
                    if ports is not None:
                        NetApp_ports[service_name]=dict()
                        NetApp_ports[service_name]['ports']=list()
                        for port_docker_compose in ports:
                            port=port_docker_compose.split(':')[0]
                            result=isOpen(netapp_host, port)
                            if not result:
                                success=False
                            NetApp_ports[service_name]['ports'].append({"port":port,"listening":result})

        elif (bool(repo_docker_file)):
            repo_dir_docker = find_file(os.path.basename(repo_docker_file[0]), repo_dir)
            with open(repo_dir_docker) as input:
                expose_list=list()
                for line in input:
                    expose_list += re.findall(r'EXPOSE \d+ ?\d*', line)
                print(expose_list)
                NetApp_ports[netapp_image_name]=dict()
                NetApp_ports[netapp_image_name]['ports']=list()
                for port in expose_list:
                    NetApp_ports[netapp_image_name]['ports'].append(port.split(" ")[1])

            # Get docker ports information
            client = docker.from_env()
            containers_list = client.containers.list(filters={"ancestor":netapp_image_name})
            container = dict()

            if len(containers_list) == 1:
                container = containers_list[0]
                print("Container " + container.attrs['Config']['Image'] + " found")
            else:
                raise Exception("Netapp " + netapp_image_name + " container not found")

            container_ports_info = container.ports
            container_ports_list = list(container_ports_info.keys())
            print(json.dumps(NetApp_ports, indent=2))

            if len(NetApp_ports[netapp_image_name]['ports']) != len(container_ports_list):
                raise Exception("Netapp ports on Dockerfile not match ports exposed on running image")
            
            mapped_ports = dict()
            for dockerfile_port in NetApp_ports[netapp_image_name]['ports']:
                for container_port in container_ports_list:
                    key_port_without_protocol = container_port.split("/")[0]
                    if key_port_without_protocol == dockerfile_port:
                        mapped_ports[dockerfile_port] = container_ports_info[container_port][0]['HostPort']
            
            print("Mapped ports:")
            print(mapped_ports)
            if len(list(mapped_ports.keys())) != len(NetApp_ports[netapp_image_name]['ports']):
                raise Exception("Netapp ports on Dockerfile not match ports map with running image")

            ports_checked=list()
            for port in NetApp_ports[netapp_image_name]['ports']:
                result=isOpen(netapp_host, port, mapped_ports)
                if not result:
                    success = False
                ports_checked.append({"port":port,"mapped_port":mapped_ports[port],"listening":result})

            NetApp_ports[netapp_image_name]['ports']=ports_checked
            

        else:
            print("\n\nNo docker-compose y(a)ml found in your repository.\nPlease make sure your NetApp (repository) has a docker-compose yaml file.")

        json_formatted_str = json.dumps(NetApp_ports, indent=2)

        print(json_formatted_str)

        netapp_name = git_repository.split('/')[-1].lower()

        with open('report-build-ports-'+ netapp_name +'.json', 'w', encoding='utf-8') as f:
            json.dump(NetApp_ports, f, ensure_ascii=False)

        # Repository folder removed
        shutil.rmtree(repo_dir)
        if (not success):
            raise Exception("One or more Network App ports are not listening properly")
    
    except git.exc.GitError as err:
        print(str(err))
