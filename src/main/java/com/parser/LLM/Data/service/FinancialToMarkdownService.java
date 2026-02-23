package com.parser.LLM.Data.service;

import com.parser.LLM.Data.dto.request.*;
import com.parser.LLM.Data.dto.response.FinancialConvertResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FinancialToMarkdownService {

    public FinancialConvertResponse  convertJsonDataInMarkDown(FinancialConvertRequest request){
        return new FinancialConvertResponse(new HashMap<>());
    }

    public FinancialConvertResponse  convertJsonArrayDataInMarkDown(FinancialConvertRequest request){
        return new FinancialConvertResponse(new HashMap<>());
    }



}
