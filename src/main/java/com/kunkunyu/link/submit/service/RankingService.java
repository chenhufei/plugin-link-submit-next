package com.kunkunyu.link.submit.service;

import java.util.List;
import java.util.Map;

public interface RankingService {

    List<Map<String, Object>> getLinkRanking(int limit);

    int calculateScore(String url);
}
