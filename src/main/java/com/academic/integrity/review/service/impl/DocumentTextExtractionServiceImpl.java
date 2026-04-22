package com.academic.integrity.review.service.impl;

import com.academic.integrity.review.domain.Document;
import com.academic.integrity.review.service.DocumentTextExtractionService;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

@Service
public class DocumentTextExtractionServiceImpl implements DocumentTextExtractionService {

	@Override
	public String extractText(Document document) {
		if (document == null) {
			throw new IllegalArgumentException("Document is required");
		}
		String storedPath = normalize(document.getStoredPath());
		if (storedPath == null) {
			throw new IllegalArgumentException("Document does not have a stored file path");
		}

		Path filePath = Paths.get(storedPath);
		if (!Files.exists(filePath)) {
			throw new IllegalArgumentException("Stored file not found: " + filePath);
		}

		String nameForExtension = normalize(document.getOriginalFilename());
		if (nameForExtension == null) {
			nameForExtension = normalize(document.getStoredFilename());
		}
		if (nameForExtension == null) {
			nameForExtension = storedPath;
		}

		String extension = getExtension(nameForExtension);
		return switch (extension) {
			case "txt" -> extractTxt(filePath);
			case "pdf" -> extractPdf(filePath);
			case "docx" -> extractDocx(filePath);
			default -> throw new IllegalArgumentException("Unsupported file type for extraction: " + extension);
		};
	}

	private static String extractTxt(Path filePath) {
		try {
			return Files.readString(filePath, StandardCharsets.UTF_8);
		} catch (MalformedInputException ex) {
			try {
				return Files.readString(filePath, Charset.defaultCharset());
			} catch (IOException ioex) {
				throw new IllegalArgumentException("Failed to read TXT file", ioex);
			}
		} catch (IOException ex) {
			throw new IllegalArgumentException("Failed to read TXT file", ex);
		}
	}

	private static String extractPdf(Path filePath) {
		try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
			PDFTextStripper stripper = new PDFTextStripper();
			return stripper.getText(document);
		} catch (IOException ex) {
			throw new IllegalArgumentException("Failed to extract text from PDF", ex);
		}
	}

	private static String extractDocx(Path filePath) {
		try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(filePath));
				XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
			return Objects.toString(extractor.getText(), "");
		} catch (IOException ex) {
			throw new IllegalArgumentException("Failed to extract text from DOCX", ex);
		}
	}

	private static String getExtension(String filename) {
		String cleaned = Objects.toString(filename, "");
		int lastDot = cleaned.lastIndexOf('.');
		if (lastDot < 0 || lastDot == cleaned.length() - 1) {
			return "";
		}
		return cleaned.substring(lastDot + 1).toLowerCase(Locale.ROOT);
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}