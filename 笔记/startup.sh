cd ..
mvn clean install -Prelease-all -DskipTest
cd distribution
bash bin/mqnamesrv
bash bin/mqbroker -n localhost:9876