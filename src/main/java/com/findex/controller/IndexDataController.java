package com.findex.controller;

import com.findex.dto.indexdata.IndexDataCreateRequest;
import com.findex.dto.indexdata.IndexDataDto;
import com.findex.dto.indexdata.IndexDataQuery;
import com.findex.dto.indexdata.IndexDataUpdateRequest;
import com.findex.dto.response.CursorPageResponse;
import com.findex.service.IndexDataExportService;
import com.findex.service.IndexDataService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/index-data")
public class IndexDataController {
    private final IndexDataService indexDataService;
    private final IndexDataExportService indexDataExportService;

    @PostMapping
    public ResponseEntity<IndexDataDto> create(@RequestBody @Valid IndexDataCreateRequest request) {
        IndexDataDto indexDataDto = indexDataService.create(request);
        return ResponseEntity.ok(indexDataDto);
    }

    @GetMapping
    public ResponseEntity<CursorPageResponse> findAll(@ModelAttribute IndexDataQuery indexDataQuery) {
        CursorPageResponse cursorPageResponse = indexDataService.findAll(indexDataQuery);
        return ResponseEntity.ok(cursorPageResponse);
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

    @GetMapping("/export/csv")
    public void exportCSV(@ModelAttribute IndexDataQuery indexDataQuery, HttpServletResponse response) {
        response.setContentType("text/csv; charset=UTF-8");
        String fileName = String.format("index-data-export-%s.csv", LocalDate.now());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

        try (OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            indexDataExportService.generateCSV(indexDataQuery, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
