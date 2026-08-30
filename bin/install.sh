#!/bin/bash
FILE=ctrail-1.1.1.jar
aws s3 cp s3://alertnest-releases/color-trail/$FILE /tmp/$FILE
sudo mv /tmp/$FILE /usr/local/lib

if [ -e /usr/local/lib/ctrail.jar ] ; then
    sudo rm /usr/local/lib/ctrail.jar
fi
sudo ln -s /usr/local/lib/$FILE /usr/local/lib/ctrail.jar

FILE=ctrail.xml
aws s3 cp s3://alertnest-releases/color-trail/$FILE /tmp/$FILE
# /ec/ was a typo for /etc/ -- the test never matched, so every install
# silently clobbered the user's existing config
if [ ! -e /etc/$FILE ] ; then
    sudo mv /tmp/$FILE /etc/$FILE
else
    echo "NOT replacing pre-existing file: /etc/$FILE"
fi


FILE=ctr
aws s3 cp s3://alertnest-releases/color-trail/$FILE /tmp/$FILE
sudo mv /tmp/$FILE /usr/local/bin
sudo chmod +x /usr/local/bin/ctr




