package com.findex.controller;

import com.findex.dto.indexdata.IndexDataCreateRequest;
import com.findex.dto.indexdata.IndexDataDto;
import com.findex.dto.indexdata.IndexDataUpdateRequest;
import com.findex.service.IndexDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/index-data")
public class IndexDataController {
    private final IndexDataService indexDataService;

    @PostMapping
    public ResponseEntity<IndexDataDto> create(@RequestBody @Valid IndexDataCreateRequest request) {
        IndexDataDto indexDataDto = indexDataService.create(request);
        return ResponseEntity.ok(indexDataDto);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<IndexDataDto> update(@PathVariable Long id, @RequestBody IndexDataUpdateRequest request) {
        IndexDataDto indexDataDto = indexDataService.update(id, request);
        return ResponseEntity.ok(indexDataDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        indexDataService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
