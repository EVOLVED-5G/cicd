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
print("total arguments: " + str(total_arguments))
if len(sys.argv) > 7:
    for index in range(7, total_arguments, 2):
        print("Index: " + str(index))
        if index + 1 >= total_arguments:
            print("Parameter without value present on command invocation")
            break
        value = sys.argv[index+1]
        parameter_name = sys.argv[index].replace('--','')

        print("Add parameter " + parameter_name + ":" + value)
        optional_arguments[parameter_name] = value
print("Optional Arguments:")
print(json.dumps(optional_arguments, indent=4))

def render(template,json_data):
    return jinja2.Environment(
        loader=jinja2.FileSystemLoader(THIS_DIR)
    ).get_template(template).render(json_data)

# load json from file
jsonConfigName = json_filename
with open(jsonConfigName) as json_file:
    json_data = json.load(json_file)

json_data.update(optional_arguments)

##Apply the template
result =render(template,json_data)

# write output to a file 
outFile = open(output_filename, "w")
outFile.write(result)
outFile.close()