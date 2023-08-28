#!/usr/bin/python

import sys, getopt
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas
from reportlab.lib.colors import yellow, red, black,white
from reportlab.pdfbase.pdfmetrics import stringWidth
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

def generate_fingerprint(fingerprint, network_app):
    fingerprint_page = canvas.Canvas("fingerprint.pdf", pagesize=A4)
    pdfmetrics.registerFont(TTFont('Georgia', './utils/fonts/georgia/georgia.ttf'))
    fingerprint_page.setFont("Georgia", 40)

    #A4 = 595x891
    PAGE_WIDTH  = A4[0]
    PAGE_HEIGHT = A4[1]


    if fingerprint != None:
        title_span = 30
        title_font_size =30
        fingerprint_page.setFont("Georgia", title_font_size)
        title_width = stringWidth("Fingerprint", "Georgia", title_font_size)
        fingerprint_page.drawString(int((PAGE_WIDTH - title_width) / 2.0), int((PAGE_HEIGHT - title_font_size - title_span)), "Fingerprint")

        second_title_font_size =15
        fingerprint_page.setFont("Georgia", second_title_font_size)
        title_width = stringWidth("%s" %network_app, "Georgia", second_title_font_size)
        fingerprint_page.drawString(int((PAGE_WIDTH - title_width) / 2.0), int(PAGE_HEIGHT - title_font_size - 2 * title_span - second_title_font_size), "%s" %network_app)

        
        box_height=60
        font_size=20
        fingerprint_page.setFont("Georgia", font_size)
        fingerprint_width = stringWidth("%s" %fingerprint, "Georgia", font_size)
        fingerprint_span = 30
        fingerprint_page.setFillColorRGB(.0,0,1,0.30)
        fingerprint_page.setStrokeColor(black)
        fingerprint_page.rect(int((PAGE_WIDTH - fingerprint_width - fingerprint_span) / 2.0), int(((PAGE_HEIGHT - box_height + font_size/2) / 2.0)), fingerprint_width + fingerprint_span, box_height, fill=True, stroke=True)
        fingerprint_page.setFillColor(black)
        fingerprint_page.drawString(int((PAGE_WIDTH - fingerprint_width) / 2.0), int((PAGE_HEIGHT) / 2.0), "%s" %fingerprint)

    fingerprint_page.setFillColorRGB(.963,.344,.285,1)
    fingerprint_page.rect(30, 20, 40, 800, 0, 1)
    # fingerprint_page.drawImage("utils/e5g.png", 160, 350)
    fingerprint_page.save()


def main(argv):
    fingerprint = None
    network_app = None
    try:
        opts, args = getopt.getopt(argv,"hf:n:")
    except getopt.GetoptError:
        print ('generate_fingerprint.py -f <fingerprint> -n <network_app>')
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print ('generate_fingerprint.py -f <fingerprint> -n <network_app>')
            sys.exit()
        elif opt in ("-f"):
            fingerprint = arg
        elif opt in ("-n"):
            network_app = arg
    generate_fingerprint(fingerprint,network_app)


if __name__ == "__main__":
   main(sys.argv[1:])