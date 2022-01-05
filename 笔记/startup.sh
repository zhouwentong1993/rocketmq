cd ..
mvn clean install -Prelease-all -D maven.test.skip=true
cd distribution
bash bin/mqnamesrv
#bash bin/mqbroker -n localhost:9876