# .PHONY defines parts of the makefile that are not dependant on any specific file
# This is most often used to store functions
.PHONY = help setup test run clean prepare_dev_package prepare_package package_dev package

# NetAPP Name
EXECUTABLE_NAME=$(name)

# Signifies our desired python version
# Makefile macros (or variables) are defined a little bit differently than traditional bash, keep in mind that in the Makefile there's top-level Makefile-only syntax, and everything else is bash script syntax.
PYTHON = python3
PIP3 = pip3

# Defines the default target that `make` will to try to make, or in the case of a phony target, execute the specified commands
# This target is executed whenever we just type `make`
.DEFAULT_GOAL = help

#Checking Jenkins Workspace
BRANCH := $(if $(JENKINS_HOME),$(shell basename $(GIT_BRANCH)),$(shell git rev-parse --abbrev-ref HEAD))
COMMIT := $(shell git rev-parse --short HEAD)
TAG := $(shell git describe --abbrev=0 --tags)

# The @ makes sure that the command itself isn't echoed in the terminal
help:
	@echo "---------------HELP-----------------"
	@echo "To setup the project type make name=netappName setup"
	@echo "To test the project type make name=netappName test"
	@echo "To run the project type make name=netappName run"
	@echo "------------------------------------"

# This generates the desired project file structure
# A very important thing to note is that macros (or makefile variables) are referenced in the target's code with a single dollar sign ${}, but all script variables are referenced with two dollar signs $${}
setup:
	
	@echo "Install NetApp requirements"
	${PIP3} install -r requirements.txt

# The ${} notation is specific to the make syntax and is very similar to bash's $() 
# This function uses pytest to test our source files
test:
	${PYTHON} -m pytest
	
run:
	${PYTHON} our_app.py <-- TO REVIEW

clean: ## task to clean project
	@echo "Start clean"
	@rm -fr ./*
	@echo "Finish clean"

prepare_dev_package: ## task to prepare code package <-- TO REVIEW
	@echo "Start prepare code package"
	@cp -R ./build ./$(EXECUTABLE_NAME)-$(BRANCH)-$(COMMIT)
	@echo "Finish prepare package"

package_dev: ## task to code package <-- TO REVIEW
	@echo "Start package"
	@tar czfv $(EXECUTABLE_NAME)-$(BRANCH)-$(COMMIT).tar.gz $(EXECUTABLE_NAME)-$(BRANCH)-$(COMMIT)
	@echo "Finish package"

prepare_package: ## task to prepare code package <-- TO REVIEW
	@echo "Start prepare code package"
	@cp -R ./build ./$(EXECUTABLE_NAME)-$(TAG)
	@echo "Finish prepare package"

package: ## task to code package
	@echo "Start package"
	@tar czfv ./$(EXECUTABLE_NAME)-$(TAG).tar.gz ./$(EXECUTABLE_NAME)-$(TAG)
	@echo "Finish package"

publish: ## task to upload to artifactory whatever
	@echo "Start publish"
