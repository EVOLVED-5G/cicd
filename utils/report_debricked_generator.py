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
version = sys.argv[14]
url = sys.argv[16]

def render(template,json_data, licenses, dependencies):
    return jinja2.Environment(
        loader=jinja2.FileSystemLoader(THIS_DIR)
    ).get_template(template).render(json_data, licenses=licenses, dependencies=dependencies, repo=repo, branch=branch, commit=commit, version=version, url=url)

# load json from file
jsonConfigName = json_filename
with open(jsonConfigName) as json_file:
    json_data = json.load(json_file)

licenses_summary = {}
dependencies_summary = {}

for dependency in json_data["dependencyLicenses"]:
    for license in dependency["licenses"]:
        if license in licenses_summary:
            print ("License already found %s" %license)
            print (licenses_summary[license])
            licenses_summary[license]["count"] += 1
        else:
            print ("License not found %s" %license)
            licenses_summary[license] = {"count" : 1}
            licenses_summary[license]["family"] = dependency["families"][dependency["licenses"].index(license)]

    dependencies_summary[dependency["name"]] = {}
    dependencies_summary[dependency["name"]]["version"] = dependency["version"]
    dependencies_summary[dependency["name"]]["licenses"] = ', '.join(dependency["licenses"])
    dependencies_summary[dependency["name"]]["families"] = ', '.join(dependency["families"])

##Apply the template
result =render(template, json_data, licenses_summary, dependencies_summary)

# write output to a file
outFile = open(output_filename, "w")
outFile.write(result)
outFile.close()