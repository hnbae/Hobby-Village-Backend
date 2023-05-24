package com.hobbyvillage.backend.user_reviews;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hobbyvillage.backend.UploadDir;

@RestController
public class UserReviewsController {

	private UserReviewsServiceImpl service;
	
	public UserReviewsController(UserReviewsServiceImpl service) {
		this.service = service;
	}
	
	// 상품 리뷰 개수 
	@GetMapping("/prodReview/count")
	public int getProdRevwCount(@RequestParam(value="prodCode") String prodCode) {
		return service.getProdRevwCount(prodCode);
	}
	
	// 상품 리뷰 목록+상세 조회 
	@GetMapping("/prodReview/list")
	public List<UserReviewsDTO> getProdRevwList(@RequestParam(value="prodCode") String prodCode) {
		return service.getProdRevwList(prodCode);
	}
	
	// 리뷰 신고
	@GetMapping("/prodReview/report")
	public void reportReview(@RequestParam(value="email") String email, @RequestParam(value="revwCode") String revwCode) {
		service.reportReview(email, revwCode);
	}
	
	// 리뷰 이미지 파일명 조회 
	@GetMapping("/prodReview/imgName")
	public List<String> getProdRevwPics(@RequestParam(value="revwCode") String revwCode) {
		return service.getProdRevwPics(revwCode);
	}
	
	// macOS 경로: //Uploaded//ProductsImage
	// 윈도우 경로: \\Uploaded\\ProductsImage
	@GetMapping("/prodReview/upload/{fileName}") // 이미지 불러오기 
	public ResponseEntity<byte[]> getRequestFileData(
			@PathVariable(value = "fileName", required = true) String fileName) {
		File file = new File(UploadDir.uploadDir + "//Uploaded//ReviewsImage", fileName);
		ResponseEntity<byte[]> result = null;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Type", Files.probeContentType(file.toPath()));
			result = new ResponseEntity<>(FileCopyUtils.copyToByteArray(file), headers, HttpStatus.OK);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
}
