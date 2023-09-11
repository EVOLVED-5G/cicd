#!/usr/bin/python

import sys, getopt
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas
from reportlab.lib.colors import yellow, red, black,white
from reportlab.pdfbase.pdfmetrics import stringWidth
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

def generate_cover(title, date, stage):

    cover = canvas.Canvas("cover.pdf", pagesize=A4)
    pdfmetrics.registerFont(TTFont('Georgia', './utils/fonts/georgia/georgia.ttf'))
    cover.setFont("Georgia", 40)

    #A4 = 595x891
    PAGE_WIDTH  = A4[0]
    PAGE_HEIGHT = A4[1]


    if title != None:
        cover.setFont("Georgia", 20)
        title_width = stringWidth("Network App %s Report: %s" %(stage,title), "Georgia", 20)
        cover.drawString(int((PAGE_WIDTH - title_width) / 2.0), 750, "Network App %s Report: %s" %(stage,title))
    if date != None:
        cover.setFont("Georgia", 20)
        cover.drawString(int((PAGE_WIDTH - title_width) / 2.0), 720, "Date: %s" %date)
    # if buildnumber != None:
    #     cover.setFont("Times-Roman", 20)
    #     cover.drawString(400, 50, "Build Number: %s" %buildnumber)
    cover.setFillColorRGB(.963,.344,.285,1)
    cover.rect(30, 20, 40, 800, 0, 1)
    cover.drawImage("utils/e5g.png", 160, 350)
    cover.save()


def main(argv):
    title = None
    date = None
    stage = 'Validation'
    try:
        opts, args = getopt.getopt(argv,"ht:d:s:")
    except getopt.GetoptError:
        print ('generate_cover.py -t <title> -d <date>')
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print ('test.py -t <title> -d <21/11/2022>')
            sys.exit()
        elif opt in ("-t"):
            title = arg
        elif opt in ("-d"):
            date = arg
        elif opt in ("-s"):
            stage = arg.title()
    generate_cover(title, date, stage)


if __name__ == "__main__":
   main(sys.argv[1:])