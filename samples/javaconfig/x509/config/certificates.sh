#!/bin/bash

# For the following commands, set the values in parenthesis to be whatever makes sense for your environment.  The parenthesis are not necessary for the command.

# This is an all-in-one command that generates a certificate for the server and places it in a keystore file, while setting both the certifcate password and the keystore password.
# The net result is a file called "tomcat.keystore".
export SERVER_ALIAS=tomcat
export CLIENT_ALIAS=user
export KEYPASS=password
export STOREPASS=$KEYPASS

keytool -genkeypair -alias $SERVER_ALIAS -keyalg RSA -dname "CN=localhost,OU=security,O=spring,L=Chicago,ST=IL,C=US" -keystore tomcat.keystore -keypass $KEYPASS -storepass $STOREPASS -deststoretype pkcs12

# This is the all-in-one command that generates the certificate for the client and places it in a keystore file, while setting both the certificate password and the keystore password.
# The net result is a file called "client.keystore"

keytool -genkeypair -alias $CLIENT_ALIAS -keyalg RSA -dname "CN=$CLIENT_ALIAS,OU=security,O=spring,L=Chicago,ST=IL,C=US" -keypass $KEYPASS -keystore client.keystore -storepass $STOREPASS -deststoretype pkcs12

# This command exports the client certificate.
# The net result is a file called "client.cer" in your home directory.

keytool -exportcert -rfc -alias $CLIENT_ALIAS -file client.cer -keypass $KEYPASS -keystore client.keystore -storepass $STOREPASS -deststoretype pkcs12

# This command imports the client certificate into the "tomcat.keystore" file.

keytool -importcert -alias $CLIENT_ALIAS -file client.cer -keystore tomcat.keystore -storepass $STOREPASS -noprompt -deststoretype pkcs12

openssl pkcs12 -in client.keystore  -nokeys -out client.pem
openssl pkcs12 -in client.keystore -nodes -nocerts -out client.pem
