SHELL=/bin/bash

ifndef VERSION
VERSION=$(shell grep version.number= ${PWD}/../version.properties | cut -d= -f 2)
endif
ifndef RELEASE
RELEASE=$(shell grep version.release.number= ${PWD}/../version.properties | cut -d= -f 2)
endif

TAG?=snapshot

RPMBUILD_DEFINES = --define "_topdir ${PWD}/dist" 
RPMBUILD_DEFINES+= --define "_version ${VERSION}"
RPMBUILD_DEFINES+= --define "_release ${RELEASE}"

ifndef BUILD_NUMBER
ALPHATAG?=$(shell date "+%Y%m%d%H%M")$(TAG)
else
ALPHATAG?=$(TAG)
RPMBUILD_DEFINES+= --define "_buildnumber $(BUILD_NUMBER)"
endif

ifndef GA_RELEASE
RPMBUILD_DEFINES+= --define "_alphatag $(ALPHATAG)"
endif

.PHONY: rpm msi

rpm: dist
	rpmbuild -bb --target=noarch-linux $(RPMBUILD_DEFINES) --buildroot ${PWD}/dist/SOURCES rundeck.spec 

msi: dist
	@if ! which candle.exe >&/dev/null; then \
	   echo 'ERROR: Please put WIX-35 tools in your path.'; exit 1; \
	 fi
	cp LICENSE.rtf dist/SOURCES ;\
	cd dist/SOURCES ;\
	heat.exe dir "." -nologo \
	  -var var.SOURCES \
	  -gg -scom -sreg -sfrag -srd \
	  -dr INSTALLDIR \
	  -cg RunDeckComponentGroup \
	  -out ../FileFragment.wxs ;\
	candle.exe -nologo \
	  -dSOURCES=. \
	  -dVERSION=1.3.0 \
	  -out ../ \
	  ../FileFragment.wxs ../../rundeck.wxs ;\
	light.exe -nologo \
	  -dSOURCES=. \
	  -dVERSION=1.3.0 \
	  -out ../rundeck.msi \
	  -ext WixUIExtension -cultures:en-us \
	  ../rundeck.wixobj ../FileFragment.wixobj

dist:
	@mkdir -p dist/RPMS/noarch
	@mkdir -p dist/SOURCES
	@mkdir -p dist/SOURCES/usr/bin
	@mkdir -p dist/SOURCES/var/lib/rundeck
	@mkdir -p dist/SOURCES/var/lib/rundeck/exp/webapp
	@mkdir -p dist/SOURCES/var/lib/rundeck/{data,work,logs,cli,libext}
	@mkdir -p dist/SOURCES/var/log/rundeck
	@mkdir -p dist/SOURCES/var/lib/rundeck/var/tmp
	@mkdir -p dist/SOURCES/var/rundeck/projects
	@mkdir -p dist/SOURCES/usr/share/man/man{1,5}
	@mkdir -p dist/SOURCES/usr/share/man/en/man{1,5}
	@cp -R root/* dist/SOURCES
	@-cp ../docs/en/manpages/man1/*.1.gz dist/SOURCES/usr/share/man/man1
	@-cp ../docs/en/manpages/man5/*.5.gz dist/SOURCES/usr/share/man/man5
	@-cp ../docs/en/manpages/man1/*.1.gz dist/SOURCES/usr/share/man/en/man1
	@-cp ../docs/en/manpages/man5/*.5.gz dist/SOURCES/usr/share/man/en/man5
	@cp -R ../core/build/tools/lib/*.jar dist/SOURCES/var/lib/rundeck/cli
	@cp ../core/build/libs/rundeck-core-*.jar dist/SOURCES/var/lib/rundeck/cli
	@cp ../rundeckapp/target/rundeck-server-*.jar dist/SOURCES/var/lib/rundeck/bootstrap	
	@cp ../plugins/*-plugin/build/libs/*-plugin*.jar dist/SOURCES/var/lib/rundeck/libext
	@find ../core/src/resources/com/dtolabs/rundeck/core/cli/templates ! -regex ".*.bat$$" -and -type f | sed -e /rd-check/d -e /rd-setup/d | xargs -I Z cp Z dist/SOURCES/usr/bin
	@unzip -oq ../rundeckapp/target/rundeck-$(VERSION).war -d dist/SOURCES/var/lib/rundeck/exp/webapp/
	ln -s /etc/rundeck/log4j.properties; mv log4j.properties dist/SOURCES/var/lib/rundeck/exp/webapp/WEB-INF/classes
    
clean:
	rm -rf target dist/
