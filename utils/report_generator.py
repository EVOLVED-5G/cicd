#!/usr/bin/python
#
# prereq:
# sudo apt-get install python-pip -y
# sudo pip install jinja2
#
import json
import os
import sys
import jinja2


THIS_DIR = os.path.dirname(os.path.abspath(__file__))+"/../"
template = sys.argv[2]
json_filename = sys.argv[4]
output_filename = sys.argv[6]

optional_arguments = dict()
total_arguments = len(sys.argv)
if len(sys.argv) > 7:
    for index in range(7, total_arguments, 2):
        if index + 1 < total_arguments:
            print("Parameter without value present con command invocation")
            break
        parameter_name = sys.argv[index].replace('--','')
        optional_arguments[parameter_name] = sys.argv[index+1]


# repo = sys.argv[8]
# branch = sys.argv[10]
# commit = sys.argv[12]
# version = sys.argv[14]
# url = sys.argv[16]
# name = sys.argv[18]

# if sys.argv[20]:
#     counter = sys.argv[20]

def render(template,json_data):
    return jinja2.Environment(
        loader=jinja2.FileSystemLoader(THIS_DIR)
    ).get_template(template).render(json_data, optional_arguments)
# ).get_template(template).render(json_data, repo=repo, branch=branch, commit=commit, version=version, url=url, name=name, counter=counter)

# load json from file
jsonConfigName = json_filename
with open(jsonConfigName) as json_file:
    json_data = json.load(json_file)

##Apply the template
result =render(template,json_data)

# write output to a file 
outFile = open(output_filename, "w")
outFile.write(result)
outFile.close()