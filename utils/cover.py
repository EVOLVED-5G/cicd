#!/usr/bin/python
import sys, getopt
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas
from reportlab.lib.colors import yellow, red, black,white
from reportlab.pdfbase.pdfmetrics import stringWidth



## DefaultPageSize of a A4 paper
PAGE_WIDTH  = 595
PAGE_HEIGHT = 891

def generate_cover(title, date, buildnumber):

    cover = canvas.Canvas("cover.pdf", pagesize=A4)
    cover.setFont("Times-Roman", 40)
    #A4 = 595x891
    ##Part of the code to adjust the text to the file
    text = title
    text_width = stringWidth(text)
    y=750 ## y position (height)
    ##Handle the case if the text_width is larger than the PAGE_WIDTH
    if title != None:
        cover.drawString((PAGE_WIDTH - text_width) / 2.0, y, text)
    if date != None:
        cover.setFont("Helvetica", 20)
        cover.drawString(400, 80, "Date: %s" %date)
    if buildnumber != None:
        cover.setFont("Times-Roman", 20)
        cover.drawString(400, 50, "Build Number: %s" %buildnumber)
    cover.setFillColorRGB(.963,.344,.285,1)
    cover.rect(30, 20, 40, 800, 0, 1)
    cover.drawImage("utils/e5g.png", 170, 350)
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