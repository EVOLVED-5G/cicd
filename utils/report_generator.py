
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

THIS_DIR = os.path.dirname(os.path.abspath(__file__))


template = sys.argv[2]
json_filename = sys.argv[4]

def render(template,json_data):
    return jinja2.Environment(
        loader=jinja2.FileSystemLoader(THIS_DIR)
    ).get_template(template).render(json_data)


# load json from file

jsonConfigName = json_filename
with open(jsonConfigName) as json_file:
    json_data = json.load(json_file)

##Apply the template
result =render(template,json_data)
##Naming of the report
name=template.split("-")[1].split(".")[0] 
if (name == "image"):
    ## This variable is added to differentiate when there are more than one image for the application
    name_aux = json_filename.split("-")[-1].split(".")[0]
    outputFileName = 'report-'+name+"-"+name_aux+'.md'
else:
    outputFileName = 'report-'+name+'.md'
# write output to a file 
outFile = open(outputFileName, "w")
outFile.write(result)
outFile.close()