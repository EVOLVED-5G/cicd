import socket, os, sys, shutil, re, git, nmap3, glob

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

   except:
       print(f"NetApp TCP Port {int(port)}, is NOT LISTENING/NOT OPENED.")


def find_docker_compose(name, path):
    for root, dirs, files in os.walk(path):
        if name in files:
            return os.path.join(root, name)


if __name__ == '__main__':
    NetApp_ports=[]

    repo = input("Please provide the NetApp repository:\n")
    git_url = "https://github.com/EVOLVED-5G/"+repo

    #Create repository folder (then it'll be removed)
    repo_dir = os.path.abspath(os.getcwd()) + "/ports_tmp/"
    
    if (os.path.isdir(repo_dir)):
      shutil.rmtree(repo_dir)

    try:
        git.Repo.clone_from(git_url, repo_dir, branch="evolved5g")
        repo_docker_file = glob.glob(repo_dir + 'docker-compose.y*ml')
        
        # If there is no docker-compose yaml then, the python stops.
        if (bool(repo_docker_file)):

            repo_dir_docker = find_docker_compose(os.path.basename(repo_docker_file[0]), repo_dir)
            with open(repo_dir_docker) as input:
                for line in input:
                    if 'ports' in line:
                        NetApp_ports += re.findall(r'\d+:', next(input))

            for port in NetApp_ports:
                isOpen(socket.gethostname(), port.split(":")[0])

        else:
            print("\n\nNo docker-compose y(a)ml found in your repository.\nPlease make sure your NetApp (repository) has a docker-compose yaml file.")

        # Repository folder removed
        shutil.rmtree(repo_dir)
    
    except git.exc.GitError as err:
        print(str(err))
