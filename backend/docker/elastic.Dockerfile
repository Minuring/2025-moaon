FROM docker.elastic.co/elasticsearch/elasticsearch:9.1.4

RUN bin/elasticsearch-plugin install --batch analysis-nori

USER elasticsearch

# 사전 파일은 compose에서 config/analysis에 마운트한다. 이 빈 파일은 마운트하지 않았을 때를 위한 것이다.
RUN mkdir -p /usr/share/elasticsearch/config/analysis \
    && touch /usr/share/elasticsearch/config/analysis/dictionary.txt
