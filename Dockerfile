FROM nuxeo/nuxeo:master

RUN cat /etc/nuxeo/nuxeo.conf.template > $NUXEO_CONF
WORKDIR /tmp
ADD nuxeo-zapier-package/target/nuxeo-zapier-package-*.zip /tmp/nuxeo-zapier-package.zip
RUN printf 'a3dacf36-3d51-4f94-b492-358eec2380be.1924935180.RdZbFbmfG3nuCMpk/5Cfxnf/FiDc/JeZUfBdlND+mk6Bd8bdc+TSIxoci2QInz6bvGIG2mvmLzcXV39jzB9Tzr/IMJ2LWW4hZtuhkmCWHQ+NuZhYvJSSjLlJIRQ/VdrjETa0v6vD3x5IGYwruHzlRX5/o20F6AkH/TkxU9jdVEgXjxr3ZLifJix5J6jV6aby0FodTrFNlKCm7rNJkWPKdKaEhtyo1XbqroEmceOMGTGgXE0wUZqZyv7yXoR5ndnLzxvreHSEGy1PFgRtZOEJfIrrVy0qn4LQKpcUEq+vi7u7uECcdwEkyHY6H8jEE7m1ffUn1qTAWVxCK5nRM8qu0w== \nf14d6d68-5e0a-4b96-9e1c-3d12191f8aee' >> $NUXEO_HOME/nxserver/data/instance.clid
RUN nuxeoctl mp-install nuxeo-jsf-ui /tmp/nuxeo-zapier-package.zip --nodeps
