package com.repackio.backbreaker.processing;

public interface CardProcessingHandler {

    void handle(CardProcessingContext context) throws Exception;
}
