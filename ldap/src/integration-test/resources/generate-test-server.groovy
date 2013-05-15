File file = new File("/home/rwinch/git/spring-security-3.2.x/ldap/src/integration-test/resources/test-server.ldif")

boolean append = true
FileWriter fileWriter = new FileWriter(file, append)
BufferedWriter buffWriter = new BufferedWriter(fileWriter)

10000.times { count-> buffWriter.write """

dn: uid=user${count},ou=people,dc=springframework,dc=org
objectclass: top
objectclass: person
objectclass: organizationalPerson
objectclass: inetOrgPerson
cn: User ${count}
sn: Hamilton
uid: user${count}
userPassword: password""" }

buffWriter.flush()
buffWriter.close()