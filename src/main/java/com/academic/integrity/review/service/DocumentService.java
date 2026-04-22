package com.academic.integrity.review.service;

import com.academic.integrity.review.dto.DocumentResponseDTO;
import com.academic.integrity.review.dto.DocumentUpdateRequestDTO;
import com.academic.integrity.review.dto.DocumentUploadRequestDTO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

	List<DocumentResponseDTO> getAllDocuments();

	List<DocumentResponseDTO> getDocuments(
			String course,
			String reviewPriority,
			String reviewStatus,
			String studentName,
			String sortBy,
			String sortDirection);

	DocumentResponseDTO getDocumentById(Long id);

	DocumentResponseDTO uploadDocument(MultipartFile file, DocumentUploadRequestDTO request);

	DocumentResponseDTO updateDocument(Long id, DocumentUpdateRequestDTO request);

	String exportDocumentsCsv();

	void deleteDocument(Long id);
}
