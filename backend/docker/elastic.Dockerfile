FROM docker.elastic.co/elasticsearch/elasticsearch:9.1.4

RUN bin/elasticsearch-plugin install --batch analysis-nori

USER elasticsearch

RUN touch /usr/share/elasticsearch/config/dictionary.txt
