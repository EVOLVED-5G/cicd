import json,sys

# total arguments
n = len(sys.argv)

if n != 5:
    print("expected: " + sys.argv[0] + " <json_file> <netapp_image_name> <tag> <image_url>")
    exit(255)

json_file = sys.argv[1]
netapp_image_name = sys.argv[2]
tag = sys.argv[3]
image_url = sys.argv[4]

# Opening JSON file
f = open(json_file)
  
# returns JSON object as 
# a dictionary
data = json.load(f)

if data['services'].get(netapp_image_name,None) == None:
    data['services'][netapp_image_name] = dict()

if data['services'][netapp_image_name].get(tag,None) == None:
    data['services'][netapp_image_name][tag] = list()

data['services'][netapp_image_name][tag].append(image_url)

f.close()

with open(json_file, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False)
  