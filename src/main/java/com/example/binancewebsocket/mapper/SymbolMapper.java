package com.example.binancewebsocket.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SymbolMapper {
    /**
     * symbols 테이블에서 symbol 컬럼을 조회하여 리스트로 반환합니다.
     *
     * @return 전체 symbol 목록
     */
    List<String> selectAllSymbols();
}
