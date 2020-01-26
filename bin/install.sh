#!/bin/bash
FILE=ctrail-0.0.7.jar
aws s3 cp s3://alertnest-releases/color-trail/$FILE /tmp/$FILE
sudo mv /tmp/$FILE /usr/local/lib

if [ -e /usr/local/lib/ctrail.jar ] ; then
    sudo rm /usr/local/lib/ctrail.jar
fi
sudo ln -s /usr/local/lib/$FILE /usr/local/lib/ctrail.jar

FILE=ctrail.xml
aws s3 cp s3://alertnest-releases/color-trail/$FILE /tmp/$FILE
if [ ! -e /ec/$FILE ] ; then 
    sudo mv /tmp/$FILE /etc/$FILE
else
    echo "NOT replacing pre-exisitng file: /etc/$FILE"
fi


FILE=ctr
aws s3 cp s3://alertnest-releases/color-trail/$FILE /tmp/$FILE
sudo mv /tmp/$FILE /usr/local/bin
chmod +x /usr/local/bin/ctr




