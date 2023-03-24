import socket, os, shutil, re, git, nmap3, glob, sys, docker

def isOpen(host,port):
    #Netstat
   tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    #Nmap
   nmap = nmap3.NmapScanTechniques()

   try:
      #Checking ports with Netstat
      tcp.connect((host, int(port)))
      #Checking ports with Nmap
      resultstcp = nmap.nmap_tcp_scan(host, args="-p " + port)
      tcp.shutdown(2)
      print (f"NetApp TCP Port {int(port)}, is LISTENING/OPENED")
      return True

   except:
       print(f"NetApp TCP Port {int(port)}, is NOT LISTENING/NOT OPENED.")
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

    print("Netapp repository: " + git_repository)
    print("Netapp Host: " + netapp_host )
    
    netapp_image_name=''
    if n==4:
        netapp_image_name=sys.argv[3]
        print("Netapp Image Name: " + netapp_image_name )

    NetApp_ports=[]

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
            with open(repo_dir_docker) as input:
                for line in input:
                    if 'ports' in line:
                        NetApp_ports += re.findall(r'\d+:', next(input))

            for port in NetApp_ports:
                if( not isOpen(netapp_host, port.split(":")[0])):
                    success=False
        elif (bool(repo_docker_file)):
            repo_dir_docker = find_file(os.path.basename(repo_docker_file[0]), repo_dir)
            with open(repo_dir_docker) as input:
                for line in input:
                    NetApp_ports += re.findall(r'EXPOSE \d+ ?\d*', line).split(" ")[1]

            # Get docker ports information
            client = docker.from_env()
            containers_list=client.containers.list(filters={"ancestor":netapp_image_name})
            container=dict()

            if len(containers_list) == 1:
                container=containers_list[0]
                print("Container " + container.attrs['Config']['Image'] + "found")
            else:
                raise Exception("Netapp " + netapp_image_name + " container not found")

            container_ports_info=container.ports
            container_ports_list=list(container_ports_info.keys())

            if len(NetApp_ports) != len(container_ports_list):
                raise Exception("Netapp ports on Dockerfile not match ports exposed on running image")
            
            mapped_keys=dict()
            for dockerfile_port in NetApp_ports:
                for container_port in container_ports_list:
                    key_port_without_protocol=container_port.split("/")[0]
                    if key_port_without_protocol == dockerfile_port:
                        mapped_keys[dockerfile_port]=container_ports_info[container_port][0]['HostPort']

            if len(list(mapped_keys.keys())) != len(NetApp_ports):
                raise Exception("Netapp ports on Dockerfile not match ports map with running image")

            for port in NetApp_ports:
                if( not isOpen(netapp_host, mapped_keys[port])):
                    success=False

        else:
            print("\n\nNo docker-compose y(a)ml found in your repository.\nPlease make sure your NetApp (repository) has a docker-compose yaml file.")

        # Repository folder removed
        shutil.rmtree(repo_dir)
        if (not success):
            raise Exception("Netapp ports not found")
            # exit(1)
    
    except git.exc.GitError as err:
        print(str(err))
