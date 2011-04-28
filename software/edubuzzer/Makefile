PROJECT ?= edubuzzer
WWWDIR  ?= /srv/www
APPS    ?= /usr/share/applications
ICONS   ?= /usr/share/icons/hicolor/scalable/apps

install:
	install -m u+w,a+r -t $(WWWDIR)/$(PROJECT) share/static/*
	install -m u+w,a+r -t $(APPS)  share/apps/*
	install -m u+w,a+r -t $(ICONS) share/icons/*
