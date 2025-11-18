package com.repackio.backbreaker.backbreaker.processing;

public interface CardProcessingHandler {

    void handle(CardProcessingContext context) throws Exception;
}
