#!/bin/bash

##
# Simple script to start service.
##

CONFIG_LOCATION=$HOME/.idschemetest/idscheme-test.properties
SERVER_PORT=8888 java -jar target/idscheme-test-1.0-SNAPSHOT.jar \
   --spring.config.location=file:$CONFIG_LOCATION \
   2>&1 >> idschemetest.log
