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
repo = sys.argv[8]
branch = sys.argv[10]
commit = sys.argv[12]

def render(template,json_data, licenses):
    return jinja2.Environment(
        loader=jinja2.FileSystemLoader(THIS_DIR)
    ).get_template(template).render(json_data, licenses=licenses, repo=repo, branch=branch, commit=commit)

# load json from file
jsonConfigName = json_filename
with open(jsonConfigName) as json_file:
    json_data = json.load(json_file)

licenses_summary = {}

for package in json_data["packages"]:
    license = package["license"]
    if license in licenses_summary.keys():
        licenses_summary[license]["count"] += 1
    else:
        licenses_summary[license] = {"count" : 1}

##Apply the template
result =render(template, json_data, licenses_summary)

# write output to a file
outFile = open(output_filename, "w")
outFile.write(result)
outFile.close()