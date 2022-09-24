#!/usr/bin/python
import sys, getopt
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas
from reportlab.lib.colors import yellow, red, black,white

def generate_cover(title, date, buildnumber):

    cover = canvas.Canvas("cover.pdf", pagesize=A4)
    cover.setFont("Times-Roman", 40)
    #A4 = 595x891
    if title != None:
        cover.drawString(200, 750, title)
    if date != None:
        cover.setFont("Helvetica", 20)
        cover.drawString(400, 80, "Date: %s" %date)
    if buildnumber != None:
        cover.setFont("Times-Roman", 20)
        cover.drawString(400, 50, "Build Number: %s" %buildnumber)
    cover.setFillColorRGB(.963,.344,.285,1)
    cover.rect(30, 20, 40, 800, 0, 1)
    cover.drawImage("e5g.png", 170, 350)
    cover.save()


def main(argv):
    title = None
    date = None
    buildnumber = None
    try:
        opts, args = getopt.getopt(argv,"ht:d:b:",["title=","date=","buildnumber="])
    except getopt.GetoptError:
        print ('generate_cover.py -t <title> -d <date> -b <buildnumber>')
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print ('test.py -t <title> -d <date> -b <buildnumber>')
            sys.exit()
        elif opt in ("-t", "--title"):
            title = arg
        elif opt in ("-d", "--date"):
            date = arg
        elif opt in ("-b", "--buildnumber"):
            buildnumber = arg
    generate_cover(title, date, buildnumber)


if __name__ == "__main__":
   main(sys.argv[1:])