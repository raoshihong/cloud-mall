package com.rao.cloud.mall.search;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MallSearchApplicationTests {

    @Autowired
    private RestHighLevelClient client;

    @Test
    public void indexData() throws Exception{

        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");

        String json = "{\"name\":\"aaa\"}";
        indexRequest.source(json, XContentType.JSON);

        IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);

        System.out.println(indexResponse.toString());
    }

    @Test
    public void searchData()throws Exception{

        SearchRequest searchRequest = new SearchRequest();
        //指定索引
        searchRequest.indices("bank");

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchQuery("address","mill"));
        sourceBuilder.from(1);
        sourceBuilder.size(100);

        System.out.println(sourceBuilder);

        searchRequest.source(sourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        System.out.println(searchResponse.toString());

    }

    @Test
    public void agg()throws Exception{
        SearchRequest searchRequest = new SearchRequest();
        //指定索引
        searchRequest.indices("bank");

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchAllQuery());

        // 使用聚合
        AvgAggregationBuilder aggregationBuilder = AggregationBuilders.avg("ageAgg").field("age");
        sourceBuilder.aggregation(aggregationBuilder);

        System.out.println(sourceBuilder);

        searchRequest.source(sourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        System.out.println(searchResponse.toString());
    }
}
