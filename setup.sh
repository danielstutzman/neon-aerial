#!/bin/bash -ex
RELEASE=jai_imageio-1_1-lib-linux-amd64.tar.gz
curl http://download.java.net/media/jai-imageio/builds/release/1.1/$RELEASE > $RELEASE
tar zxvf $RELEASE
rm $RELEASE
